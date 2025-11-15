package com.unfinitas.backend.api.controller;

import com.unfinitas.backend.api.dto.AnalysisRequest;
import com.unfinitas.backend.api.dto.AnalysisResponse;
import com.unfinitas.backend.api.dto.CountsResponse;
import com.unfinitas.backend.api.dto.GapsResponse;
import com.unfinitas.backend.api.dto.SummaryResponse;
import com.unfinitas.backend.core.analysis.engine.ComplianceAnalysisEngine;
import com.unfinitas.backend.core.analysis.model.AnalysisResult;
import com.unfinitas.backend.core.analysis.model.AuditorQuestion;
import com.unfinitas.backend.core.analysis.model.CoverageResult;
import com.unfinitas.backend.core.analysis.model.GapFinding;
import com.unfinitas.backend.core.analysis.model.enums.GapSeverity;
import com.unfinitas.backend.core.analysis.repository.AnalysisResultRepository;
import com.unfinitas.backend.core.analysis.repository.AuditorQuestionRepository;
import com.unfinitas.backend.core.analysis.repository.CoverageResultRepository;
import com.unfinitas.backend.core.analysis.repository.GapFindingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AnalysisController {

    private final ComplianceAnalysisEngine analysisEngine;
    private final AnalysisResultRepository analysisRepo;
    private final CoverageResultRepository coverageRepo;
    private final GapFindingRepository gapRepo;
    private final AuditorQuestionRepository questionRepo;

    /**
     * POST /api/analysis
     * Start a new compliance analysis
     */
    @PostMapping
    public ResponseEntity<AnalysisResponse> startAnalysis(
            @RequestBody final AnalysisRequest request) {

        log.info("Starting analysis for MOE {} with regulation {}",
                request.moeId(), request.regulationVersion());

        try {
            final UUID analysisId = analysisEngine.analyzeCompliance(
                    request.moeId(),
                    request.regulationVersion()
            );

            return ResponseEntity.ok(new AnalysisResponse(
                    analysisId,
                    "Analysis completed successfully"
            ));

        } catch (final IllegalArgumentException e) {
            log.error("Invalid request", e);
            return ResponseEntity.badRequest().body(
                    new AnalysisResponse(null, "Invalid request: " + e.getMessage())
            );
        } catch (final Exception e) {
            log.error("Analysis failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new AnalysisResponse(null, "Analysis failed: " + e.getMessage())
            );
        }
    }

    /**
     * GET /api/analysis/{id}
     * Get analysis summary
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getAnalysis(@PathVariable final UUID id) {
        log.info("Fetching analysis {}", id);

        return analysisRepo.findById(id)
                .map(this::buildAnalysisSummary)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/analysis/{id}/status
     * Get analysis status
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<?> getAnalysisStatus(@PathVariable final UUID id) {
        return analysisRepo.findById(id)
                .map(a -> ResponseEntity.ok(Map.of(
                        "id", a.getId(),
                        "status", a.getStatus().toString(),
                        "completedAt", a.getCompletedAt()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/coverage?moeId={uuid}
     * Get coverage matrix for latest analysis
     */
    @GetMapping("/coverage")
    public ResponseEntity<?> getCoverage(@RequestParam final UUID moeId) {
        log.info("Fetching coverage for MOE {}", moeId);

        return analysisRepo.findTopByMoeDocument_IdOrderByCreatedAtDesc(moeId)
                .map(analysis -> {
                    final List<CoverageResult> results = coverageRepo.findByAnalysisResultOrderByClauseId(analysis);

                    return ResponseEntity.ok(Map.of(
                            "analysisId", analysis.getId(),
                            "clauses", results.stream()
                                    .map(this::buildCoverageDto)
                                    .collect(Collectors.toList())
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/questions?moeId={uuid}
     * Get auditor questions for latest analysis
     */
    @GetMapping("/questions")
    public ResponseEntity<?> getQuestions(@RequestParam final UUID moeId) {
        log.info("Fetching questions for MOE {}", moeId);

        return analysisRepo.findTopByMoeDocument_IdOrderByCreatedAtDesc(moeId)
                .map(analysis -> {
                    final List<AuditorQuestion> questions = questionRepo
                            .findByAnalysisResultOrderByPriorityDesc(analysis);

                    return ResponseEntity.ok(Map.of(
                            "analysisId", analysis.getId(),
                            "questions", questions.stream()
                                    .map(this::buildQuestionDto)
                                    .collect(Collectors.toList())
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/analysis/{id}/gaps
     * Get gap findings
     */
    @GetMapping("/{id}/gaps")
    public ResponseEntity<?> getGaps(@PathVariable final UUID id) {
        return analysisRepo.findById(id)
                .map(analysis -> {
                    final List<GapFinding> gaps = gapRepo
                            .findByAnalysisResultOrderBySeverityDesc(analysis);

                    return ResponseEntity.ok(Map.of(
                            "analysisId", id,
                            "totalGaps", gaps.size(),
                            "critical", gaps.stream()
                                    .filter(g -> g.getSeverity() == GapSeverity.CRITICAL)
                                    .count(),
                            "major", gaps.stream()
                                    .filter(g -> g.getSeverity() == GapSeverity.MAJOR)
                                    .count(),
                            "gaps", gaps.stream()
                                    .map(this::buildGapDto)
                                    .collect(Collectors.toList())
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/analysis/{id}/report
     * Get complete analysis report
     */
    @GetMapping("/{id}/report")
    public ResponseEntity<?> getReport(@PathVariable final UUID id) {
        return analysisRepo.findByIdComplete(id)
                .map(analysis -> ResponseEntity.ok(Map.of(
                        "analysis", buildAnalysisSummary(analysis),
                        "coverage", analysis.getCoverageResults().stream()
                                .map(this::buildCoverageDto)
                                .collect(Collectors.toList()),
                        "gaps", analysis.getGapFindings().stream()
                                .map(this::buildGapDto)
                                .collect(Collectors.toList()),
                        "questions", analysis.getQuestions().stream()
                                .map(this::buildQuestionDto)
                                .collect(Collectors.toList())
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/analysis/{id}/counts
     * Get analysis coverage counts
     */
    @GetMapping("/{id}/counts")
    public ResponseEntity<CountsResponse> getCounts(@PathVariable final UUID id) {
        log.info("Fetching counts for analysis {}", id);

        return analysisRepo.findById(id)
                .map(analysis -> new CountsResponse(
                        analysis.getId(),
                        analysis.getTotalItems(),
                        analysis.getCoveredCount(),
                        analysis.getMissingCount(),
                        analysis.getPartialCount()
                ))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/analysis/{id}/gaps-count
     * Get gap counts by severity
     */
    @GetMapping("/{id}/gaps-count")
    public ResponseEntity<GapsResponse> getGapsCount(@PathVariable final UUID id) {
        log.info("Fetching gap counts for analysis {}", id);

        return analysisRepo.findById(id)
                .map(analysis -> {
                    final int critical = (int) gapRepo.countByAnalysisResultAndSeverity(
                            analysis, GapSeverity.CRITICAL);
                    final int major = (int) gapRepo.countByAnalysisResultAndSeverity(
                            analysis, GapSeverity.MAJOR);
                    final int minor = (int) gapRepo.countByAnalysisResultAndSeverity(
                            analysis, GapSeverity.MINOR);

                    return new GapsResponse(analysis.getId(), critical, major, minor);
                })
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/analysis/{id}/summary
     * Get analysis summary with recommendation
     */
    @GetMapping("/{id}/summary")
    public ResponseEntity<SummaryResponse> getSummary(@PathVariable final UUID id) {
        log.info("Fetching summary for analysis {}", id);

        return analysisRepo.findById(id)
                .map(analysis -> new SummaryResponse(
                        analysis.getId(),
                        analysis.getApprovalRecommendation() != null ?
                                analysis.getApprovalRecommendation().toString() : null,
                        analysis.getExecutiveSummary()
                ))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private Map<String, Object> buildAnalysisSummary(final AnalysisResult analysis) {
        return Map.ofEntries(
                Map.entry("id", analysis.getId()),
                Map.entry("moeId", analysis.getMoeDocument().getId()),
                Map.entry("analysisType", analysis.getAnalysisType()),
                Map.entry("regulationVersion", analysis.getRegulationVersion()),
                Map.entry("status", analysis.getStatus()),
                Map.entry("totalItems", analysis.getTotalItems() != null ? analysis.getTotalItems() : 0),
                Map.entry("coveredCount", analysis.getCoveredCount()),
                Map.entry("partialCount", analysis.getPartialCount()),
                Map.entry("missingCount", analysis.getMissingCount()),
                Map.entry("complianceScore", analysis.getComplianceScore()),
                Map.entry("approvalRecommendation", analysis.getApprovalRecommendation()),
                Map.entry("executiveSummary", analysis.getExecutiveSummary()),
                Map.entry("createdAt", analysis.getCreatedAt()),
                Map.entry("completedAt", analysis.getCompletedAt())
        );
    }

    private Map<String, Object> buildCoverageDto(final CoverageResult coverage) {
        return Map.of(
                "clauseId", coverage.getClauseId(),
                "clauseTitle", coverage.getClauseTitle(),
                "status", coverage.getStatus(),
                "similarity", coverage.getSimilarity(),
                "matchType", coverage.getMatchType(),
                "moeExcerpt", coverage.getMoeExcerpt() != null ? coverage.getMoeExcerpt() : "",
                "explanation", coverage.getExplanation() != null ? coverage.getExplanation() : "",
                "evidenceSections", coverage.getEvidenceSections() != null ?
                        List.of(coverage.getEvidenceSections().split(",")) : List.of()
        );
    }

    private Map<String, Object> buildGapDto(final GapFinding gap) {
        return Map.of(
                "id", gap.getId(),
                "clauseId", gap.getClauseId(),
                "clauseTitle", gap.getClauseTitle(),
                "severity", gap.getSeverity(),
                "description", gap.getDescription(),
                "missingElements", gap.getMissingElements() != null ? gap.getMissingElements() : "",
                "suggestedActions", gap.getSuggestedActions() != null ? gap.getSuggestedActions() : "",
                "estimatedEffort", gap.getEstimatedEffort() != null ? gap.getEstimatedEffort() : ""
        );
    }

    private Map<String, Object> buildQuestionDto(final AuditorQuestion question) {
        return Map.of(
                "id", question.getId(),
                "clauseId", question.getClauseId(),
                "questionText", question.getQuestionText(),
                "priority", question.getPriority(),
                "context", question.getContext() != null ? question.getContext() : ""
        );
    }

    private int calculateProgress(final AnalysisResult analysis) {
        return switch (analysis.getStatus()) {
            case PENDING, FAILED -> 0;
            case IN_PROGRESS -> 50;
            case COMPLETED -> 100;
        };
    }
}
