package com.unfinitas.backend.core.analysis.repository;

import com.unfinitas.backend.core.analysis.model.ComplianceOutcome;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComplianceOutcomeRepository extends JpaRepository<ComplianceOutcome, UUID> {

    List<ComplianceOutcome> findByAnalysisId(UUID analysisId);

    Page<ComplianceOutcome> findByAnalysisId(UUID analysisId, Pageable pageable);

    Page<ComplianceOutcome> findByAnalysisIdAndComplianceStatus(
            UUID analysisId, String complianceStatus, Pageable pageable);

    Page<ComplianceOutcome> findByAnalysisIdAndFindingLevel(
            UUID analysisId, String findingLevel, Pageable pageable);

    Page<ComplianceOutcome> findByAnalysisIdAndComplianceStatusAndFindingLevel(
            UUID analysisId, String complianceStatus, String findingLevel, Pageable pageable);
}
