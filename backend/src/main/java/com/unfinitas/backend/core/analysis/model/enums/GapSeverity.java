package com.unfinitas.backend.core.analysis.model.enums;

public enum GapSeverity {
    CRITICAL,    // Mandatory requirement missing
    MAJOR,       // Mandatory requirement insufficient
    MINOR,       // Non-mandatory issue
    INFORMATIONAL // Note for improvement
}

