package com.unfinitas.backend.core.analysis.repository;

import com.unfinitas.backend.core.analysis.model.ComplianceOutcome;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComplianceOutcomeRepository extends JpaRepository<ComplianceOutcome, UUID> {

    List<ComplianceOutcome> findByAnalysisId(UUID analysisId);
}
