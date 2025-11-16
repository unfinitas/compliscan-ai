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

    private static final double RELEVANCE_THRESHOLD = 0.30;
    private static final double HIGH_SIMILARITY = 0.90;
    private static final double LOW_SIMILARITY  = 0.25;

    private static final int MAX_MATCHES_FOR_UI   = 10;
    private static final int MAX_CANDIDATES_FOR_LLM = 5;
    private static final int BATCH_SIZE = 5;

    private final TextMatcher textMatcher;
    private final LlmJudge judge;

    public SemanticAnalysisResult analyze(
            final List<Paragraph> moeParagraphs,
            final List<RegulationClause> clauses) {

        log.info("Starting semantic analysis: {} clauses vs {} paragraphs",
                clauses.size(), moeParagraphs.size());

        final Map<UUID, List<TextMatcher.ParagraphMatchResult>> batchResults =
                textMatcher.batchFindMatches(clauses, moeParagraphs, RELEVANCE_THRESHOLD);

        log.info("Batch similarity computation complete.");

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

            final double bestSim = clauseMatches.getFirst().similarity();
            final List<ParagraphMatch> matches = clauseMatches.stream()
                    .limit(MAX_MATCHES_FOR_UI)
                    .map(r -> new ParagraphMatch(
                            r.paragraph(), r.similarity(), extractContext(r.paragraph())))
                    .toList();

            if (bestSim >= HIGH_SIMILARITY) {
                highClauses.add(clause);
            } else if (bestSim <= LOW_SIMILARITY) {
                lowClauses.add(clause);
            } else {
                final List<MoeParagraphCandidate> candidates = clauseMatches.stream()
                        .limit(MAX_CANDIDATES_FOR_LLM)
                        .map(r -> new MoeParagraphCandidate(
                                r.paragraph().getId(),
                                r.paragraph().getContent(),
                                r.similarity(),
                                r.paragraph().getSection() != null ?
                                        r.paragraph().getSection().getSectionNumber() : "N/A",
                                r.paragraph().getParagraphOrder()))
                        .toList();

                ambiguousClauses.add(new AmbiguousClause(
                        clause, matches, candidates, bestSim
                ));
            }
        }

        final List<ClauseMatchResult> results = new ArrayList<>();

        // HIGH → cosine-only
        for (final RegulationClause clause : highClauses) {
            final List<TextMatcher.ParagraphMatchResult> clauseMatches =
                    batchResults.get(clause.getId());

            final List<ParagraphMatch> matches = clauseMatches.stream()
                    .limit(MAX_MATCHES_FOR_UI)
                    .map(r -> new ParagraphMatch(r.paragraph(), r.similarity(), extractContext(r.paragraph())))
                    .toList();

            results.add(buildCosineOnlyResult(clause, matches, clauseMatches.getFirst().similarity()));
        }

        // LOW → cosine-only
        for (final RegulationClause clause : lowClauses) {
            final List<TextMatcher.ParagraphMatchResult> clauseMatches =
                    batchResults.getOrDefault(clause.getId(), List.of());

            if (clauseMatches.isEmpty()) {
                results.add(noMatchResult(clause));
                continue;
            }

            final List<ParagraphMatch> matches = clauseMatches.stream()
                    .limit(MAX_MATCHES_FOR_UI)
                    .map(r -> new ParagraphMatch(r.paragraph(), r.similarity(), extractContext(r.paragraph())))
                    .toList();

            results.add(buildCosineOnlyResult(clause, matches, clauseMatches.getFirst().similarity()));
        }

        // AMBIGUOUS → LLM
        if (!ambiguousClauses.isEmpty()) {
            results.addAll(processAmbiguousClausesInParallel(ambiguousClauses));
        }

        return new SemanticAnalysisResult(results);
    }

    private List<ClauseMatchResult> processAmbiguousClausesInParallel(
            final List<AmbiguousClause> ambiguous) {

        final List<List<AmbiguousClause>> batches = new ArrayList<>();

        for (int i = 0; i < ambiguous.size(); i += BATCH_SIZE) {
            batches.add(ambiguous.subList(i, Math.min(i + BATCH_SIZE, ambiguous.size())));
        }

        return batches.parallelStream()
                .flatMap(batch -> processBatch(batch).stream())
                .collect(Collectors.toList());
    }

    private List<ClauseMatchResult> processBatch(final List<AmbiguousClause> batch) {

        final List<LlmJudge.ClauseBatchInput> inputs = batch.stream()
                .map(ac -> new LlmJudge.ClauseBatchInput(
                        ac.clause.getClauseId(),
                        ac.clause.getContent(),
                        ac.candidates))
                .toList();

        final Map<String, ComplianceResult> llmResults = judge.judgeBatch(inputs);

        final List<ClauseMatchResult> results = new ArrayList<>();

        for (final AmbiguousClause ac : batch) {
            final ComplianceResult compliance = llmResults.get(ac.clause.getClauseId());

            if (compliance == null) {
                results.add(buildCosineOnlyResult(ac.clause, ac.matches, ac.bestSim));
                continue;
            }

            final double complianceScore = mapComplianceScore(compliance.compliance_status());
            final ClauseMatchResult.MatchQuality quality = determineQuality(complianceScore);
            final String evidence = buildEvidenceFromCompliance(compliance);

            results.add(new ClauseMatchResult(
                    ac.clause.getClauseId(),
                    ac.clause.getTitle(),
                    ac.matches,
                    complianceScore,
                    quality,
                    evidence,
                    compliance // ✔ FULL LLM RESULT HERE
            ));
        }

        return results;
    }

    // -------------------------------------------------------------------------
    // COSINE-ONLY (no LLM)
    // -------------------------------------------------------------------------
    private ClauseMatchResult buildCosineOnlyResult(
            final RegulationClause clause,
            final List<ParagraphMatch> matches,
            final double bestSimilarity) {

        final ClauseMatchResult.MatchQuality quality = determineQuality(bestSimilarity);

        return new ClauseMatchResult(
                clause.getClauseId(),
                clause.getTitle(),
                matches,
                bestSimilarity,
                quality,
                "Top matches (embeddings only).",
                null // ✔ no LLM
        );
    }

    private ClauseMatchResult noMatchResult(final RegulationClause clause) {
        return new ClauseMatchResult(
                clause.getClauseId(),
                clause.getTitle(),
                List.of(),
                0.0,
                ClauseMatchResult.MatchQuality.NOT_FOUND,
                "No matching content found.",
                null // ✔ no LLM
        );
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    private String extractContext(final Paragraph p) {
        final String t = p.getContent();
        return t.length() > 200 ? t.substring(0,200) + "…" : t;
    }

    private double mapComplianceScore(final String status) {
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
        return ClauseMatchResult.MatchQuality.POOR;
    }

    private String buildEvidenceFromCompliance(final ComplianceResult c) {
        return "Compliance Status: " + c.compliance_status();
    }

    private record AmbiguousClause(
            RegulationClause clause,
            List<ParagraphMatch> matches,
            List<MoeParagraphCandidate> candidates,
            double bestSim
    ) {}

    public record SemanticAnalysisResult(List<ClauseMatchResult> clauseMatches) {}
}
