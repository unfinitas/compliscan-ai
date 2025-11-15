package com.unfinitas.backend.core.regulation.repository;

import com.unfinitas.backend.core.regulation.model.RegulationClause;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RegulationClauseRepository extends JpaRepository<RegulationClause, UUID> {

    List<RegulationClause> findByRegulationVersionOrderByClauseNumberAsc(String regulationVersion);

    List<RegulationClause> findByEmbeddingIsNull();

    List<RegulationClause> findByEmbeddingModelNot(String model);

    int countByEmbeddingIsNull();

    List<RegulationClause> findByRegulationId(UUID id);
}
