package com.unfinitas.backend.core.regulation.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "regulation_clauses", indexes = {
        @Index(name = "idx_regulation_version", columnList = "regulation_version"),
        @Index(name = "idx_clause_id", columnList = "clause_id"),
        @Index(name = "idx_clause_type", columnList = "clause_type"),
        @Index(name = "idx_clause_regulation_id", columnList = "regulation_id"),
        @Index(name = "idx_clause_embedding_model", columnList = "embedding_model, embedded_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegulationClause {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "regulation_id", nullable = false)
    private Regulation regulation;

    @Column(name = "regulation_version", nullable = false, length = 50)
    private String regulationVersion;

    @Column(name = "clause_number", nullable = false)
    private Integer clauseNumber;

    @Column(name = "clause_id", nullable = false, length = 100)
    private String clauseId;

    @Column(name = "clause_title", length = 500)
    private String title;

    @Column(name = "clause_content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "linked_to", length = 100)
    private String linkedTo;

    @Column(name = "clause_type", length = 20)
    private String clauseType;

    @Column(name = "parent_clause", length = 100)
    private String parentClause;

    @Column(name = "display_order")
    private Integer displayOrder;

    /**
     * Vector embedding stored as TEXT for JPA compatibility
     */
    @Column(name = "embedding", columnDefinition = "TEXT")
    private String embedding;

    @Column(name = "embedding_model", length = 100)
    private String embeddingModel;

    @Column(name = "embedded_at")
    private Instant embeddedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
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
            return null;
        }
        String[] parts = embedding.substring(1, embedding.length() - 1).split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }

    /**
     * Set embedding from float array
     */
    public void setEmbeddingFromArray(float[] arr) {
        if (arr == null) {
            this.embedding = null;
            return;
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(arr[i]);
        }
        sb.append("]");
        this.embedding = sb.toString();
    }

    public boolean isRequirement() {
        return "REQUIREMENT".equals(clauseType);
    }

    public boolean isAmc() {
        return "AMC".equals(clauseType);
    }

    public boolean isGm() {
        return "GM".equals(clauseType);
    }
}
