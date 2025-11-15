package com.unfinitas.backend.api.dto;

import java.util.UUID;

public record AnalysisResponse(
        UUID analysisId,
        String message
) {
}
