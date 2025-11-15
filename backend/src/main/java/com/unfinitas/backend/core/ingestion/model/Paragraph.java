package com.unfinitas.backend.core.ingestion.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

import java.time.LocalDateTime;

/**
 * Paragraph entity representing a normalized section of text from an MOE document
 */
@Entity
@Table(
        name = "paragraphs",
        indexes = {
                @Index
                (name = "idx_moe_order", columnList = "moe_id, paragraph_order")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Paragraph {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moe_id", nullable = false)
    private MoeDocument moeDocument;

    @Column(name = "section_number", length = 50)
    private String sectionNumber;

    /**
     * The title of the section (e.g., "Management", "Corporate Commitment")
     * Extracted from section headings like "Part 1 - Management" or "1.1 Corporate Commitment"
     */
    @Column(name = "section_title", length = 500)
    private String sectionTitle;

    /**
     * The hierarchical depth of this section in the MOE document structure.
     * - 0: Part level (e.g., "Part 1", "OSA 1")
     * - 1: Chapter level (e.g., "1.1", "2.3")
     * - 2: Section level (e.g., "1.4.1", "2.3.2")
     * - null: Regular paragraph (not a section heading)
     */
    @Column(name = "section_depth")
    private Integer sectionDepth;

    /**
     * The section number of the parent section.
     * Examples:
     * - "1.4.1" has parent "1.4"
     * - "1.1" has parent "Part 1" or "OSA 1"
     * - "Part 1" has parent null (top level)
     */
    @Column(name = "parent_section_number", length = 50)
    private String parentSectionNumber;

    /**
     * The type of content this paragraph contains.
     * HEADING: Section heading only
     * PARAGRAPH: Regular content or mixed heading+content
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", length = 20)
    private ContentType contentType = ContentType.PARAGRAPH;

    @Column(name = "paragraph_order", nullable = false)
    private Integer paragraphOrder;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "word_count")
    private Integer wordCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructor without section number
    public Paragraph(MoeDocument moeDocument, Integer paragraphOrder, String content) {
        this.moeDocument = moeDocument;
        this.paragraphOrder = paragraphOrder;
        this.content = content;
        this.wordCount = calculateWordCount(content);
        this.createdAt = LocalDateTime.now();
    }

    // Constructor with section number
    public Paragraph(MoeDocument moeDocument, String sectionNumber, Integer paragraphOrder, String content) {
        this.moeDocument = moeDocument;
        this.sectionNumber = sectionNumber;
        this.paragraphOrder = paragraphOrder;
        this.content = content;
        this.wordCount = calculateWordCount(content);
        this.createdAt = LocalDateTime.now();
    }

    // Constructor with full hierarchical section information
    public Paragraph(MoeDocument moeDocument, String sectionNumber, String sectionTitle,
                     Integer sectionDepth, String parentSectionNumber, Integer paragraphOrder,
                     String content, ContentType contentType) {
        this.moeDocument = moeDocument;
        this.sectionNumber = sectionNumber;
        this.sectionTitle = sectionTitle;
        this.sectionDepth = sectionDepth;
        this.parentSectionNumber = parentSectionNumber;
        this.paragraphOrder = paragraphOrder;
        this.content = content;
        this.contentType = contentType;
        this.wordCount = calculateWordCount(content);
        this.createdAt = LocalDateTime.now();
    }

    // Lifecycle callbacks

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (wordCount == null && content != null) {
            wordCount = calculateWordCount(content);
        }
    }

    // Helper method

    private Integer calculateWordCount(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return text.trim().split("\s+").length;
    }
}
