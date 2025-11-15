package com.unfinitas.backend.core.analysis.dto;

import com.unfinitas.backend.core.analysis.model.enums.MatchType;
import com.unfinitas.backend.core.ingrestion.model.Paragraph;

import java.util.List;

/**
 * Aggregated multi-paragraph match
 */
public record AggregatedMatch(
        List<Paragraph> paragraphs,
        double aggregatedSimilarity,
        String combinedText,
        MatchType matchType
) {
}
