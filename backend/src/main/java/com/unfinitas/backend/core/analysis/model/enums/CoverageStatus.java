package com.unfinitas.backend.core.analysis.model.enums;

public enum CoverageStatus {
    COVERED,    // ≥75% similarity
    PARTIAL,    // 40-75% similarity
    MISSING     // <40% similarity
}
