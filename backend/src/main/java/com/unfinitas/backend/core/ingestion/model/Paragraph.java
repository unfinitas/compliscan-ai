package com.unfinitas.backend.core.ingestion.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

import java.time.LocalDateTime;

/**
 * Paragraph entity representing actual text content from an MOE document.
 * Each paragraph belongs to a specific Section in the hierarchical structure.
 */
@Entity
@Table(
        name = "paragraphs",
        indexes = {
                @Index(name = "idx_section_order", columnList = "section_id, paragraph_order"),
                @Index(name = "idx_moe_order", columnList = "moe_id, paragraph_order")
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

    /**
     * The section this paragraph belongs to.
     * Null for paragraphs that don't belong to any specific section.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    private Section section;

    /**
     * Order of this paragraph within the entire document.
     * Used for sequential reading and maintaining original document order.
     */
    @Column(name = "paragraph_order", nullable = false)
    private Integer paragraphOrder;

    /**
     * The actual text content of this paragraph.
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "word_count")
    private Integer wordCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Constructor for creating a paragraph without section association.
     *
     * @param moeDocument The parent MOE document
     * @param paragraphOrder The order of this paragraph in the document
     * @param content The text content
     */
    public Paragraph(MoeDocument moeDocument, Integer paragraphOrder, String content) {
        this.moeDocument = moeDocument;
        this.paragraphOrder = paragraphOrder;
        this.content = content;
        this.wordCount = calculateWordCount(content);
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Constructor for creating a paragraph with section association.
     *
     * @param moeDocument The parent MOE document
     * @param section The section this paragraph belongs to
     * @param paragraphOrder The order of this paragraph in the document
     * @param content The text content
     */
    public Paragraph(MoeDocument moeDocument, Section section, Integer paragraphOrder, String content) {
        this.moeDocument = moeDocument;
        this.section = section;
        this.paragraphOrder = paragraphOrder;
        this.content = content;
        this.wordCount = calculateWordCount(content);
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (wordCount == null && content != null) {
            wordCount = calculateWordCount(content);
        }
    }

    private Integer calculateWordCount(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return text.trim().split("\s+").length;
    }
}
