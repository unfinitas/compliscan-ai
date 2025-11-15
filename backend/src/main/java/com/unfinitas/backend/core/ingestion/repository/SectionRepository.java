package com.unfinitas.backend.core.ingestion.repository;

import com.unfinitas.backend.core.ingestion.model.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Section entity operations.
 * Supports hierarchical queries for MOE document sections.
 */
@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {

    /**
     * Find a section by MOE document and section number.
     *
     * @param moeId MOE document UUID
     * @param sectionNumber Section number (e.g., "Part 1", "1.4.1")
     * @return Optional Section
     */
    Optional<Section> findByMoeDocumentIdAndSectionNumber(UUID moeId, String sectionNumber);

    /**
     * Find all sections for a MOE document, ordered by section order.
     *
     * @param moeId MOE document UUID
     * @return List of sections
     */
    List<Section> findByMoeDocumentIdOrderBySectionOrder(UUID moeId);

    /**
     * Find all top-level sections (Parts) for a MOE document.
     *
     * @param moeId MOE document UUID
     * @return List of Part-level sections
     */
    @Query("SELECT s FROM Section s WHERE s.moeDocument.id = :moeId AND s.parent IS NULL ORDER BY s.sectionOrder")
    List<Section> findTopLevelSections(@Param("moeId") UUID moeId);

    /**
     * Find all child sections of a parent section.
     *
     * @param parentId Parent section ID
     * @return List of child sections
     */
    @Query("SELECT s FROM Section s WHERE s.parent.id = :parentId ORDER BY s.sectionOrder")
    List<Section> findByParentId(@Param("parentId") Long parentId);

    /**
     * Find all sections at a specific depth level.
     *
     * @param moeId MOE document UUID
     * @param depth Hierarchical depth (0=Part, 1=Chapter, 2=Section)
     * @return List of sections at the specified depth
     */
    List<Section> findByMoeDocumentIdAndDepthOrderBySectionOrder(UUID moeId, Integer depth);

    /**
     * Find a section and all its descendants using materialized path pattern.
     *
     * @param moeId MOE document UUID
     * @param sectionNumber Section number prefix
     * @return List of matching sections
     */
    @Query("SELECT s FROM Section s WHERE s.moeDocument.id = :moeId " +
           "AND (s.sectionNumber = :sectionNumber OR s.sectionNumber LIKE CONCAT(:sectionNumber, '.%')) " +
           "ORDER BY s.sectionOrder")
    List<Section> findSectionAndDescendants(@Param("moeId") UUID moeId,
                                            @Param("sectionNumber") String sectionNumber);

    /**
     * Count sections in a MOE document.
     *
     * @param moeId MOE document UUID
     * @return Number of sections
     */
    long countByMoeDocumentId(UUID moeId);
}
