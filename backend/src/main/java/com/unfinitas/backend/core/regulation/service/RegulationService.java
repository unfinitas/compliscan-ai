package com.unfinitas.backend.core.regulation.service;

import com.unfinitas.backend.core.regulation.model.RegulationClause;
import com.unfinitas.backend.core.regulation.repository.RegulationClauseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RegulationService {

    private final RegulationClauseRepository regulationClauseRepository;

    @Transactional(readOnly = true)
    public List<RegulationClause> loadClauses(final String regulationVersion) {
        log.info("Loading regulation clauses for version: {}", regulationVersion);

        final List<RegulationClause> clauses = regulationClauseRepository
                .findByRegulationVersionOrderByClauseNumberAsc(regulationVersion);

        log.info("Loaded {} clauses for regulation version {}", clauses.size(), regulationVersion);

        return clauses;
    }
}
