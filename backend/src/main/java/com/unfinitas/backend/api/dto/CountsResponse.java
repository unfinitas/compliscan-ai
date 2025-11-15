package com.unfinitas.backend.api.dto;

import java.util.UUID;

/**
 * Response DTO for analysis counts endpoint
 * Provides coverage statistics for an analysis
 */
public record CountsResponse(
    UUID id,
    Integer totalCount,
    Integer coverageCount,
    Integer missingCount,
    Integer partialCount
) {
}