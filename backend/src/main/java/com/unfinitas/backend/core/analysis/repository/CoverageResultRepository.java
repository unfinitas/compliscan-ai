package com.unfinitas.backend.core.analysis.repository;

import com.unfinitas.backend.core.analysis.model.AnalysisResult;
import com.unfinitas.backend.core.analysis.model.CoverageResult;
import com.unfinitas.backend.core.analysis.model.enums.CoverageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CoverageResultRepository extends JpaRepository<CoverageResult, Long> {

    // Find all coverage results for an analysis
    List<CoverageResult> findByAnalysisResultOrderByClauseId(AnalysisResult analysisResult);

    // Find by status
    List<CoverageResult> findByAnalysisResultAndStatus(
            AnalysisResult analysisResult,
            CoverageStatus status
    );

    // Find mandatory requirements
    @Query("SELECT c FROM CoverageResult c " +
            "WHERE c.analysisResult = :analysis " +
            "AND c.regulatoryLevel = 'HARD_LAW'")
    List<CoverageResult> findMandatoryRequirements(@Param("analysis") AnalysisResult analysis);

    // Find non-compliant mandatory requirements
    @Query("SELECT c FROM CoverageResult c " +
            "WHERE c.analysisResult = :analysis " +
            "AND c.regulatoryLevel = 'HARD_LAW' " +
            "AND c.status IN ('PARTIAL', 'MISSING')")
    List<CoverageResult> findNonCompliantMandatory(@Param("analysis") AnalysisResult analysis);

    // Find by clause ID
    Optional<CoverageResult> findByAnalysisResultAndClauseId(
            AnalysisResult analysisResult,
            String clauseId
    );

    // Count by status
    long countByAnalysisResultAndStatus(AnalysisResult analysisResult, CoverageStatus status);

    // Find with matched paragraphs (eager fetch)
    @Query("SELECT DISTINCT c FROM CoverageResult c " +
            "LEFT JOIN FETCH c.matchedParagraphs " +
            "WHERE c.analysisResult = :analysis")
    List<CoverageResult> findByAnalysisResultWithParagraphs(@Param("analysis") AnalysisResult analysis);

    // Statistics
    @Query("SELECT AVG(c.similarity) FROM CoverageResult c " +
            "WHERE c.analysisResult = :analysis " +
            "AND c.status = 'COVERED'")
    Double getAverageSimilarityForCovered(@Param("analysis") AnalysisResult analysis);
}
