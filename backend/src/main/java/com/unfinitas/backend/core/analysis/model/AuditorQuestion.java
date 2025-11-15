package com.unfinitas.backend.core.analysis.model;

import com.unfinitas.backend.core.analysis.model.enums.QuestionPriority;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "auditor_questions", indexes = {
        @Index(name = "idx_questions_analysis_id", columnList = "analysis_id"),
        @Index(name = "idx_questions_priority", columnList = "priority")
})
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AuditorQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private AnalysisResult analysisResult;

    @Column(name = "clause_id", nullable = false, length = 50)
    private String clauseId;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private QuestionPriority priority = QuestionPriority.MEDIUM;

    @Column(columnDefinition = "TEXT")
    private String context;

    @Column(name = "related_finding_id")
    private Long relatedFindingId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
