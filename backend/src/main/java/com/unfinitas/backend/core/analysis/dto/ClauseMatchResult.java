package com.unfinitas.backend.core.analysis.dto;

import java.util.List;

/**
 * Result of semantic matching for a single clause
 */
public record ClauseMatchResult(
        String clauseId,
        String clauseTitle,
        List<ParagraphMatch> matches,
        double bestSimilarity,
        MatchQuality quality,
        String evidence
) {
    public enum MatchQuality {
        EXCELLENT,      // ≥90%
        GOOD,           // 75-90%
        ADEQUATE,       // 60-75%
        WEAK,           // 40-60%
        POOR,           // 30-40%
        NOT_FOUND       // <30%
    }
}
