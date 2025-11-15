package com.unfinitas.backend.core.analysis.engine;

import com.unfinitas.backend.core.analysis.dto.ClauseMatchResult;
import com.unfinitas.backend.core.analysis.dto.MoeParagraphCandidate;
import com.unfinitas.backend.core.analysis.dto.ParagraphMatch;
import com.unfinitas.backend.core.analysis.matcher.TextMatcher;
import com.unfinitas.backend.core.ingestion.model.Paragraph;
import com.unfinitas.backend.core.llm.LlmJudge;
import com.unfinitas.backend.core.llm.dto.ComplianceResult;
import com.unfinitas.backend.core.regulation.model.RegulationClause;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SemanticAnalyzer {

    // Embedding filter threshold used by TextMatcher
    private static final double RELEVANCE_THRESHOLD = 0.30;

    // Adjusted thresholds - prioritize LLM over cosine
    private static final double HIGH_SIMILARITY = 0.90;  // Very high bar for cosine-only
    private static final double LOW_SIMILARITY  = 0.25;  // Very low bar for cosine-only

    private static final int MAX_MATCHES_FOR_UI   = 10;
    private static final int MAX_CANDIDATES_FOR_LLM = 5;
    private static final int BATCH_SIZE = 5;  // Smaller batches for reliability

    private final TextMatcher textMatcher;
    private final LlmJudge judge;

    public SemanticAnalysisResult analyze(
            final List<Paragraph> moeParagraphs,
            final List<RegulationClause> clauses) {

        log.info("Starting semantic analysis: {} clauses vs {} paragraphs",
                clauses.size(), moeParagraphs.size());

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

        // Batch cosine similarity
        final Map<UUID, List<TextMatcher.ParagraphMatchResult>> batchResults =
                textMatcher.batchFindMatches(clauses, moeParagraphs, RELEVANCE_THRESHOLD);

        log.info("Batch similarity computation complete. Classifying clauses & batching LLM calls...");

        final List<RegulationClause> highClauses = new ArrayList<>();
        final List<RegulationClause> lowClauses = new ArrayList<>();
        final List<AmbiguousClause> ambiguousClauses = new ArrayList<>();

        // Classify clauses
        for (final RegulationClause clause : clauses) {
            final List<TextMatcher.ParagraphMatchResult> clauseMatches =
                    batchResults.getOrDefault(clause.getId(), List.of());

            if (clauseMatches.isEmpty()) {
                lowClauses.add(clause);
                continue;
            }

            final double bestSim = clauseMatches.get(0).similarity();
            final List<ParagraphMatch> matches = clauseMatches.stream()
                    .limit(MAX_MATCHES_FOR_UI)
                    .map(r -> new ParagraphMatch(
                            r.paragraph(),
                            r.similarity(),
                            extractContext(r.paragraph())
                    ))
                    .toList();

            if (bestSim >= HIGH_SIMILARITY) {
                highClauses.add(clause);
            } else if (bestSim <= LOW_SIMILARITY) {
                lowClauses.add(clause);
            } else {
                // Everything in between goes to LLM
                final List<MoeParagraphCandidate> candidates = clauseMatches.stream()
                        .limit(MAX_CANDIDATES_FOR_LLM)
                        .map(r -> new MoeParagraphCandidate(
                                r.paragraph().getId(),
                                r.paragraph().getContent(),
                                r.similarity(),
                                r.paragraph().getSection() != null
                                        ? r.paragraph().getSection().getSectionNumber()
                                        : "N/A",
                                r.paragraph().getParagraphOrder()
                        ))
                        .toList();

                ambiguousClauses.add(new AmbiguousClause(clause, matches, candidates, bestSim));
            }
        }

        log.info("Classification: HIGH={} LOW={} AMBIGUOUS={} (sending to LLM)",
                highClauses.size(), lowClauses.size(), ambiguousClauses.size());

        // Build results list
        final List<ClauseMatchResult> results = new ArrayList<>();

        // Add HIGH matches (no LLM)
        for (final RegulationClause clause : highClauses) {
            final List<TextMatcher.ParagraphMatchResult> clauseMatches =
                    batchResults.get(clause.getId());
            final List<ParagraphMatch> matches = clauseMatches.stream()
                    .limit(MAX_MATCHES_FOR_UI)
                    .map(r -> new ParagraphMatch(
                            r.paragraph(),
                            r.similarity(),
                            extractContext(r.paragraph())
                    ))
                    .toList();
            results.add(buildCosineOnlyResult(clause, matches, clauseMatches.getFirst().similarity()));
        }

        // Add LOW matches (no LLM)
        for (final RegulationClause clause : lowClauses) {
            final List<TextMatcher.ParagraphMatchResult> clauseMatches =
                    batchResults.getOrDefault(clause.getId(), List.of());
            if (clauseMatches.isEmpty()) {
                results.add(noMatchResult(clause));
            } else {
                final List<ParagraphMatch> matches = clauseMatches.stream()
                        .limit(MAX_MATCHES_FOR_UI)
                        .map(r -> new ParagraphMatch(
                                r.paragraph(),
                                r.similarity(),
                                extractContext(r.paragraph())
                        ))
                        .toList();
                results.add(buildCosineOnlyResult(clause, matches, clauseMatches.getFirst().similarity()));
            }
        }

        // Process AMBIGUOUS in parallel batches (PREFER LLM)
        if (!ambiguousClauses.isEmpty()) {
            log.info("Dispatching {} ambiguous clauses to LLM in batches of {}",
                    ambiguousClauses.size(), BATCH_SIZE);

            results.addAll(processAmbiguousClausesInParallel(ambiguousClauses));
        }

        log.info("Semantic analysis complete: {} clause results generated", results.size());
        log.info("Breakdown: {} LLM results, {} cosine-only results",
                ambiguousClauses.size(), highClauses.size() + lowClauses.size());

        logAnalysisSummary(results);

        return new SemanticAnalysisResult(results);
    }

    private List<ClauseMatchResult> processAmbiguousClausesInParallel(
            final List<AmbiguousClause> ambiguous) {

        // Split into batches
        final List<List<AmbiguousClause>> batches = new ArrayList<>();
        for (int i = 0; i < ambiguous.size(); i += BATCH_SIZE) {
            batches.add(ambiguous.subList(i, Math.min(i + BATCH_SIZE, ambiguous.size())));
        }

        log.debug("Split {} clauses into {} batches", ambiguous.size(), batches.size());

        // Process batches in parallel
        return batches.parallelStream()
                .flatMap(batch -> processBatch(batch).stream())
                .collect(Collectors.toList());
    }

    private List<ClauseMatchResult> processBatch(final List<AmbiguousClause> batch) {
        final long startTime = System.currentTimeMillis();

        final List<LlmJudge.ClauseBatchInput> inputs = batch.stream()
                .map(ac -> new LlmJudge.ClauseBatchInput(
                        ac.clause.getClauseId(),
                        ac.clause.getContent(),
                        ac.candidates
                ))
                .toList();

        final Map<String, ComplianceResult> llmResults = judge.judgeBatch(inputs);

        final long elapsed = System.currentTimeMillis() - startTime;
        log.debug("Batch of {} processed in {}ms ({} LLM results)",
                batch.size(), elapsed, llmResults.size());

        final List<ClauseMatchResult> results = new ArrayList<>();
        int llmSuccessCount = 0;
        int fallbackCount = 0;

        for (final AmbiguousClause ac : batch) {
            final ComplianceResult compliance = llmResults.get(ac.clause.getClauseId());

            if (Objects.isNull(compliance)) {
                // Fallback to cosine-only if LLM fails
                log.debug("No LLM result for {}, using cosine fallback", ac.clause.getClauseId());
                results.add(buildCosineOnlyResult(ac.clause, ac.matches, ac.bestSim));
                fallbackCount++;
                continue;
            }

            // Prefer LLM result
            llmSuccessCount++;
            final double complianceScore = mapComplianceScore(compliance.compliance_status());
            final ClauseMatchResult.MatchQuality quality = determineQuality(complianceScore);
            final String evidence = buildEvidenceFromCompliance(compliance);

            results.add(new ClauseMatchResult(
                    ac.clause.getClauseId(),
                    ac.clause.getTitle(),
                    ac.matches,
                    complianceScore,
                    quality,
                    evidence
            ));
        }

        if (fallbackCount > 0) {
            log.info("Batch completed: {} LLM results, {} cosine fallbacks",
                    llmSuccessCount, fallbackCount);
        }

        return results;
    }

    private ClauseMatchResult noMatchResult(final RegulationClause clause) {
        return new ClauseMatchResult(
                clause.getClauseId(),
                clause.getTitle(),
                List.of(),
                0.0,
                ClauseMatchResult.MatchQuality.NOT_FOUND,
                "No matching content found in MOE for this requirement"
        );
    }

    private ClauseMatchResult buildCosineOnlyResult(
            final RegulationClause clause,
            final List<ParagraphMatch> matches,
            final double bestSimilarity
    ) {
        final ClauseMatchResult.MatchQuality quality = determineQuality(bestSimilarity);
        final StringBuilder evidenceBuilder = new StringBuilder();

        if (matches.isEmpty()) {
            evidenceBuilder.append("No matching MOE content identified by semantic search.");
        } else {
            evidenceBuilder.append("Top semantic matches:\n");
            matches.stream()
                    .limit(3)
                    .forEach(m -> {
                        final String sectionNum = m.paragraph().getSection() != null
                                ? m.paragraph().getSection().getSectionNumber()
                                : "N/A";
                        evidenceBuilder.append(String.format(
                                "  - Section %s (%.0f%%): \"%s\"%n",
                                sectionNum,
                                m.similarity() * 100,
                                truncate(m.excerptContext(), 200)
                        ));
                    });
        }

        evidenceBuilder.append("\n⚠️ Based on embeddings only (LLM not used).");

        return new ClauseMatchResult(
                clause.getClauseId(),
                clause.getTitle(),
                matches,
                bestSimilarity,
                quality,
                evidenceBuilder.toString().trim()
        );
    }

    private double mapComplianceScore(final String status) {
        if (status == null) return 0.0;
        return switch (status) {
            case "full" -> 1.0;
            case "partial" -> 0.5;
            default -> 0.0;
        };
    }

    private ClauseMatchResult.MatchQuality determineQuality(final double score) {
        if (score >= 0.90) return ClauseMatchResult.MatchQuality.EXCELLENT;
        if (score >= 0.75) return ClauseMatchResult.MatchQuality.GOOD;
        if (score >= 0.60) return ClauseMatchResult.MatchQuality.ADEQUATE;
        if (score >= 0.40) return ClauseMatchResult.MatchQuality.WEAK;
        if (score >= 0.30) return ClauseMatchResult.MatchQuality.POOR;
        return ClauseMatchResult.MatchQuality.NOT_FOUND;
    }

    private String extractContext(final Paragraph paragraph) {
        final String content = paragraph.getContent();
        return content.length() > 200
                ? content.substring(0, 200) + "..."
                : content;
    }

    private String buildEvidenceFromCompliance(final ComplianceResult c) {
        if (c == null) {
            return "No LLM compliance result available.";
        }

        final StringBuilder sb = new StringBuilder();

        if (c.compliance_status() != null) {
            sb.append("✓ Compliance status: ").append(c.compliance_status()).append("\n");
        }
        if (c.finding_level() != null) {
            sb.append("Finding level: ").append(c.finding_level()).append("\n");
        }

        if (c.justification() != null && !c.justification().isBlank()) {
            sb.append("Justification: ").append(c.justification().trim()).append("\n");
        }

        if (c.evidence() != null && !c.evidence().isEmpty()) {
            sb.append("Evidence:\n");
            c.evidence().stream()
                    .limit(3)
                    .forEach(ev -> sb.append(String.format(
                            "  - Paragraph %d (sim=%.2f): \"%s\"%n",
                            ev.moe_paragraph_id(),
                            ev.similarity_score() != null ? ev.similarity_score() : 0.0,
                            truncate(ev.relevant_excerpt(), 200)
                    )));
        }

        if (c.missing_elements() != null && !c.missing_elements().isEmpty()) {
            sb.append("Missing elements:\n");
            c.missing_elements().forEach(m ->
                    sb.append("  - ").append(m).append("\n")
            );
        }

        if (c.recommended_actions() != null && !c.recommended_actions().isEmpty()) {
            sb.append("Recommended actions:\n");
            c.recommended_actions().forEach(a ->
                    sb.append("  - ").append(a).append("\n")
            );
        }

        return sb.toString().trim();
    }

    private String truncate(final String text, final int maxLen) {
        if (text == null) return "";
        final String t = text.trim();
        return t.length() > maxLen ? t.substring(0, maxLen) + "..." : t;
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

        log.info("  Clauses with matches (any quality): {}/{}", clausesWithMatches, results.size());
    }

    private record AmbiguousClause(
            RegulationClause clause,
            List<ParagraphMatch> matches,
            List<MoeParagraphCandidate> candidates,
            double bestSim
    ) {}

    public record SemanticAnalysisResult(List<ClauseMatchResult> clauseMatches) {

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
    ) {}
}
