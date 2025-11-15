package com.unfinitas.backend.core.regulation.parser;

import com.unfinitas.backend.core.regulation.dto.ClauseData;
import com.unfinitas.backend.core.regulation.dto.RegulationData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class EasaXmlParser {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM, yyyy");

    public RegulationData parseXml(final String filePath) throws Exception {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        final DocumentBuilder builder = factory.newDocumentBuilder();
        final Document document = builder.parse(filePath);
        document.getDocumentElement().normalize();

        // Extract regulation metadata from the root document element
        final Element documentElement = (Element) document.getElementsByTagName("er:document").item(0);
        final String sourceTitle = documentElement.getAttribute("source-title");

        // Parse regulation code and name
        final String[] titleParts = parseRegulationTitle(sourceTitle);
        final String regulationCode = titleParts[0];
        final String regulationName = titleParts[1];

        // Check if this is an AMC/GM file from the filepath
        String version = extractVersion(document);
        if (filePath != null && (filePath.toLowerCase().contains("_acm_gm") ||
                                 filePath.toLowerCase().contains("_amc_gm"))) {
            version = version + "-AMC-GM";
        }

        // Extract effective date
        final LocalDate effectiveDate = extractEffectiveDate(document);

        final List<ClauseData> clauses = new ArrayList<>();

        // Parse the hierarchical structure
        parseHierarchicalStructure(document, clauses);

        log.info("Parsed regulation: {} - {} with {} clauses", regulationCode, regulationName, clauses.size());

        return new RegulationData(
                regulationCode,
                version,
                regulationName,
                effectiveDate,
                clauses
        );
    }

    private String[] parseRegulationTitle(final String sourceTitle) {
        // Example: "Easy Access Rules for Continuing Airworthiness (Regulation (EU) No 1321/2014)"
        if (sourceTitle.contains("(Regulation")) {
            final int start = sourceTitle.indexOf("(Regulation");
            // Find the last closing parenthesis to get the complete regulation code
            final int end = sourceTitle.lastIndexOf(")");
            if (end > start) {
                final String code = sourceTitle.substring(start + 1, end).trim(); // "Regulation (EU) No 1321/2014"
                final String name = sourceTitle.substring(0, start).trim();
                return new String[]{code, name};
            }
        }
        return new String[]{"UNKNOWN", sourceTitle};
    }

    private String extractVersion(final Document document) {
        // Try to extract version from publication date or other metadata
        final Element documentElement = (Element) document.getElementsByTagName("er:document").item(0);
        final String pubTime = documentElement.getAttribute("pub-time");
        if (!pubTime.isEmpty()) {
            return pubTime.substring(0, 7); // Use YYYY-MM as version
        }
        return "1.0";
    }

    private LocalDate extractEffectiveDate(final Document document) {
        try {
            // Look for the most recent applicability date
            final NodeList topics = document.getElementsByTagName("er:topic");
            LocalDate latestDate = null;

            for (int i = 0; i < topics.getLength(); i++) {
                final Element topic = (Element) topics.item(i);
                final String applicabilityDate = topic.getAttribute("ApplicabilityDate");
                if (!applicabilityDate.isEmpty()) {
                    final LocalDate date = parseEasaDate(applicabilityDate);
                    if (date != null && (latestDate == null || date.isAfter(latestDate))) {
                        latestDate = date;
                    }
                }
            }
            return latestDate != null ? latestDate : LocalDate.now();
        } catch (final Exception e) {
            log.warn("Could not extract effective date", e);
            return LocalDate.now();
        }
    }

    private void parseHierarchicalStructure(final Document document, final List<ClauseData> clauses) {
        // Start parsing from the main TOC structure
        final Element rootToc = (Element) document.getElementsByTagName("er:toc").item(0);
        if (rootToc != null) {
            parseTocElement(rootToc, clauses, null, 0);
        } else {
            // Fallback: parse all topics and headings directly
            parseAllElements(document, clauses);
        }
    }

    private void parseTocElement(final Element tocElement, final List<ClauseData> clauses, final String parentClause, final int depth) {
        final NodeList children = tocElement.getChildNodes();
        int order = clauses.size();

        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                final Element element = (Element) child;

                if ("er:topic".equals(element.getNodeName())) {
                    final ClauseData clause = parseTopicElement(element, order++, parentClause);
                    if (clause != null) {
                        clauses.add(clause);

                        // Recursively parse nested TOC structures
                        final NodeList nestedTocs = element.getElementsByTagName("er:toc");
                        for (int j = 0; j < nestedTocs.getLength(); j++) {
                            parseTocElement((Element) nestedTocs.item(j), clauses, clause.clauseId(), depth + 1);
                        }
                    }
                } else if ("er:heading".equals(element.getNodeName())) {
                    final ClauseData clause = parseHeadingElement(element, order++, parentClause);
                    if (clause != null) {
                        clauses.add(clause);
                    }
                }
            }
        }
    }

    private ClauseData parseTopicElement(final Element topic, final int order, final String parentClause) {
        final String sourceTitle = topic.getAttribute("source-title");
        if (sourceTitle == null || sourceTitle.trim().isEmpty()) {
            return null;
        }

        final String erulesId = topic.getAttribute("ERulesId");
        final String clauseId = erulesId.isEmpty() ? "TOPIC_" + order : erulesId;

        // Determine clause type
        final String clauseType = determineClauseType(sourceTitle);

        // Extract parent clause from ParentIR attribute if available
        String actualParentClause = parentClause;
        final String parentIR = topic.getAttribute("ParentIR");
        if (!parentIR.isEmpty() && !"Powers and recital".equals(parentIR)) {
            actualParentClause = parentIR;
        }

        return new ClauseData(
                clauseId,
                sourceTitle.trim(),
                buildContentFromTopic(topic),
                "", // linkedTo - you can populate this based on your business logic
                clauseType,
                order,
                actualParentClause,
                order
        );
    }

    private ClauseData parseHeadingElement(final Element heading, final int order, final String parentClause) {
        final String title = heading.getAttribute("title");
        if (title == null || title.trim().isEmpty()) {
            return null;
        }

        return new ClauseData(
                "HEADING_" + order,
                title.trim(),
                "", // Headings typically don't have content
                "", // linkedTo
                "HEADING",
                order,
                parentClause,
                order
        );
    }

    private void parseAllElements(final Document document, final List<ClauseData> clauses) {
        // Parse all topics
        final NodeList topics = document.getElementsByTagName("er:topic");
        for (int i = 0; i < topics.getLength(); i++) {
            final Element topic = (Element) topics.item(i);
            final ClauseData clause = parseTopicElement(topic, i, null);
            if (clause != null) {
                clauses.add(clause);
            }
        }

        // Parse all headings
        final NodeList headings = document.getElementsByTagName("er:heading");
        for (int i = 0; i < headings.getLength(); i++) {
            final Element heading = (Element) headings.item(i);
            final ClauseData clause = parseHeadingElement(heading, topics.getLength() + i, null);
            if (clause != null) {
                clauses.add(clause);
            }
        }
    }

    private String determineClauseType(final String title) {
        if (title == null) return "PROVISION";

        final String upperTitle = title.toUpperCase();

        if (title.startsWith("GM") || upperTitle.contains("GUIDANCE MATERIAL")) {
            return "GM";
        } else if (title.startsWith("AMC") || upperTitle.contains("ACCEPTABLE MEANS OF COMPLIANCE")) {
            return "AMC";
        } else if (title.startsWith("Article") || title.matches("^M\\.A\\.\\d+.*")) {
            return "REQUIREMENT";
        } else if (title.contains("Definition") || title.contains("Definitions")) {
            return "DEFINITION";
        } else if (title.contains("Scope") || title.contains("Application")) {
            return "SCOPE";
        } else {
            return "PROVISION";
        }
    }

    private String buildContentFromTopic(final Element topic) {
        final StringBuilder content = new StringBuilder();

        // Add relevant metadata as content
        final String domain = topic.getAttribute("Domain");
        if (!domain.isEmpty()) {
            content.append("Domain: ").append(domain).append("\n\n");
        }

        final String activityType = topic.getAttribute("ActivityType");
        if (!activityType.isEmpty()) {
            content.append("Activities: ").append(activityType).append("\n\n");
        }

        final String aircraftUse = topic.getAttribute("AircraftUse");
        if (!aircraftUse.isEmpty()) {
            content.append("Aircraft Use: ").append(aircraftUse).append("\n\n");
        }

        final String aircraftCategory = topic.getAttribute("AircraftCategory");
        if (!aircraftCategory.isEmpty()) {
            content.append("Aircraft Category: ").append(aircraftCategory).append("\n\n");
        }

        final String regulatedEntity = topic.getAttribute("RegulatedEntity");
        if (!regulatedEntity.isEmpty()) {
            content.append("Regulated Entities: ").append(regulatedEntity).append("\n\n");
        }

        final String regulatorySource = topic.getAttribute("RegulatorySource");
        if (!regulatorySource.isEmpty()) {
            content.append("Regulatory Source: ").append(regulatorySource).append("\n\n");
        }

        final String regulatorySubject = topic.getAttribute("RegulatorySubject");
        if (!regulatorySubject.isEmpty()) {
            content.append("Regulatory Subject: ").append(regulatorySubject).append("\n\n");
        }

        // Add applicability information
        final String applicabilityDate = topic.getAttribute("ApplicabilityDate");
        if (!applicabilityDate.isEmpty()) {
            content.append("Applicability Date: ").append(applicabilityDate).append("\n");
        }

        final String entryIntoForceDate = topic.getAttribute("EntryIntoForceDate");
        if (!entryIntoForceDate.isEmpty()) {
            content.append("Entry Into Force Date: ").append(entryIntoForceDate).append("\n");
        }

        return content.toString().trim();
    }

    private LocalDate parseEasaDate(final String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            // Handle various EASA date formats
            if (dateStr.matches("\\d{1,2} [A-Za-z]+, \\d{4}")) {
                return LocalDate.parse(dateStr, DATE_FORMATTER);
            } else if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDate.parse(dateStr);
            } else if (dateStr.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
                final String[] parts = dateStr.split("/");
                return LocalDate.of(Integer.parseInt(parts[2]), Integer.parseInt(parts[1]), Integer.parseInt(parts[0]));
            }
        } catch (final Exception e) {
            log.warn("Failed to parse date: {}", dateStr, e);
        }
        return null;
    }
}
