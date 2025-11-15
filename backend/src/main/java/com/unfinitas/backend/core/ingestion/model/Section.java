package com.unfinitas.backend.core.ingestion.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

import java.time.LocalDateTime;

/**
 * Section entity representing the hierarchical structure of MOE documents.
 *
 * Supports EASA Part-145 MOE document structure:
 * - Part level (depth 0): "Part 1", "OSA 1", etc.
 * - Chapter level (depth 1): "1.1", "2.3", etc.
 * - Section level (depth 2): "1.4.1", "2.3.2", etc.
 */
@Entity
@Table(
        name = "sections",
        indexes = {
                @Index(name = "idx_moe_section", columnList = "moe_id, section_number"),
                @Index(name = "idx_parent", columnList = "parent_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moe_id", nullable = false)
    private MoeDocument moeDocument;

    /**
     * The section number (e.g., "Part 1", "1.4.1", "OSA 1")
     */
    @Column(name = "section_number", nullable = false, length = 50)
    private String sectionNumber;

    /**
     * The title of the section (e.g., "Management", "Corporate Commitment")
     */
    @Column(name = "section_title", length = 500)
    private String sectionTitle;

    /**
     * The hierarchical depth of this section:
     * - 0: Part level (e.g., "Part 1", "OSA 1")
     * - 1: Chapter level (e.g., "1.1", "2.3")
     * - 2: Section level (e.g., "1.4.1", "2.3.2")
     */
    @Column(name = "depth", nullable = false)
    private Integer depth;

    /**
     * Parent section in the tree structure (unidirectional).
     * Null for top-level sections (Parts).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Section parent;

    /**
     * Order of this section within the document.
     * Used for sequential display of sections.
     */
    @Column(name = "section_order", nullable = false)
    private Integer sectionOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Constructor for creating a section with hierarchical information.
     *
     * @param moeDocument The parent MOE document
     * @param sectionNumber The section number (e.g., "Part 1", "1.4.1")
     * @param sectionTitle The section title
     * @param depth The hierarchical depth (0=Part, 1=Chapter, 2=Section)
     * @param parent The parent section (null for top-level)
     * @param sectionOrder The order of this section in the document
     */
    public Section(MoeDocument moeDocument, String sectionNumber, String sectionTitle,
                   Integer depth, Section parent, Integer sectionOrder) {
        this.moeDocument = moeDocument;
        this.sectionNumber = sectionNumber;
        this.sectionTitle = sectionTitle;
        this.depth = depth;
        this.parent = parent;
        this.sectionOrder = sectionOrder;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
