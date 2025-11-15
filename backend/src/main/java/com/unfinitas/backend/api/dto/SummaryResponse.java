package com.unfinitas.backend.api.dto;

import java.util.UUID;

/**
 * Response DTO for analysis summary endpoint
 * Provides recommendation and executive summary
 */
public record SummaryResponse(
    UUID id,
    String recommendation,
    String executiveSummary
) {
}