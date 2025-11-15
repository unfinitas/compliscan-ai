package com.unfinitas.backend.api.dto;

import java.util.UUID;

/**
 * Response DTO for analysis gaps endpoint
 * Provides gap findings breakdown by severity
 */
public record GapsResponse(
    UUID id,
    Integer criticalGaps,
    Integer majorGaps,
    Integer minorGaps
) {
}