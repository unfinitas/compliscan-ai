package com.unfinitas.backend.core.analysis.dto;

import com.unfinitas.backend.core.analysis.model.GapFinding;

import java.util.List;

public record SemanticAnalysisResult(List<GapFinding> gaps) {
}
