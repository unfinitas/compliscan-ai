package com.unfinitas.backend.core.ingestion.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AccessLevel;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDateTime;

@Slf4j
@Entity
@Table(
        name = "paragraphs",
        indexes = {
                @Index(name = "idx_section_order", columnList = "section_id, paragraph_order"),
                @Index(name = "idx_moe_order", columnList = "moe_id, paragraph_order"),
                @Index(name = "idx_embedding_model", columnList = "embedding_model, embedded_at")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    private Section section;

    @Column(name = "paragraph_order", nullable = false)
    private Integer paragraphOrder;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "word_count")
    private Integer wordCount;

    /**
     * Vector embedding stored as JSON text for compatibility
     */
    @Setter
    @Column(name = "embedding", columnDefinition = "TEXT")
    private String embedding;

    @Setter
    @Column(name = "embedding_model", length = 100)
    private String embeddingModel;

    @Setter
    @Column(name = "embedded_at")
    private Instant embeddedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Paragraph(final MoeDocument moeDocument, final Integer paragraphOrder, final String content) {
        this.moeDocument = moeDocument;
        this.paragraphOrder = paragraphOrder;
        this.content = content;
        this.wordCount = calculateWordCount(content);
        this.createdAt = LocalDateTime.now();
    }

    public Paragraph(final MoeDocument moeDocument, final Section section, final Integer paragraphOrder, final String content) {
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

    public boolean needsEmbedding(final String currentModel) {
        return embedding == null
                || embeddingModel == null
                || !embeddingModel.equals(currentModel);
    }

    /**
     * Convert embedding string to float array for calculations
     */
    @Transient
    public float[] getEmbeddingArray() {
        if (embedding == null || embedding.isEmpty()) {
            log.debug("Paragraph {} has null/empty embedding", id);
            return null;
        }

        try {
            final String[] parts = embedding.substring(1, embedding.length() - 1).split(",");
            final float[] result = new float[parts.length];
            for (int i = 0; i < parts.length; i++) {
                result[i] = Float.parseFloat(parts[i].trim());
            }
            log.trace("Parsed embedding for paragraph {}: {} dimensions", id, result.length);
            return result;
        } catch (final Exception e) {
            log.error("Failed to parse embedding for paragraph {}: {}", id, e.getMessage());
            return null;
        }
    }

    /**
     * Set embedding from float array
     */
    public void setEmbeddingFromArray(final float[] arr) {
        if (arr == null) {
            this.embedding = null;
            return;
        }
        final StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(arr[i]);
        }
        sb.append("]");
        this.embedding = sb.toString();
    }

    private Integer calculateWordCount(final String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }
}
