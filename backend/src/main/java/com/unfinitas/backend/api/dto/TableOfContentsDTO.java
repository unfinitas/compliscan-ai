package com.unfinitas.backend.api.dto;

import java.util.List;

/**
 * DTO for representing a hierarchical table of contents entry from an MOE document.
 * 
 * Uses nested structure where each entry can contain child entries,
 * allowing representation of the complete MOE document hierarchy.
 * 
 * Example structure:
 * {
 *   "paragraphId": 1,
 *   "sectionNumber": "Part 1",
 *   "sectionTitle": "Management",
 *   "depth": 0,
 *   "children": [
 *     {
 *       "paragraphId": 2,
 *       "sectionNumber": "1.1",
 *       "sectionTitle": "Corporate Commitment",
 *       "depth": 1,
 *       "children": []
 *     }
 *   ]
 * }
 */
public record TableOfContentsDTO(
    /**
     * The database ID of the paragraph
     */
    Long paragraphId,

    /**
     * The section number (e.g., "Part 1", "1.4.1", "OSA 2")
     */
    String sectionNumber,

    /**
     * The section title (e.g., "Management", "Corporate Commitment")
     */
    String sectionTitle,

    /**
     * The hierarchical depth of this section
     * - 0: Part level (e.g., "Part 1")
     * - 1: Chapter level (e.g., "1.1")
     * - 2: Section level (e.g., "1.4.1")
     */
    Integer depth,

    /**
     * Child sections nested under this section.
     * Empty list if this is a leaf node.
     */
    List<TableOfContentsDTO> children
) {}
