package com.unfinitas.backend.core.analysis.repository;

import com.unfinitas.backend.core.analysis.model.AnalysisResult;
import com.unfinitas.backend.core.analysis.model.GapFinding;
import com.unfinitas.backend.core.analysis.model.enums.GapSeverity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GapFindingRepository extends JpaRepository<GapFinding, Long> {

    // Find all gaps for an analysis
    List<GapFinding> findByAnalysisResultOrderBySeverityDesc(AnalysisResult analysisResult);

    // Find by severity
    List<GapFinding> findByAnalysisResultAndSeverity(
            AnalysisResult analysisResult,
            GapSeverity severity
    );

    // Find critical and major gaps
    @Query("SELECT g FROM GapFinding g " +
            "WHERE g.analysisResult = :analysis " +
            "AND g.severity IN ('CRITICAL', 'MAJOR') " +
            "ORDER BY g.severity DESC, g.clauseId")
    List<GapFinding> findCriticalAndMajorGaps(@Param("analysis") AnalysisResult analysis);

    // Count by severity
    long countByAnalysisResultAndSeverity(AnalysisResult analysisResult, GapSeverity severity);

    // Find gaps for specific clause
    Optional<GapFinding> findByAnalysisResultAndClauseId(
            AnalysisResult analysisResult,
            String clauseId
    );
}
