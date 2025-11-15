package com.unfinitas.backend.core.analysis.engine;

import com.unfinitas.backend.core.analysis.dto.ClauseMatchResult;
import com.unfinitas.backend.core.analysis.dto.ParagraphMatch;
import com.unfinitas.backend.core.analysis.matcher.TextMatcher;
import com.unfinitas.backend.core.ingrestion.model.Paragraph;
import com.unfinitas.backend.core.regulation.model.RegulationClause;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SemanticAnalyzer {

    private final TextMatcher textMatcher;

    private static final double RELEVANCE_THRESHOLD = 0.30;
    private static final int MAX_MATCHES = 10;

    public SemanticAnalysisResult analyze(
            final List<Paragraph> moeParagraphs,
            final List<RegulationClause> clauses) {

        final List<ClauseMatchResult> results = new ArrayList<>();

        for (final RegulationClause clause : clauses) {
            log.debug("Analyzing clause: {}", clause.getClauseId());

            // Find all relevant paragraphs
            final List<ParagraphMatch> matches = textMatcher.findAllMatches(
                            clause.getContent(),
                            moeParagraphs,
                            RELEVANCE_THRESHOLD
                    ).stream()
                    .limit(MAX_MATCHES)
                    .map(r -> new ParagraphMatch(
                            r.paragraph(),
                            r.similarity(),
                            extractContext(r.paragraph())
                    ))
                    .toList();

            final double bestSimilarity = matches.isEmpty() ? 0.0 : matches.get(0).similarity();
            final ClauseMatchResult.MatchQuality quality = determineQuality(bestSimilarity);
            final String evidence = buildEvidence(matches);

            results.add(new ClauseMatchResult(
                    clause.getClauseId(),
                    clause.getTitle(),
                    matches,
                    bestSimilarity,
                    quality,
                    evidence
            ));
        }

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
                .map(m -> String.format("Section %s (%.0f%%): \"%s\"",
                        m.paragraph().getSectionNumber(),
                        m.similarity() * 100,
                        m.excerptContext()
                ))
                .collect(Collectors.joining("\n"));
    }

    public record SemanticAnalysisResult(List<ClauseMatchResult> clauseMatches) {}
}
