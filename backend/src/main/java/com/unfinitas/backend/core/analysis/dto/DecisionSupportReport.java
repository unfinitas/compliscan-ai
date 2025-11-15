package com.unfinitas.backend.core.analysis.dto;

import com.unfinitas.backend.core.analysis.model.enums.ApprovalRecommendation;

public record DecisionSupportReport(
        ApprovalRecommendation recommendation,
        String executiveSummary
) {
}
