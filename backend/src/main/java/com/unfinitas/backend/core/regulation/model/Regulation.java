package com.unfinitas.backend.core.regulation.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "regulations", indexes = {
        @Index(name = "idx_regulation_code_version", columnList = "code, version"),
        @Index(name = "idx_regulation_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Regulation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 50)
    private String version;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "regulation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<RegulationClause> clauses = new ArrayList<>();

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

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

    public void addClause(final RegulationClause clause) {
        clauses.add(clause);
        clause.setRegulation(this);
        clause.setRegulationVersion(this.version);
    }

    public int getClauseCount() {
        return clauses != null ? clauses.size() : 0;
    }

    public List<RegulationClause> getRequirements() {
        return clauses.stream()
                .filter(RegulationClause::isRequirement)
                .toList();
    }

    public List<RegulationClause> getAmcClauses() {
        return clauses.stream()
                .filter(RegulationClause::isAmc)
                .toList();
    }

    public List<RegulationClause> getGmClauses() {
        return clauses.stream()
                .filter(RegulationClause::isGm)
                .toList();
    }
}
