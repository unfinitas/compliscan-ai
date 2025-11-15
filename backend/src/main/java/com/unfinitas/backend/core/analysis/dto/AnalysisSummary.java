package com.unfinitas.backend.core.analysis.dto;

import com.unfinitas.backend.core.analysis.model.enums.AnalysisType;
import com.unfinitas.backend.core.analysis.model.enums.ApprovalRecommendation;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Complete analysis summary
 *
 * @param overallCompliance Scores
 * @param totalClauses      Counts
 * @param criticalGaps      Findings
 * @param recommendation    Recommendation
 * @param completedAt       Metadata
 */
public record AnalysisSummary(
        UUID analysisId,
        String moeIdentifier,
        AnalysisType analysisType,

        double overallCompliance,
        double hardLawCompliance,
        double softLawCompliance,
        int totalClauses,
        int coveredCount,
        int partialCount,
        int missingCount,
        int criticalGaps,
        int majorGaps,
        int minorGaps,
        ApprovalRecommendation recommendation,
        String executiveSummary,
        LocalDateTime completedAt,
        long processingTimeMs
) {
}
