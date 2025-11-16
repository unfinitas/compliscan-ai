package com.unfinitas.backend.api.controller;

import com.unfinitas.backend.api.dto.AnalysisResponse;
import com.unfinitas.backend.core.analysis.engine.ComplianceAnalysisEngine;
import com.unfinitas.backend.core.analysis.model.AnalysisResult;
import com.unfinitas.backend.core.analysis.model.ComplianceOutcome;
import com.unfinitas.backend.core.analysis.repository.AnalysisResultRepository;
import com.unfinitas.backend.core.analysis.repository.ComplianceOutcomeRepository;
import com.unfinitas.backend.core.regulation.repository.RegulationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@Slf4j
public class AnalysisController {

    private final ComplianceAnalysisEngine analysisEngine;
    private final AnalysisResultRepository analysisRepo;
    private final ComplianceOutcomeRepository complianceOutcomeRepo;
    private final RegulationRepository regulationRepo;

    @PostMapping
    public ResponseEntity<AnalysisResponse> startAnalysis(
            @RequestParam final UUID moeId
    ) {
        final long count = regulationRepo.count();

        if (count == 0) {
            return ResponseEntity.status(409)
                    .body(new AnalysisResponse(null,
                            "No regulation uploaded. Please upload regulation first."));
        }

        if (count > 1) {
            return ResponseEntity.status(409)
                    .body(new AnalysisResponse(null,
                            "Multiple regulations exist. System expects exactly ONE regulation."));
        }

        final UUID regulationId = regulationRepo.findAll().getFirst().getId();

        log.info("Auto-detected regulation {} for analysis", regulationId);

        final UUID analysisId = analysisEngine.analyzeCompliance(moeId, regulationId);

        return ResponseEntity.ok(new AnalysisResponse(analysisId, "Analysis started"));
    }

    @GetMapping
    public ResponseEntity<Page<Map<String, Object>>> listAnalyses(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) final Pageable pageable,
            @RequestParam(required = false) final UUID moeId
    ) {
        return ResponseEntity.ok(
                analysisRepo.findAllWithFetch(moeId, null, pageable)
                        .map(this::toDto)
        );
    }

    @Transactional(readOnly = true)
    @GetMapping("/{id}")
    public ResponseEntity<?> getAnalysisReport(@PathVariable final UUID id) {
        return analysisRepo.findWithAll(id)
                .map(this::buildReport)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Transactional(readOnly = true)
    @GetMapping("/{id}/outcomes")
    public ResponseEntity<Page<Map<String, Object>>> getAnalysisOutcomes(
            @PathVariable final UUID id,
            @PageableDefault(size = 20, sort = "requirementId") final Pageable pageable,
            @RequestParam(required = false) final String complianceStatus,
            @RequestParam(required = false) final String findingLevel
    ) {
        final Page<ComplianceOutcome> outcomes;

        if (complianceStatus != null && findingLevel != null) {
            outcomes = complianceOutcomeRepo.findByAnalysisIdAndComplianceStatusAndFindingLevel(
                    id, complianceStatus, findingLevel, pageable);
        } else if (complianceStatus != null) {
            outcomes = complianceOutcomeRepo.findByAnalysisIdAndComplianceStatus(
                    id, complianceStatus, pageable);
        } else if (findingLevel != null) {
            outcomes = complianceOutcomeRepo.findByAnalysisIdAndFindingLevel(
                    id, findingLevel, pageable);
        } else {
            outcomes = complianceOutcomeRepo.findByAnalysisId(id, pageable);
        }

        return ResponseEntity.ok(outcomes.map(this::buildComplianceDto));
    }

    private Map<String, Object> toDto(final AnalysisResult a) {
        return Map.of(
                "id", a.getId(),
                "moeId", a.getMoeDocument().getId(),
                "regulationId", a.getRegulation().getId(),
                "status", a.getStatus(),
                "analysisType", a.getAnalysisType(),
                "complianceScore", a.getComplianceScore() != null ? a.getComplianceScore() : 0.0,
                "createdAt", a.getCreatedAt()
        );
    }

    private Map<String, Object> buildReport(final AnalysisResult analysis) {
        final List<ComplianceOutcome> outcomes =
                complianceOutcomeRepo.findByAnalysisId(analysis.getId());
        return Map.of(
                "analysisId", analysis.getId(),
                "moeId", analysis.getMoeDocument().getId(),
                "regulationVersion", analysis.getRegulation().getVersion(),
                "totalRequirements", outcomes.size(),
                "compliance", outcomes.stream()
                        .map(this::buildComplianceDto)
                        .toList()
        );
    }

    private Map<String, Object> buildComplianceDto(final ComplianceOutcome o) {
        final Map<String, Object> m = new HashMap<>();
        m.put("requirement_id", o.getRequirementId());
        m.put("compliance_status", o.getComplianceStatus());
        m.put("finding_level", o.getFindingLevel());
        m.put("justification", o.getJustification());
        m.put("evidence", safeList(o.getEvidenceJson()));
        m.put("missing_elements", safeList(o.getMissingElementsJson()));
        m.put("recommended_actions", safeList(o.getRecommendedActionsJson()));
        return m;
    }

    @SuppressWarnings("unchecked")
    private List<String> safeList(final String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return new com.google.gson.Gson().fromJson(json, List.class);
        } catch (final Exception e) {
            return List.of();
        }
    }
}
