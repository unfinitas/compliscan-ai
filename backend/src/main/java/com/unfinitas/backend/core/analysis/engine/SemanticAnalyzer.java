package com.unfinitas.backend.core.analysis.engine;

import com.unfinitas.backend.core.analysis.dto.ClauseMatchResult;
import com.unfinitas.backend.core.analysis.dto.ParagraphMatch;
import com.unfinitas.backend.core.analysis.matcher.TextMatcher;
import com.unfinitas.backend.core.ingestion.model.Paragraph;
import com.unfinitas.backend.core.regulation.model.RegulationClause;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SemanticAnalyzer {
    private static final double RELEVANCE_THRESHOLD = 0.30;
    private static final int MAX_MATCHES = 10;

    private final TextMatcher textMatcher;

    public SemanticAnalysisResult analyze(
            final List<Paragraph> moeParagraphs,
            final List<RegulationClause> clauses) {

        log.info("Starting semantic analysis: {} clauses vs {} paragraphs",
                clauses.size(), moeParagraphs.size());

        // Validate embeddings exist
        final long clausesWithoutEmbedding = clauses.stream()
                .filter(c -> c.getEmbedding() == null)
                .count();

        final long paragraphsWithoutEmbedding = moeParagraphs.stream()
                .filter(p -> p.getEmbedding() == null)
                .count();

        if (clausesWithoutEmbedding > 0) {
            log.warn("{} clauses missing embeddings - they will be skipped", clausesWithoutEmbedding);
        }

        if (paragraphsWithoutEmbedding > 0) {
            log.warn("{} paragraphs missing embeddings - they will be skipped", paragraphsWithoutEmbedding);
        }

        // Use batch processing for efficiency - now returns Map<UUID, ...>
        final Map<UUID, List<TextMatcher.ParagraphMatchResult>> batchResults =
                textMatcher.batchFindMatches(clauses, moeParagraphs, RELEVANCE_THRESHOLD);

        log.info("Batch similarity computation complete. Building results...");

        // Build results from batch processing - lookup by UUID instead of clauseId
        final List<ClauseMatchResult> results = clauses.stream()
                .map(clause -> {
                    final List<TextMatcher.ParagraphMatchResult> clauseMatches =
                            batchResults.getOrDefault(clause.getId(), List.of()); // Use UUID

                    final List<ParagraphMatch> matches = clauseMatches.stream()
                            .limit(MAX_MATCHES)
                            .map(r -> new ParagraphMatch(
                                    r.paragraph(),
                                    r.similarity(),
                                    extractContext(r.paragraph())
                            ))
                            .toList();

                    final double bestSimilarity = matches.isEmpty() ? 0.0 : matches.get(0).similarity();
                    final ClauseMatchResult.MatchQuality quality = determineQuality(bestSimilarity);
                    if (quality.equals(ClauseMatchResult.MatchQuality.GOOD)) {
                        log.error("========================================");
                        log.error(clause.getContent());
                        log.error(matches.get(0).paragraph().getContent());
                        log.error("Quality: " + quality.toString());
                        log.error("Similar: " + matches.get(0).similarity());
                        log.error("========================================");
                    }

                    final String evidence = buildEvidence(matches);

                    return new ClauseMatchResult(
                            clause.getClauseId(),
                            clause.getTitle(),
                            matches,
                            bestSimilarity,
                            quality,
                            evidence
                    );
                })
                .toList();

        log.info("Semantic analysis complete: {} clause results generated", results.size());
        logAnalysisSummary(results);

        return new SemanticAnalysisResult(results);
    }

    private ClauseMatchResult.MatchQuality determineQuality(final double similarity) {
        if (similarity >= 0.90) return ClauseMatchResult.MatchQuality.EXCELLENT;
        if (similarity >= 0.75) return ClauseMatchResult.MatchQuality.GOOD;
        if (similarity >= 0.60) return ClauseMatchResult.MatchQuality.ADEQUATE;
        if (similarity >= 0.40) return ClauseMatchResult.MatchQuality.WEAK;
        if (similarity >= 0.30) return ClauseMatchResult.MatchQuality.POOR;
        return ClauseMatchResult.MatchQuality.NOT_FOUND;
    }

    private String extractContext(final Paragraph paragraph) {
        final String content = paragraph.getContent();
        return content.length() > 200
                ? content.substring(0, 200) + "..."
                : content;
    }

    private String buildEvidence(final List<ParagraphMatch> matches) {
        if (matches.isEmpty()) {
            return "No matching content found";
        }

        return matches.stream()
                .limit(3)
                .map(m -> {
                    final String sectionNumber = m.paragraph().getSection() != null
                            ? m.paragraph().getSection().getSectionNumber()
                            : "N/A";

                    return String.format("Section %s (%.0f%%): \"%s\"",
                            sectionNumber,
                            m.similarity() * 100,
                            m.excerptContext()
                    );
                })
                .collect(Collectors.joining("\n"));
    }

    private void logAnalysisSummary(final List<ClauseMatchResult> results) {
        final Map<ClauseMatchResult.MatchQuality, Long> qualityDistribution = results.stream()
                .collect(Collectors.groupingBy(
                        ClauseMatchResult::quality,
                        Collectors.counting()
                ));

        log.info("Analysis Summary:");
        log.info("  Total clauses analyzed: {}", results.size());
        qualityDistribution.forEach((quality, count) ->
                log.info("  {} matches: {}", quality, count)
        );

        final long clausesWithMatches = results.stream()
                .filter(r -> !r.matches().isEmpty())
                .count();

        log.info("  Clauses with matches: {}/{}", clausesWithMatches, results.size());
    }

    public record SemanticAnalysisResult(List<ClauseMatchResult> clauseMatches) {

        /**
         * Get summary statistics of the analysis
         */
        public AnalysisSummary getSummary() {
            final Map<ClauseMatchResult.MatchQuality, Long> distribution = clauseMatches.stream()
                    .collect(Collectors.groupingBy(
                            ClauseMatchResult::quality,
                            Collectors.counting()
                    ));

            final long totalClauses = clauseMatches.size();
            final long matchedClauses = clauseMatches.stream()
                    .filter(r -> !r.matches().isEmpty())
                    .count();

            final double averageSimilarity = clauseMatches.stream()
                    .mapToDouble(ClauseMatchResult::bestSimilarity)
                    .average()
                    .orElse(0.0);

            return new AnalysisSummary(
                    totalClauses,
                    matchedClauses,
                    averageSimilarity,
                    distribution
            );
        }
    }

    public record AnalysisSummary(
            long totalClauses,
            long matchedClauses,
            double averageSimilarity,
            Map<ClauseMatchResult.MatchQuality, Long> qualityDistribution
    ) {
    }
}
