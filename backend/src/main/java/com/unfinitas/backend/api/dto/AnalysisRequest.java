package com.unfinitas.backend.api.dto;

import java.util.UUID;

public record AnalysisRequest(
        UUID moeId,
        String regulationVersion
) {}
