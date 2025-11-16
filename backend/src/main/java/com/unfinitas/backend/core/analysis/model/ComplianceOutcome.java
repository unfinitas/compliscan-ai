package com.unfinitas.backend.core.analysis.model;

import com.unfinitas.backend.core.analysis.model.AnalysisResult;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "compliance_outcomes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceOutcome {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private AnalysisResult analysis;

    @Column(nullable = false)
    private String requirementId;

    @Column(nullable = false)
    private String complianceStatus;

    @Column(nullable = false)
    private String findingLevel;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String justification;

    @Column(columnDefinition = "TEXT")
    private String evidenceJson;

    @Column(columnDefinition = "TEXT")
    private String missingElementsJson;

    @Column(columnDefinition = "TEXT")
    private String recommendedActionsJson;
}
