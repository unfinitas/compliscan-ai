package com.unfinitas.backend.core.regulation.repository;

import com.unfinitas.backend.core.regulation.model.Regulation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegulationRepository extends JpaRepository<Regulation, UUID> {

    // Find by code and version
    Optional<Regulation> findByCodeAndVersion(String code, String version);

    // Find all active regulations
    List<Regulation> findByActiveTrue();

    // Find by code (all versions)
    List<Regulation> findByCodeOrderByEffectiveDateDesc(String code);

    // Find latest version by code
    @Query("SELECT r FROM Regulation r " +
            "WHERE r.code = :code AND r.active = true " +
            "ORDER BY r.effectiveDate DESC")
    Optional<Regulation> findLatestByCode(@Param("code") String code);

    // Find with clauses eagerly loaded
    @Query("SELECT r FROM Regulation r " +
            "LEFT JOIN FETCH r.clauses " +
            "WHERE r.id = :id")
    Optional<Regulation> findByIdWithClauses(@Param("id") UUID id);

    // Find by code and version with clauses
    @Query("SELECT r FROM Regulation r " +
            "LEFT JOIN FETCH r.clauses " +
            "WHERE r.code = :code AND r.version = :version")
    Optional<Regulation> findByCodeAndVersionWithClauses(
            @Param("code") String code,
            @Param("version") String version
    );

    // Check if regulation exists
    boolean existsByCodeAndVersion(String code, String version);

    // Count clauses by regulation
    @Query("SELECT COUNT(c) FROM Regulation r " +
            "JOIN r.clauses c " +
            "WHERE r.id = :regulationId")
    long countClausesByRegulation(@Param("regulationId") UUID regulationId);
}
