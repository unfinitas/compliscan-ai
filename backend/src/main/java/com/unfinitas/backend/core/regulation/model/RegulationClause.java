package com.unfinitas.backend.core.regulation.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "regulation_clauses", indexes = {
        @Index(name = "idx_regulation_version", columnList = "regulation_version"),
        @Index(name = "idx_clause_id", columnList = "clause_id"),
        @Index(name = "idx_clause_type", columnList = "clause_type"),
        @Index(name = "idx_clause_regulation_id", columnList = "regulation_id")
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

    @Column(name = "embedding", columnDefinition = "TEXT")
    private String embedding;

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
