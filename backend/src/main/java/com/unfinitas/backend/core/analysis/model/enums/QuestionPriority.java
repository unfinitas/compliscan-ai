package com.unfinitas.backend.core.analysis.model.enums;

public enum QuestionPriority {
    CRITICAL,    // Missing mandatory requirements
    HIGH,        // Partial mandatory or missing recommended
    MEDIUM,      // Minor gaps
    LOW          // Informational
}
