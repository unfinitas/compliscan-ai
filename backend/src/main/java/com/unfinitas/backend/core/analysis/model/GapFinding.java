package com.unfinitas.backend.core.analysis.model;

import com.unfinitas.backend.core.analysis.model.enums.GapSeverity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "gap_findings", indexes = {
        @Index(name = "idx_gap_analysis_id", columnList = "analysis_id"),
        @Index(name = "idx_gap_severity", columnList = "severity")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GapFinding {

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
    @Column(nullable = false, length = 20)
    private GapSeverity severity;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "missing_elements", columnDefinition = "TEXT")
    private String missingElements;

    @Column(name = "suggested_actions", columnDefinition = "TEXT")
    private String suggestedActions;

    @Column(name = "estimated_effort", length = 50)
    private String estimatedEffort; // e.g., "2-3 days", "1 week"

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
