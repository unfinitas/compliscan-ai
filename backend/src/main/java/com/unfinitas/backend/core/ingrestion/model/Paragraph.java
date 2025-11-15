package com.unfinitas.backend.core.ingrestion.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Paragraph entity representing a normalized section of text from an MOE document
 */
@Entity
@Table(name = "paragraphs")
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

    @Column(name = "paragraph_order", nullable = false)
    private Integer paragraphOrder;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "word_count")
    private Integer wordCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructor without section number
    public Paragraph(final MoeDocument moeDocument, final Integer paragraphOrder, final String content) {
        this.moeDocument = moeDocument;
        this.paragraphOrder = paragraphOrder;
        this.content = content;
        this.wordCount = calculateWordCount(content);
        this.createdAt = LocalDateTime.now();
    }

    // Constructor with section number
    public Paragraph(final MoeDocument moeDocument, final String sectionNumber, final Integer paragraphOrder, final String content) {
        this.moeDocument = moeDocument;
        this.sectionNumber = sectionNumber;
        this.paragraphOrder = paragraphOrder;
        this.content = content;
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

    // Domain methods

    protected void assignToDocument(final MoeDocument document) {
        this.moeDocument = document;
    }

    // Helper method

    private Integer calculateWordCount(final String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }
}
