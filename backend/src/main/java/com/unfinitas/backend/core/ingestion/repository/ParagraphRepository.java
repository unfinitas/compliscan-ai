package com.unfinitas.backend.core.ingestion.repository;

import com.unfinitas.backend.core.ingestion.model.MoeDocument;
import com.unfinitas.backend.core.ingestion.model.Paragraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Paragraph persistence with hierarchical and section-based queries.
 */
public interface ParagraphRepository extends JpaRepository<Paragraph, Long> {

    List<Paragraph> findByMoeDocument(MoeDocument moeDocument);

    /**
     * Find all paragraphs for a given MOE document in document order.
     */
    List<Paragraph> findByMoeDocumentIdOrderByParagraphOrder(UUID moeId);

    /**
     * Count paragraphs for a given MOE document.
     */
    long countByMoeDocumentId(UUID moeId);

    /**
     * Delete all paragraphs under a MOE document.
     */
    void deleteByMoeDocumentId(UUID moeId);

    /**
     * Find paragraphs without any section assignment.
     */
    List<Paragraph> findByMoeDocumentIdAndSectionIsNullOrderByParagraphOrder(UUID moeId);

    /**
     * Count paragraphs in a specific section.
     */
    long countBySectionId(Long sectionId);


    // ============================================================
    // SECTION QUERIES
    // ============================================================

    /**
     * Find paragraphs inside a specific section.
     */
    List<Paragraph> findBySectionIdOrderByParagraphOrder(Long sectionId);

    /**
     * Query paragraphs at a specific depth (0=Part, 1=Chapter, 2=Section, etc.).
     */
    @Query("""
            SELECT p FROM Paragraph p JOIN p.section s
            WHERE p.moeDocument.id = :moeId
              AND s.depth = :depth
            ORDER BY p.paragraphOrder
            """)
    List<Paragraph> findByDepth(@Param("moeId") UUID moeId, @Param("depth") Integer depth);

    /**
     * Find paragraphs inside a section and all its descendants using materialized path.
     * e.g., "1.4" → "1.4", "1.4.1", "1.4.2", "1.4.2.1", etc.
     */
    @Query("""
            SELECT p FROM Paragraph p JOIN p.section s
            WHERE p.moeDocument.id = :moeId
              AND (s.sectionNumber = :sectionNumber OR s.sectionNumber LIKE CONCAT(:sectionNumber, '.%'))
            ORDER BY p.paragraphOrder
            """)
    List<Paragraph> findBySectionAndDescendants(
            @Param("moeId") UUID moeId,
            @Param("sectionNumber") String sectionNumber
    );

    /**
     * Find all paragraphs in a Part (e.g., "Part 1", "OSA 1") and its nested sections.
     */
    @Query("""
            SELECT p FROM Paragraph p JOIN p.section s
            WHERE p.moeDocument.id = :moeId
              AND (
                    s.sectionNumber = :partNumber
                 OR s.sectionNumber LIKE CONCAT(:partNumber, '.%')
                 OR (s.parent IS NOT NULL AND s.parent.sectionNumber = :partNumber)
              )
            ORDER BY p.paragraphOrder
            """)
    List<Paragraph> findByPart(
            @Param("moeId") UUID moeId,
            @Param("partNumber") String partNumber
    );

    /**
     * Find paragraphs in sections at a specific depth.
     */
    List<Paragraph> findByMoeDocumentIdAndSectionDepthOrderByParagraphOrder(
            UUID moeId, Integer depth
    );

    /**
     * Search inside a section hierarchy with content filtering.
     */
    @Query("""
            SELECT p FROM Paragraph p JOIN p.section s
            WHERE p.moeDocument.id = :moeId
              AND (s.sectionNumber = :sectionPrefix OR s.sectionNumber LIKE CONCAT(:sectionPrefix, '.%'))
              AND LOWER(p.content) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            ORDER BY p.paragraphOrder
            """)
    List<Paragraph> searchInSection(
            @Param("moeId") UUID moeId,
            @Param("sectionPrefix") String sectionPrefix,
            @Param("searchTerm") String searchTerm
    );

    /**
     * Search across the entire document.
     */
    @Query("""
            SELECT p FROM Paragraph p
            WHERE p.moeDocument.id = :moeId
              AND LOWER(p.content) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            ORDER BY p.paragraphOrder
            """)
    List<Paragraph> searchInDocument(
            @Param("moeId") UUID moeId,
            @Param("searchTerm") String searchTerm
    );

    List<Paragraph> findByEmbeddingIsNull();

    List<Paragraph> findByEmbeddingModelNot(String model);

    int countByEmbeddingIsNull();

    int countByMoeDocumentIdAndEmbeddingIsNotNull(UUID docId);

    List<Paragraph> findByMoeDocumentId(UUID id);

}
