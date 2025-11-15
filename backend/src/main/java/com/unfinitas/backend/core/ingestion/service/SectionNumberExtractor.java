package com.unfinitas.backend.core.ingestion.service;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for extracting section numbers and hierarchical information from MOE document text.
 * 
 * Supports EASA Part-145 MOE document structure with multi-language Part keywords:
 * - English: "Part 1", "Part 2"
 * - Finnish: "OSA 1", "OSA 2"
 * - French: "PARTIE 1"
 * - German: "TEIL 1"
 * - Spanish: "PARTE 1"
 * 
 * Also supports numeric hierarchical sections up to 3 levels (e.g., "1.4.1")
 */
@Component
public class SectionNumberExtractor {

    /**
     * Pattern for Part-level sections in multiple languages.
     * Matches: "Part 1", "OSA 2", "PARTIE 3", etc.
     * Group 1: Keyword (Part, OSA, PARTIE, TEIL, PARTE)
     * Group 2: Number (0-9 or A-Z)
     * Group 3: Title text after the number
     */
    private static final Pattern PART_PATTERN = Pattern.compile(
        "^\s*(Part|OSA|PARTIE|TEIL|PARTE|SECTION)\s+([\\dA-Z]+)\s*[-:]?\s*(.*)$",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Pattern for numeric hierarchical sections (up to 3 levels).
     * Matches: "1.1", "1.4.1", "2.3.2"
     * Group 1: Section number (X.Y or X.Y.Z format)
     * Group 2: Section title
     */
    private static final Pattern NUMERIC_PATTERN =
            Pattern.compile("^\\s*(\\d+\\.\\d+(?:\\.\\d+)?)\\s+(.+)$");

    /**
     * Data class containing extracted section information
     * 
     * @param number Section number (e.g., "Part 1", "1.4.1")
     * @param title Section title (e.g., "Management", "Corporate Commitment")
     * @param depth Hierarchical depth (0=Part, 1=Chapter, 2=Section)
     * @param parent Parent section number (null for top-level)
     * @param isPart True if this is a Part-level section
     */
    public record SectionInfo(
        String number,
        String title,
        int depth,
        String parent,
        boolean isPart
    ) {}

    /**
     * Extracts section information from a text paragraph.
     * 
     * Attempts to match the text against known section patterns:
     * 1. Part-level patterns (Part 1, OSA 1, etc.)
     * 2. Numeric hierarchical patterns (1.1, 1.4.1, etc.)
     * 
     * @param text The text to analyze
     * @return SectionInfo if a section pattern is found, null otherwise
     */
    public SectionInfo extract(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        String trimmed = text.trim();
        Matcher matcher;

        // Try Part-level pattern first
        matcher = PART_PATTERN.matcher(trimmed);
        if (matcher.find()) {
            String keyword = capitalize(matcher.group(1));  // "Part", "OSA", etc.
            String number = matcher.group(2);               // "0", "1", "2", etc.
            String title = matcher.group(3).trim();

            return new SectionInfo(
                keyword + " " + number,  // "Part 1", "OSA 1"
                title,
                0,    // Part level is always depth 0
                null, // Part level has no parent
                true  // This is a Part-level section
            );
        }

        // Try numeric hierarchical pattern
        matcher = NUMERIC_PATTERN.matcher(trimmed);
        if (matcher.find()) {
            String number = matcher.group(1);        // "1.4.1"
            String title = matcher.group(2).trim();  // "Accountable Manager"

            int depth = countDots(number);

            // Limit to 3 levels (0, 1, 2)
            if (depth > 2) {
                return null; // Ignore 4th level and deeper (e.g., "1.4.1.1")
            }

            String parent = getParent(number);
            return new SectionInfo(number, title, depth, parent, false);
        }

        return null; // No section pattern found
    }

    /**
     * Counts the number of dots in a section number to determine depth.
     * Examples:
     * - "1" → 0 dots → depth 0
     * - "1.1" → 1 dot → depth 1
     * - "1.4.1" → 2 dots → depth 2
     * 
     * @param sectionNumber The section number
     * @return The number of dots (= depth)
     */
    private int countDots(String sectionNumber) {
        return (int) sectionNumber.chars().filter(ch -> ch == '.').count();
    }

    /**
     * Extracts the parent section number by removing the last segment.
     * Examples:
     * - "1.4.1" → "1.4"
     * - "1.1" → null (no parent in numeric hierarchy)
     * 
     * Note: For sections like "1.1", the parent will be set to the current Part
     * in the parsing logic, not here.
     * 
     * @param sectionNumber The section number
     * @return The parent section number, or null if no parent
     */
    private String getParent(String sectionNumber) {
        int lastDot = sectionNumber.lastIndexOf('.');
        return (lastDot > 0) ? sectionNumber.substring(0, lastDot) : null;
    }

    /**
     * Capitalizes the first letter of a string.
     * Used to normalize Part keywords: "part" → "Part", "osa" → "OSA"
     * 
     * @param s The string to capitalize
     * @return Capitalized string
     */
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
