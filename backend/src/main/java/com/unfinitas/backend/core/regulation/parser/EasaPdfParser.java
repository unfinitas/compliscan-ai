package com.unfinitas.backend.core.regulation.parser;

import com.unfinitas.backend.core.ingestion.PdfParser;
import com.unfinitas.backend.core.regulation.dto.ClauseData;
import com.unfinitas.backend.core.regulation.dto.RegulationData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for EASA regulation PDF documents.
 * Converts PDF content to structured RegulationData compatible with the database schema.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EasaPdfParser {

    private final PdfParser pdfParser;

    // Date formatter for parsing dates in PDF
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy");

    // Patterns for clause identification
    private static final Pattern CLAUSE_PATTERN = Pattern.compile(
            "^(?:(?:AMC|GM)\\d*\\s+)?(?:M\\.A\\.|ML\\.|Part-M\\s+Subpart|Part-|Article|Appendix)\\s*[IVX\\d]+(?:\\.[\\d]+)*"
    );

    private static final Pattern AMC_PATTERN = Pattern.compile("^AMC\\d*\\s+(.+)");
    private static final Pattern GM_PATTERN = Pattern.compile("^GM\\d*\\s+(.+)");
    private static final Pattern MA_PATTERN = Pattern.compile("^(M\\.A\\.\\d+)(?:\\s+(.+))?");
    private static final Pattern ML_PATTERN = Pattern.compile("^(ML\\.A\\.\\d+)(?:\\s+(.+))?");
    private static final Pattern ARTICLE_PATTERN = Pattern.compile("^Article\\s+(\\d+)(?:\\s+(.+))?");
    private static final Pattern APPENDIX_PATTERN = Pattern.compile("^Appendix\\s+([IVX]+)(?:\\s+(.+))?");

    // Section headers that indicate major divisions
    private static final Pattern SECTION_HEADER_PATTERN = Pattern.compile(
            "^(?:SUBPART|SECTION|CHAPTER|PART)\\s+([A-Z]+|\\d+)(?:\\s+[—-]\\s+)?(.+)?",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Parse PDF file and extract structured regulation data
     */
    public RegulationData parsePdf(final InputStream inputStream, final String filename) throws Exception {
        log.info("Parsing EASA PDF: {}", filename);

        // Extract raw text using PdfBoxParser
        final PdfParser.ParsedPdfResult pdfResult = pdfParser.parse(inputStream, filename);
        final String rawText = pdfResult.rawText();

        // Extract regulation metadata
        final String regulationCode = extractRegulationCode(filename, pdfResult.title());
        final String regulationName = extractRegulationName(filename, pdfResult.title());
        final String version = extractVersion(filename);
        final LocalDate effectiveDate = extractEffectiveDate(rawText);

        // Determine if this is an AMC/GM document
        final boolean isAmcGm = isAmcGmFile(filename);

        // Extract clauses from text
        final List<ClauseData> clauses = extractClauses(rawText, isAmcGm);

        log.info("Parsed regulation: {} - {} with {} clauses", regulationCode, regulationName, clauses.size());

        return new RegulationData(
                regulationCode,
                version,
                regulationName,
                effectiveDate,
                clauses
        );
    }

    /**
     * Extract regulation code from filename or title
     * Example: "95CADA_2025-09-02_06.16.08_EAR-for-Continuing-Airworthiness-Regulation-EU-No-1321-2014.pdf"
     * Returns: "Regulation (EU) No 1321/2014"
     */
    private String extractRegulationCode(final String filename, final String pdfTitle) {
        if (filename != null) {
            final Matcher matcher = Pattern.compile("(?:Regulation-)?EU-No-(\\d+)-(\\d+)").matcher(filename);
            if (matcher.find()) {
                return "Regulation (EU) No " + matcher.group(1) + "/" + matcher.group(2);
            }
        }

        if (pdfTitle != null && pdfTitle.contains("Regulation")) {
            final Matcher matcher = Pattern.compile("Regulation\\s+\\(EU\\)\\s+No\\.?\\s+(\\d+/\\d+)").matcher(pdfTitle);
            if (matcher.find()) {
                return "Regulation (EU) No " + matcher.group(1);
            }
        }

        return "UNKNOWN";
    }

    /**
     * Extract regulation name from filename or title
     */
    private String extractRegulationName(final String filename, final String pdfTitle) {
        if (filename != null && filename.contains("EAR-for-")) {
            String name = filename.substring(filename.indexOf("EAR-for-"));
            name = name.replaceFirst("EAR-for-", "Easy Access Rules for ");
            name = name.replaceFirst("-Regulation-.*", "");
            name = name.replace("-", " ");
            return name;
        }

        if (pdfTitle != null && pdfTitle.contains("Easy Access Rules")) {
            final int start = pdfTitle.indexOf("Easy Access Rules");
            final int end = pdfTitle.indexOf("(Regulation");
            if (end > start) {
                return pdfTitle.substring(start, end).trim();
            }
            return pdfTitle;
        }

        return "Easy Access Rules for Continuing Airworthiness";
    }

    /**
     * Extract version from filename (date part)
     * Example: "95CADA_2025-09-02_06.16.08_..." -> "2025-09-PDF"
     * For AMC/GM files, append "-AMC-GM" to differentiate from main regulation
     * For regular PDF files, append "-PDF" to differentiate from XML versions
     */
    private String extractVersion(final String filename) {
        String baseVersion = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        if (filename != null) {
            final Matcher matcher = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})").matcher(filename);
            if (matcher.find()) {
                baseVersion = matcher.group(1) + "-" + matcher.group(2);
            }

            // Append AMC-GM suffix if this is an AMC/GM file
            if (isAmcGmFile(filename)) {
                baseVersion = baseVersion + "-AMC-GM";
            } else {
                // Append PDF suffix for regular PDF files to differentiate from XML versions
                baseVersion = baseVersion + "-PDF";
            }
        }

        return baseVersion;
    }

    /**
     * Try to extract effective date from PDF content
     */
    private LocalDate extractEffectiveDate(final String rawText) {
        // Look for common date patterns in the text
        final Pattern datePattern = Pattern.compile("(?:Effective|Applicable|Entry into force).*?(\\d{1,2}\\s+\\w+\\s+\\d{4})",
                                              Pattern.CASE_INSENSITIVE);
        final Matcher matcher = datePattern.matcher(rawText);

        if (matcher.find()) {
            try {
                return LocalDate.parse(matcher.group(1), DATE_FORMATTER);
            } catch (final Exception e) {
                log.debug("Could not parse date: {}", matcher.group(1));
            }
        }

        // Default to current date if not found
        return LocalDate.now();
    }

    /**
     * Determine if this is an AMC/GM file from filename
     */
    private boolean isAmcGmFile(final String filename) {
        if (filename == null) {
            return false;
        }
        final String lower = filename.toLowerCase();
        return lower.contains("_acm_gm") || lower.contains("_amc_gm") ||
               lower.contains("amgm") || lower.contains("amc") || lower.contains("gm");
    }

    /**
     * Extract structured clauses from PDF raw text
     */
    private List<ClauseData> extractClauses(final String rawText, final boolean isAmcGm) {
        final List<ClauseData> clauses = new ArrayList<>();

        if (rawText == null || rawText.trim().isEmpty()) {
            log.warn("Empty raw text provided for clause extraction");
            return clauses;
        }

        final String[] lines = rawText.split("\\r?\\n");
        StringBuilder currentContent = new StringBuilder();
        String currentClauseId = null;
        String currentTitle = null;
        String currentType = null;
        String currentParent = null;
        int displayOrder = 0;
        int clauseNumber = 0;

        for (String line : lines) {
            line = line.trim();

            // Skip empty lines and page numbers
            if (line.isEmpty() || line.matches("^\\d+$") || line.matches("^Page\\s+\\d+.*")) {
                continue;
            }

            // Check if this line starts a new clause
            final ClauseIdentifier identifier = identifyClause(line, isAmcGm);

            if (identifier != null && identifier.isClauseStart) {
                // Save previous clause if exists
                if (currentClauseId != null) {
                    clauses.add(new ClauseData(
                            currentClauseId,
                            currentTitle != null ? currentTitle : currentClauseId,
                            currentContent.toString().trim(),
                            "", // linkedTo
                            currentType,
                            displayOrder++,
                            currentParent,
                            clauseNumber++
                    ));
                    currentContent = new StringBuilder();
                }

                // Start new clause
                currentClauseId = identifier.clauseId;
                currentTitle = identifier.title;
                currentType = identifier.clauseType;
                currentParent = identifier.parentClause;

                // Add remaining content if title is extracted from line
                if (identifier.title != null && line.length() > identifier.title.length()) {
                    final String remainingContent = line.substring(line.indexOf(identifier.title) + identifier.title.length()).trim();
                    if (!remainingContent.isEmpty()) {
                        currentContent.append(remainingContent).append("\n");
                    }
                }
            } else if (currentClauseId != null) {
                // Append to current clause content
                currentContent.append(line).append("\n");
            }
        }

        // Save last clause
        if (currentClauseId != null) {
            clauses.add(new ClauseData(
                    currentClauseId,
                    currentTitle != null ? currentTitle : currentClauseId,
                    currentContent.toString().trim(),
                    "",
                    currentType,
                    displayOrder,
                    currentParent,
                    clauseNumber
            ));
        }

        log.info("Extracted {} clauses from PDF text", clauses.size());
        return clauses;
    }

    /**
     * Identify if a line starts a new clause and extract its metadata
     */
    private ClauseIdentifier identifyClause(final String line, final boolean defaultIsAmcGm) {
        // Check for AMC pattern
        final Matcher amcMatcher = AMC_PATTERN.matcher(line);
        if (amcMatcher.find()) {
            final String reference = amcMatcher.group(1);
            final String clauseNumber = extractClauseNumberFromReference(reference);
            return new ClauseIdentifier(
                    clauseNumber != null ? "AMC_" + clauseNumber : "AMC_" + reference.split("\\s+")[0],
                    line,
                    "AMC",
                    extractParentFromReference(reference),
                    true
            );
        }

        // Check for GM pattern
        final Matcher gmMatcher = GM_PATTERN.matcher(line);
        if (gmMatcher.find()) {
            final String reference = gmMatcher.group(1);
            final String clauseNumber = extractClauseNumberFromReference(reference);
            return new ClauseIdentifier(
                    clauseNumber != null ? "GM_" + clauseNumber : "GM_" + reference.split("\\s+")[0],
                    line,
                    "GM",
                    extractParentFromReference(reference),
                    true
            );
        }

        // Check for M.A. pattern
        final Matcher maMatcher = MA_PATTERN.matcher(line);
        if (maMatcher.find()) {
            final String clauseId = maMatcher.group(1);
            final String title = maMatcher.group(2);
            return new ClauseIdentifier(
                    clauseId,
                    title != null ? clauseId + " " + title : clauseId,
                    defaultIsAmcGm ? "AMC" : "REQUIREMENT",
                    null,
                    true
            );
        }

        // Check for ML.A. pattern
        final Matcher mlMatcher = ML_PATTERN.matcher(line);
        if (mlMatcher.find()) {
            final String clauseId = mlMatcher.group(1);
            final String title = mlMatcher.group(2);
            return new ClauseIdentifier(
                    clauseId,
                    title != null ? clauseId + " " + title : clauseId,
                    defaultIsAmcGm ? "AMC" : "REQUIREMENT",
                    null,
                    true
            );
        }

        // Check for Article pattern
        final Matcher articleMatcher = ARTICLE_PATTERN.matcher(line);
        if (articleMatcher.find()) {
            final String articleNum = articleMatcher.group(1);
            final String title = articleMatcher.group(2);
            return new ClauseIdentifier(
                    "Article_" + articleNum,
                    title != null ? "Article " + articleNum + " " + title : "Article " + articleNum,
                    "REQUIREMENT",
                    null,
                    true
            );
        }

        // Check for Appendix pattern
        final Matcher appendixMatcher = APPENDIX_PATTERN.matcher(line);
        if (appendixMatcher.find()) {
            final String appendixNum = appendixMatcher.group(1);
            final String title = appendixMatcher.group(2);
            return new ClauseIdentifier(
                    "Appendix_" + appendixNum,
                    title != null ? "Appendix " + appendixNum + " " + title : "Appendix " + appendixNum,
                    "PROVISION",
                    null,
                    true
            );
        }

        // Check for section headers
        final Matcher sectionMatcher = SECTION_HEADER_PATTERN.matcher(line);
        if (sectionMatcher.find()) {
            final String sectionNum = sectionMatcher.group(1);
            final String title = sectionMatcher.group(2);
            return new ClauseIdentifier(
                    "SECTION_" + sectionNum,
                    title != null ? "SECTION " + sectionNum + " - " + title : line,
                    "HEADING",
                    null,
                    true
            );
        }

        return null;
    }

    /**
     * Extract parent clause from a reference string
     */
    private String extractParentFromReference(final String reference) {
        if (reference == null) {
            return null;
        }

        final Matcher maMatcher = Pattern.compile("(M\\.A\\.\\d+)").matcher(reference);
        if (maMatcher.find()) {
            return maMatcher.group(1);
        }

        final Matcher mlMatcher = Pattern.compile("(ML\\.A\\.\\d+)").matcher(reference);
        if (mlMatcher.find()) {
            return mlMatcher.group(1);
        }

        final Matcher articleMatcher = Pattern.compile("Article\\s+(\\d+)").matcher(reference);
        if (articleMatcher.find()) {
            return "Article_" + articleMatcher.group(1);
        }

        return null;
    }

    /**
     * Extract clause number from reference text (e.g., "M.A.101 Aircraft maintenance programme" -> "M.A.101")
     * This ensures clauseId stays under 100 characters for database compatibility
     */
    private String extractClauseNumberFromReference(final String reference) {
        if (reference == null) {
            return null;
        }

        // Try to extract M.A.XXX or ML.A.XXX pattern
        final Matcher maMatcher = Pattern.compile("^(M\\.A\\.\\d+(?:\\.\\d+)?)").matcher(reference);
        if (maMatcher.find()) {
            return maMatcher.group(1).replaceAll("\\.", "_");
        }

        final Matcher mlMatcher = Pattern.compile("^(ML\\.A\\.\\d+(?:\\.\\d+)?)").matcher(reference);
        if (mlMatcher.find()) {
            return mlMatcher.group(1).replaceAll("\\.", "_");
        }

        // Try to extract Article pattern
        final Matcher articleMatcher = Pattern.compile("^Article\\s+(\\d+)").matcher(reference);
        if (articleMatcher.find()) {
            return "Article_" + articleMatcher.group(1);
        }

        // Try to extract Part pattern
        final Matcher partMatcher = Pattern.compile("^Part-([A-Z]+)").matcher(reference);
        if (partMatcher.find()) {
            return "Part_" + partMatcher.group(1);
        }

        // If no pattern matches, truncate to ensure it fits in database
        String fallback = reference.split("\\s+")[0].replaceAll("[\\s.]+", "_");
        if (fallback.length() > 50) {
            fallback = fallback.substring(0, 50);
        }
        return fallback;
    }

    /**
         * Internal class to hold clause identification results
         */
        private record ClauseIdentifier(String clauseId, String title, String clauseType, String parentClause,
                                        boolean isClauseStart) {
    }
}
