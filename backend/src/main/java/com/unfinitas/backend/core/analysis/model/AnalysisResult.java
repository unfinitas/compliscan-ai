package com.unfinitas.backend.core.analysis.model;

import com.unfinitas.backend.core.analysis.model.enums.AnalysisStatus;
import com.unfinitas.backend.core.analysis.model.enums.AnalysisType;
import com.unfinitas.backend.core.analysis.model.enums.ApprovalRecommendation;
import com.unfinitas.backend.core.ingestion.model.MoeDocument;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "analysis_results", indexes = {
        @Index(name = "idx_analysis_moe_id", columnList = "moe_id"),
        @Index(name = "idx_analysis_base_moe_id", columnList = "base_moe_id"),
        @Index(name = "idx_analysis_status", columnList = "status"),
        @Index(name = "idx_analysis_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID id;

    // The MOE being analyzed
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moe_id", nullable = false)
    private MoeDocument moeDocument;

    // Optional: Base MOE for comparison
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_moe_id")
    private MoeDocument baseMoeDocument;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_type", nullable = false, length = 30)
    private AnalysisType analysisType;

    @Column(name = "regulation_version", length = 50)
    private String regulationVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnalysisStatus status;

    // Statistics
    @Column(name = "total_items")
    private Integer totalItems;

    @Column(name = "covered_count")
    @Builder.Default
    private Integer coveredCount = 0;

    @Column(name = "partial_count")
    @Builder.Default
    private Integer partialCount = 0;

    @Column(name = "missing_count")
    @Builder.Default
    private Integer missingCount = 0;

    // Overall compliance score (0-100)
    @Column(name = "compliance_score", precision = 5, scale = 2)
    private BigDecimal complianceScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_recommendation", length = 30)
    private ApprovalRecommendation approvalRecommendation;

    @Column(name = "executive_summary", columnDefinition = "TEXT")
    private String executiveSummary;

    @Column(name = "source_language", length = 10)
    private String sourceLanguage;

    @Column(name = "translation_required")
    private Boolean translationRequired;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Relationships
    @OneToMany(mappedBy = "analysisResult", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CoverageResult> coverageResults = new ArrayList<>();

    @OneToMany(mappedBy = "analysisResult", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AuditorQuestion> questions = new ArrayList<>();

    @OneToMany(mappedBy = "analysisResult", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GapFinding> gapFindings = new ArrayList<>();

    // Lifecycle hooks
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = AnalysisStatus.PENDING;
        }
    }

    // Domain methods
    public void start() {
        this.status = AnalysisStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
    }

    public void complete(final int total, final int covered, final int partial, final int missing, final BigDecimal score) {
        this.totalItems = total;
        this.coveredCount = covered;
        this.partialCount = partial;
        this.missingCount = missing;
        this.complianceScore = score;
        this.status = AnalysisStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void fail(final String errorMessage) {
        this.status = AnalysisStatus.FAILED;
        this.executiveSummary = "Analysis failed: " + errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    public void addCoverageResult(final CoverageResult result) {
        coverageResults.add(result);
        result.setAnalysisResult(this);
    }

    public void addQuestion(final AuditorQuestion question) {
        questions.add(question);
        question.setAnalysisResult(this);
    }

    public void addGapFinding(final GapFinding gap) {
        gapFindings.add(gap);
        gap.setAnalysisResult(this);
    }

    public double getCompliancePercentage() {
        return complianceScore != null ? complianceScore.doubleValue() : 0.0;
    }
}
