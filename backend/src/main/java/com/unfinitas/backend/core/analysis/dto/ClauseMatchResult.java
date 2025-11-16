package com.unfinitas.backend.core.analysis.dto;

import com.unfinitas.backend.core.llm.dto.ComplianceResult;
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
        String evidence,
        ComplianceResult complianceResult
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
