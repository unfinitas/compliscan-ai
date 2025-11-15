package com.unfinitas.backend.core.analysis.dto;

import lombok.Builder;

/**
 * LLM-ready candidate paragraph for compliance analysis.
 * <p>
 * This structure is the "exchange format" between:
 * - vector search
 * - reranker LLM
 * - judge LLM
 * <p>
 * It contains ONLY what the LLM and semantic pipeline need,
 * not the full JPA entity.
 */
@Builder
public record MoeParagraphCandidate(

        Long paragraphId,          // Paragraph entity ID
        String text,               // Full paragraph text content
        Double similarityScore,    // Optional cosine similarity score (0..1)
        String sectionNumber,      // Optional, from paragraph.getSection()
        Integer paragraphOrder     // For traceability and UI sorting

) {
}
