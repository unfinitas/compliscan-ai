package com.unfinitas.backend.core.regulation.parser;

import com.unfinitas.backend.core.regulation.dto.ClauseData;
import com.unfinitas.backend.core.regulation.dto.RegulationData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser for EASA Easy Access Rules XML
 * Extracts Part-145 requirements, AMC, and GM clauses
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EasaXmlParser {

    /**
     * Parse EASA Easy Access Rules XML and extract Part-145 regulation data
     */
    public RegulationData parseXml(final String xmlPath) throws Exception {
        log.info("Parsing EASA Part-145 XML: {}", xmlPath);

        final List<ClauseData> clauses = new ArrayList<>();

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        final DocumentBuilder builder = factory.newDocumentBuilder();
        final Document doc = builder.parse(new File(xmlPath));

        // Find all topics (er:topic elements)
        final NodeList topics = doc.getElementsByTagNameNS("http://www.easa.europa.eu/erules-export", "topic");

        int order = 1;
        for (int i = 0; i < topics.getLength(); i++) {
            final Element topic = (Element) topics.item(i);

            final String sourceTitle = topic.getAttribute("source-title");
            final String typeOfContent = topic.getAttribute("TypeOfContent");
            final String regulatorySubject = topic.getAttribute("RegulatorySubject");

            // Only process Part-145 content
            if (!regulatorySubject.contains("Part-145")) {
                continue;
            }

            // Determine clause type and ID
            final ClauseData clause = parseClause(topic, sourceTitle, typeOfContent, order);
            if (clause != null) {
                clauses.add(clause);
                order++;
                log.debug("Parsed: {} - {}", clause.clauseId(), clause.title());
            }
        }

        log.info("Parsed {} Part-145 clauses", clauses.size());

        return new RegulationData(
                "EASA-145",
                "EU 1321/2014",
                "Part-145 Approved Maintenance Organizations",
                LocalDate.now(),
                clauses
        );
    }

    /**
     * Parse individual clause from topic element
     */
    private ClauseData parseClause(final Element topic, final String sourceTitle, final String typeOfContent, final int order) {
        if (sourceTitle == null || sourceTitle.isBlank()) {
            return null;
        }

        final String clauseType = determineClauseType(sourceTitle, typeOfContent);
        if (clauseType == null) {
            return null; // Skip non-relevant content
        }

        final String clauseId = extractClauseId(sourceTitle);
        final String title = extractTitle(sourceTitle);
        final String linkedTo = extractLinkedTo(sourceTitle, clauseType);

        // Get content from child elements (would need full XML to extract text content)
        final String content = ""; // XML content would be in child elements

        return new ClauseData(
                clauseId,
                title,
                content,
                linkedTo,
                clauseType,
                order
        );
    }

    /**
     * Determine if this is a REQUIREMENT, AMC, or GM
     */
    private String determineClauseType(final String sourceTitle, String typeOfContent) {
        if (typeOfContent == null) typeOfContent = "";

        // Check for AMC
        if (sourceTitle.startsWith("AMC") ||
                typeOfContent.contains("Acceptable means of compliance")) {
            return "AMC";
        }

        // Check for GM
        if (sourceTitle.startsWith("GM") ||
                typeOfContent.contains("Guidance material")) {
            return "GM";
        }

        // Check for requirements (145.A.XX or 145.B.XX pattern)
        if (sourceTitle.matches("^145\\.[AB]\\.\\d+.*")) {
            return "REQUIREMENT";
        }

        return null; // Not a relevant clause
    }

    /**
     * Extract clause ID from source title
     * Examples:
     * "145.A.30 Personnel requirements" -> "145.A.30"
     * "AMC1 145.A.30 Personnel requirements" -> "AMC1.145.A.30"
     * "GM1 145.A.30 Personnel requirements" -> "GM1.145.A.30"
     */
    private String extractClauseId(final String sourceTitle) {
        // Handle AMC/GM format
        if (sourceTitle.startsWith("AMC") || sourceTitle.startsWith("GM")) {
            final String[] parts = sourceTitle.split("\\s+");
            if (parts.length >= 2) {
                final String prefix = parts[0]; // AMC1, GM1, etc.
                final String clauseNum = parts[1]; // 145.A.30
                return prefix + "." + clauseNum;
            }
        }

        // Handle requirement format (145.A.30)
        final String[] parts = sourceTitle.split("\\s+");
        if (parts.length > 0 && parts[0].matches("145\\.[AB]\\.\\d+.*")) {
            return parts[0];
        }

        return sourceTitle.split("\\s+")[0];
    }

    /**
     * Extract title from source title (remove clause ID)
     */
    private String extractTitle(final String sourceTitle) {
        // Remove clause ID prefix
        if (sourceTitle.startsWith("AMC") || sourceTitle.startsWith("GM")) {
            final String[] parts = sourceTitle.split("\\s+", 3);
            return parts.length >= 3 ? parts[2] : sourceTitle;
        }

        final String[] parts = sourceTitle.split("\\s+", 2);
        return parts.length >= 2 ? parts[1] : sourceTitle;
    }

    /**
     * Extract which requirement clause this AMC/GM links to
     */
    private String extractLinkedTo(final String sourceTitle, final String clauseType) {
        if (!"AMC".equals(clauseType) && !"GM".equals(clauseType)) {
            return null; // Only AMC/GM link to requirements
        }

        // Extract base requirement from AMC/GM title
        // "AMC1 145.A.30 Personnel" -> "145.A.30"
        final String[] parts = sourceTitle.split("\\s+");
        if (parts.length >= 2) {
            final String clauseNum = parts[1];
            if (clauseNum.matches("145\\.[AB]\\.\\d+.*")) {
                // Remove any sub-clause notation like (a), (b)
                return clauseNum.split("\\(")[0];
            }
        }

        return null;
    }
}

