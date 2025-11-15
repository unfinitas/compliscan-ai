package com.unfinitas.backend.core.ingestion.model;

/**
 * Represents the type of content in a MOE document paragraph.
 *
 * EASA Part-145 MOE documents have a hierarchical section structure,
 * where each paragraph can contain either just a section heading or regular content.
 */
public enum ContentType {

    /**
     * Paragraph contains only a section heading.
     * Example: "Part 1 - Management", "1.1 Corporate Commitment"
     */
    HEADING,

    /**
     * Paragraph contains regular text content, or a mix of heading and content.
     * Example: descriptive text under a section, or heading with inline content
     */
    PARAGRAPH
}
