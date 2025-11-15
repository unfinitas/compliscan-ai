package com.unfinitas.backend.core.analysis.engine;

import com.unfinitas.backend.core.analysis.dto.ClauseMatchResult;
import com.unfinitas.backend.core.analysis.dto.DecisionSupportReport;
import com.unfinitas.backend.core.analysis.dto.GapAnalysisResult;
import com.unfinitas.backend.core.analysis.dto.ParagraphMatch;
import com.unfinitas.backend.core.analysis.model.AnalysisResult;
import com.unfinitas.backend.core.analysis.model.AuditorQuestion;
import com.unfinitas.backend.core.analysis.model.CoverageResult;
import com.unfinitas.backend.core.analysis.model.GapFinding;
import com.unfinitas.backend.core.analysis.model.enums.AnalysisType;
import com.unfinitas.backend.core.analysis.model.enums.CoverageStatus;
import com.unfinitas.backend.core.analysis.model.enums.MatchType;
import com.unfinitas.backend.core.analysis.repository.AnalysisResultRepository;
import com.unfinitas.backend.core.ingestion.model.MoeDocument;
import com.unfinitas.backend.core.ingestion.model.Paragraph;
import com.unfinitas.backend.core.ingestion.repository.MoeDocumentRepository;
import com.unfinitas.backend.core.ingestion.repository.ParagraphRepository;
import com.unfinitas.backend.core.regulation.model.RegulationClause;
import com.unfinitas.backend.core.regulation.service.RegulationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComplianceAnalysisEngine {

    private final AnalysisResultRepository analysisRepo;
    private final MoeDocumentRepository moeDocRepo;
    private final ParagraphRepository paragraphRepo;
    private final RegulationService regulationService;

    private final SemanticAnalyzer semanticAnalyzer;
    private final GapDetector gapDetector;
    private final QuestionGenerator questionGenerator;
    private final DecisionSupportGenerator decisionGenerator;

    /**
     * Run complete compliance analysis
     */
    @Transactional
    public UUID analyzeCompliance(final UUID moeId, final String regulationVersion) {
        log.info("Starting compliance analysis for MOE {} against regulation {}",
                moeId, regulationVersion);

        final long startTime = System.currentTimeMillis();

        // Load MOE document
        final MoeDocument moeDoc = moeDocRepo.findById(moeId)
                .orElseThrow(() -> new IllegalArgumentException("MOE not found: " + moeId));

        // Create analysis record
        AnalysisResult analysis = AnalysisResult.builder()
                .moeDocument(moeDoc)
                .analysisType(AnalysisType.REGULATION_COMPLIANCE)
                .regulationVersion(regulationVersion)
                .build();

        analysis = analysisRepo.save(analysis);
        analysis.start();

        try {
            // Load data
            final List<Paragraph> moeParagraphs = paragraphRepo.findByMoeDocument(moeDoc);
            final List<RegulationClause> clauses = regulationService.loadClauses(regulationVersion);

            log.info("Loaded {} paragraphs and {} regulation clauses",
                    moeParagraphs.size(), clauses.size());

            // Stage 1: Semantic Analysis
            log.info("Stage 1/4: Semantic matching");
            final SemanticAnalyzer.SemanticAnalysisResult semanticResult = semanticAnalyzer.analyze(
                    moeParagraphs,
                    clauses
            );

            // Save coverage results
            for (final ClauseMatchResult matchResult : semanticResult.clauseMatches()) {
                final CoverageResult coverage = createCoverageResult(analysis, matchResult);
                analysis.addCoverageResult(coverage);
            }

            // Stage 2: Gap Detection
            log.info("Stage 2/4: Gap detection");
            final GapAnalysisResult gapResult = gapDetector.detectGaps(
                    semanticResult,
                    clauses
            );

            // Save gap findings
            for (final GapFinding gap : gapResult.gaps()) {
                analysis.addGapFinding(gap);
            }

            // Stage 3: Question Generation
            log.info("Stage 3/4: Question generation");
            final List<AuditorQuestion> questions = questionGenerator.generate(
                    analysis.getCoverageResults(),
                    gapResult
            );

            questions.forEach(analysis::addQuestion);

            // Stage 4: Decision Support
            log.info("Stage 4/4: Decision support generation");
            final DecisionSupportReport decision = decisionGenerator.generate(
                    analysis.getCoverageResults(),
                    gapResult
            );

            // Calculate statistics
            final int total = clauses.size();
            final int covered = (int) analysis.getCoverageResults().stream()
                    .filter(c -> c.getStatus() == CoverageStatus.COVERED)
                    .count();
            final int partial = (int) analysis.getCoverageResults().stream()
                    .filter(c -> c.getStatus() == CoverageStatus.PARTIAL)
                    .count();
            final int missing = total - covered - partial;

            final BigDecimal score = calculateComplianceScore(covered, partial, missing, total);

            // Update analysis
            analysis.complete(total, covered, partial, missing, score);
            analysis.setApprovalRecommendation(decision.recommendation());
            analysis.setExecutiveSummary(decision.executiveSummary());

            analysisRepo.save(analysis);

            final long duration = System.currentTimeMillis() - startTime;
            log.info("Analysis completed in {}ms - Score: {}%, Recommendation: {}",
                    duration, score, decision.recommendation());

            return analysis.getId();

        } catch (final Exception e) {
            log.error("Analysis failed for MOE {}", moeId, e);
            analysis.fail(e.getMessage());
            analysisRepo.save(analysis);
            throw new RuntimeException("Analysis failed", e);
        }
    }

    private CoverageResult createCoverageResult(
            final AnalysisResult analysis,
            final ClauseMatchResult matchResult) {

        // Determine status
        final CoverageStatus status = classifyMatch(matchResult.bestSimilarity());

        // Extract evidence
        final String evidence = matchResult.matches().stream()
                .limit(3)
                .map(m -> String.format("Section %s (%.0f%%)",
                        m.paragraph().getSection().getSectionNumber(),
                        m.similarity() * 100))
                .collect(Collectors.joining(", "));

        // Get matched paragraphs
        final List<Paragraph> paragraphs = matchResult.matches().stream()
                .limit(5)
                .map(ParagraphMatch::paragraph)
                .collect(Collectors.toList());

        // Determine match type
        final MatchType matchType = paragraphs.size() > 1
                ? MatchType.AGGREGATE
                : MatchType.SINGLE;

        return CoverageResult.builder()
                .analysisResult(analysis)
                .clauseId(matchResult.clauseId())
                .clauseTitle(matchResult.clauseTitle())
                .status(status)
                .similarity(BigDecimal.valueOf(matchResult.bestSimilarity()))
                .matchType(matchType)
                .matchedParagraphs(paragraphs)
                .moeExcerpt(evidence)
                .explanation(generateExplanation(matchResult, status))
                .build();
    }

    private CoverageStatus classifyMatch(final double similarity) {
        if (similarity >= 0.75) return CoverageStatus.COVERED;
        if (similarity >= 0.40) return CoverageStatus.PARTIAL;
        return CoverageStatus.MISSING;
    }

    private String generateExplanation(final ClauseMatchResult match, final CoverageStatus status) {
        return switch (status) {
            case COVERED -> "Requirement adequately addressed in MOE";
            case PARTIAL -> String.format(
                    "Partially addressed (%.0f%% match) - review for completeness",
                    match.bestSimilarity() * 100
            );
            case MISSING -> "Requirement not found in MOE - must be documented";
        };
    }

    private BigDecimal calculateComplianceScore(final int covered, final int partial, final int missing, final int total) {
        if (total == 0) return BigDecimal.ZERO;

        // Covered = 100%, Partial = 50%, Missing = 0%
        final double score = ((covered * 1.0) + (partial * 0.5)) / total * 100;
        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }
}
