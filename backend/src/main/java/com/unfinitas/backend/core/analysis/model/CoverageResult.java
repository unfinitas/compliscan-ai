package com.unfinitas.backend.core.analysis.model;

import com.unfinitas.backend.core.analysis.model.enums.CoverageStatus;
import com.unfinitas.backend.core.analysis.model.enums.MatchType;
import com.unfinitas.backend.core.ingrestion.model.Paragraph;
import com.unfinitas.backend.core.regulation.model.RegulatoryLevel;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "coverage_results", indexes = {
        @Index(name = "idx_coverage_analysis_id", columnList = "analysis_id"),
        @Index(name = "idx_coverage_clause_id", columnList = "clause_id"),
        @Index(name = "idx_coverage_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CoverageResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private AnalysisResult analysisResult;

    @Column(name = "clause_id", nullable = false, length = 50)
    private String clauseId;

    @Column(name = "clause_title")
    private String clauseTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "regulatory_level", length = 20)
    private RegulatoryLevel regulatoryLevel;

    // Coverage assessment
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CoverageStatus status;

    @Column(precision = 5, scale = 4)
    private BigDecimal similarity;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", length = 20)
    private MatchType matchType;

    // Matched content
    @ManyToMany
    @JoinTable(
            name = "coverage_paragraph_matches",
            joinColumns = @JoinColumn(name = "coverage_result_id"),
            inverseJoinColumns = @JoinColumn(name = "paragraph_id")
    )
    @Builder.Default
    private List<Paragraph> matchedParagraphs = new ArrayList<>();

    @Column(name = "moe_excerpt", columnDefinition = "TEXT")
    private String moeExcerpt;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    // Evidence tracking
    @Column(name = "evidence_sections", length = 500)
    private String evidenceSections; // Comma-separated section numbers

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public boolean isMandatory() {
        return regulatoryLevel == RegulatoryLevel.HARD_LAW;
    }

    public boolean isCompliant() {
        return status == CoverageStatus.COVERED;
    }
}
