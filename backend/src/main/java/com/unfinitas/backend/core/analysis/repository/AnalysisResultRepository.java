package com.unfinitas.backend.core.analysis.repository;

import com.unfinitas.backend.core.analysis.model.AnalysisResult;
import com.unfinitas.backend.core.analysis.model.enums.AnalysisStatus;
import com.unfinitas.backend.core.analysis.model.enums.AnalysisType;
import com.unfinitas.backend.core.ingestion.model.MoeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, UUID> {

    @Query("""
                select a
                from AnalysisResult a
                join fetch a.regulation
                join fetch a.moeDocument
                where a.id = :id
            """)
    Optional<AnalysisResult> findWithAll(@Param("id") UUID id);


    // Find all analyses for a MOE
    List<AnalysisResult> findByMoeDocumentOrderByCreatedAtDesc(MoeDocument moeDocument);

    // Find latest analysis for a MOE
    Optional<AnalysisResult> findTopByMoeDocumentOrderByCreatedAtDesc(MoeDocument moeDocument);

    // Find by status
    List<AnalysisResult> findByStatus(AnalysisStatus status);

    // Find by type
    List<AnalysisResult> findByAnalysisType(AnalysisType type);

    // Find with coverage results (eager fetch)
    @Query("SELECT a FROM AnalysisResult a " +
            "LEFT JOIN FETCH a.coverageResults " +
            "WHERE a.id = :id")
    Optional<AnalysisResult> findByIdWithCoverage(@Param("id") UUID id);

    // Find with questions (eager fetch)
    @Query("SELECT a FROM AnalysisResult a " +
            "LEFT JOIN FETCH a.questions " +
            "WHERE a.id = :id")
    Optional<AnalysisResult> findByIdWithQuestions(@Param("id") UUID id);

    // Find with gap findings (eager fetch)
    @Query("SELECT a FROM AnalysisResult a " +
            "LEFT JOIN FETCH a.gapFindings " +
            "WHERE a.id = :id")
    Optional<AnalysisResult> findByIdWithGaps(@Param("id") UUID id);

    // Find complete analysis (all relationships)
    @Query("SELECT DISTINCT a FROM AnalysisResult a " +
            "LEFT JOIN FETCH a.coverageResults " +
            "LEFT JOIN FETCH a.questions " +
            "LEFT JOIN FETCH a.gapFindings " +
            "WHERE a.id = :id")
    Optional<AnalysisResult> findByIdComplete(@Param("id") UUID id);

    // Find by date range
    List<AnalysisResult> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // Statistics queries
    @Query("SELECT COUNT(a) FROM AnalysisResult a " +
            "WHERE a.status = :status " +
            "AND a.createdAt >= :since")
    long countByStatusSince(@Param("status") AnalysisStatus status, @Param("since") LocalDateTime since);

    @Query("SELECT AVG(a.complianceScore) FROM AnalysisResult a " +
            "WHERE a.status = 'COMPLETED' " +
            "AND a.createdAt >= :since")
    Double getAverageComplianceScoreSince(@Param("since") LocalDateTime since);

    // Find latest analysis for a MOE by ID
    Optional<AnalysisResult> findTopByMoeDocument_IdOrderByCreatedAtDesc(UUID moeId);
}
