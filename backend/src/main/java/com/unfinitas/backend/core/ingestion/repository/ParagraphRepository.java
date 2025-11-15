package com.unfinitas.backend.core.ingestion.repository;

import com.unfinitas.backend.core.ingestion.model.ContentType;
import com.unfinitas.backend.core.ingestion.model.MoeDocument;
import com.unfinitas.backend.core.ingestion.model.Paragraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Paragraph persistence with hierarchical section query support
 */
public interface ParagraphRepository extends JpaRepository<Paragraph, Long> {

    // ========== Existing Flat Query Methods ==========

    List<Paragraph> findByMoeDocument(MoeDocument moeDocument);

    /**
     * Find all paragraphs for a given MOE document, ordered by paragraph order
     */
    List<Paragraph> findByMoeDocumentIdOrderByParagraphOrder(UUID moeId);

    /**
     * Find all paragraphs with a specific section number
     */
    List<Paragraph> findBySectionNumber(String sectionNumber);

    /**
     * Count paragraphs for a given MOE document
     */
    long countByMoeDocumentId(UUID moeId);

    /**
     * Delete all paragraphs for a given MOE document
     */
    void deleteByMoeDocumentId(UUID moeId);

    // ========== Hierarchical Section Query Methods ==========

    /**
     * Get table of contents (section headings only) for a MOE document.
     * Returns only paragraphs with contentType = HEADING, ordered by document order.
     *
     * Used for building the MOE document structure/navigation.
     *
     * @param moeId The MOE document ID
     * @return List of heading paragraphs
     */
    @Query("SELECT p FROM Paragraph p WHERE p.moeDocument.id = :moeId " +
           "AND p.contentType = 'HEADING' " +
           "ORDER BY p.paragraphOrder")
    List<Paragraph> findTableOfContents(@Param("moeId") UUID moeId);

    /**
     * Find a specific section and all its descendants using materialized path.
     * For example, querying "1.4" returns: "1.4", "1.4.1", "1.4.2", "1.4.3.1", etc.
     *
     * Uses LIKE pattern matching on section numbers for efficient querying.
     *
     * @param moeId The MOE document ID
     * @param sectionNumber The parent section number (e.g., "1.4", "Part 1")
     * @return List of paragraphs in the section and its descendants
     */
    @Query("SELECT p FROM Paragraph p WHERE p.moeDocument.id = :moeId " +
           "AND (p.sectionNumber = :sectionNumber " +
           "     OR p.sectionNumber LIKE CONCAT(:sectionNumber, '.%')) " +
           "ORDER BY p.paragraphOrder")
    List<Paragraph> findBySectionAndDescendants(
        @Param("moeId") UUID moeId,
        @Param("sectionNumber") String sectionNumber
    );

    /**
     * Find all paragraphs within a specific Part (e.g., "Part 1", "OSA 1").
     * Returns the Part itself and all sections under it (1.1, 1.2, 1.4.1, etc.).
     *
     * This is a specialized version of findBySectionAndDescendants for Part-level queries.
     *
     * @param moeId The MOE document ID
     * @param partNumber The Part number (e.g., "Part 1", "OSA 1")
     * @return List of all paragraphs in the Part
     */
    @Query("SELECT p FROM Paragraph p WHERE p.moeDocument.id = :moeId " +
           "AND (p.sectionNumber = :partNumber " +
           "     OR p.parentSectionNumber = :partNumber " +
           "     OR p.sectionNumber LIKE CONCAT(:partNumber, '.%')) " +
           "ORDER BY p.paragraphOrder")
    List<Paragraph> findByPart(
        @Param("moeId") UUID moeId,
        @Param("partNumber") String partNumber
    );

    /**
     * Find direct children of a section (not grandchildren).
     * For example, children of "1.4" are: "1.4.1", "1.4.2", "1.4.3" (but NOT "1.4.1.1").
     *
     * @param moeId The MOE document ID
     * @param parentSection The parent section number
     * @return List of direct child paragraphs, ordered by section number
     */
    List<Paragraph> findByMoeDocumentIdAndParentSectionNumberOrderBySectionNumber(
        UUID moeId, String parentSection
    );

    /**
     * Find all top-level sections at a specific depth.
     * Examples:
     * - depth=0: All Part-level sections ("Part 1", "Part 2", "OSA 1")
     * - depth=1: All chapter-level sections ("1.1", "1.2", "2.1")
     * - depth=2: All section-level sections ("1.4.1", "2.3.2")
     *
     * @param moeId The MOE document ID
     * @param depth The hierarchical depth (0, 1, or 2)
     * @return List of paragraphs at the specified depth
     */
    List<Paragraph> findByMoeDocumentIdAndSectionDepthOrderByParagraphOrder(
        UUID moeId, Integer depth
    );

    /**
     * Find all paragraphs of a specific content type.
     * Useful for filtering headings vs regular content.
     *
     * @param moeId The MOE document ID
     * @param contentType The content type (HEADING or PARAGRAPH)
     * @return List of paragraphs with the specified content type
     */
    List<Paragraph> findByMoeDocumentIdAndContentTypeOrderByParagraphOrder(
        UUID moeId, ContentType contentType
    );

    /**
     * Search for paragraphs within a specific section hierarchy.
     * Combines section filtering with full-text search.
     *
     * @param moeId The MOE document ID
     * @param sectionPrefix The section number prefix (e.g., "1.4" to search within section 1.4)
     * @param searchTerm The search term (case-insensitive)
     * @return List of matching paragraphs
     */
    @Query("SELECT p FROM Paragraph p WHERE p.moeDocument.id = :moeId " +
           "AND (p.sectionNumber = :sectionPrefix " +
           "     OR p.sectionNumber LIKE CONCAT(:sectionPrefix, '.%')) " +
           "AND LOWER(p.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY p.paragraphOrder")
    List<Paragraph> searchInSection(
        @Param("moeId") UUID moeId,
        @Param("sectionPrefix") String sectionPrefix,
        @Param("searchTerm") String searchTerm
    );
}
