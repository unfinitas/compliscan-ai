package com.unfinitas.backend.core.analysis.dto;

import lombok.Builder;

import java.util.UUID;

/**
 * Request object for compliance analysis
 */
@Builder
public record AnalysisRequest(
        UUID moeDocumentId,
        UUID regulationId
) {
    public static AnalysisRequest of(final UUID moeDocumentId, final UUID regulationId) {
        return AnalysisRequest.builder()
                .moeDocumentId(moeDocumentId)
                .regulationId(regulationId)
                .build();
    }
}
