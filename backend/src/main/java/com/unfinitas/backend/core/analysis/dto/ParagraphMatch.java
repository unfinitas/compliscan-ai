package com.unfinitas.backend.core.analysis.dto;


import com.unfinitas.backend.core.ingestion.model.Paragraph;

/**
 * Single paragraph match with score
 */
public record ParagraphMatch(
        Paragraph paragraph,
        double similarity,
        String excerptContext
) {
}
