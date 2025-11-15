package com.unfinitas.backend.core.llm.dto;

import lombok.Builder;

/**
 * Result from the LLM reranker stage.
 * <p>
 * Represents the relevance score assigned to a paragraph
 * for a specific regulation clause.
 * <p>
 * This object is small, clean, and compatible with:
 * - LLM JSON output
 * - Compliance judge input
 * - ClauseMatchResult combination
 */
@Builder
public record RerankedParagraph(
        Long paragraphId,        // ID of the MOE paragraph
        Double relevanceScore    // 0.0–1.0 precision LLM ranking score
) {
}
