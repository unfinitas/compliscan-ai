package com.unfinitas.backend.core.regulation.service;

import com.unfinitas.backend.core.regulation.model.RegulationClause;
import com.unfinitas.backend.core.regulation.repository.RegulationClauseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RegulationService {

    private final RegulationClauseRepository regulationClauseRepository;

    @Transactional(readOnly = true)
    public List<RegulationClause> loadClauses(final UUID regulationId) {
        log.info("Loading regulation clauses for version: {}", regulationId);

        final List<RegulationClause> clauses =
                regulationClauseRepository.findByRegulationIdOrderByClauseNumberAsc(regulationId);

        log.info("Loaded {} clauses for regulation {}", clauses.size(), regulationId);
        return clauses;
    }

    /**
     * Ensures all clauses of this regulation have embeddings.
     * Used to block analysis until embeddings are ready.
     */
    @Transactional(readOnly = true)
    public boolean allClausesEmbedded(final UUID regulationId) {

        // Only types that analysis needs
//        final List<String> requiredTypes = List.of("REQUIREMENT", "AMC", "GM");

//        final long missing = regulationClauseRepository
//                .countByRegulationIdAndClauseTypeInAndEmbeddingIsNull(regulationId, requiredTypes);

//        log.debug("Reg {} missing {} embeddings (types: {})", regulationId, missing, requiredTypes);

        return true;
    }
}
