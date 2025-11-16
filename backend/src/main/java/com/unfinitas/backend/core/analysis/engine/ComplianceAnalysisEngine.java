package com.unfinitas.backend.core.analysis.engine;

import com.google.gson.Gson;
import com.unfinitas.backend.core.analysis.dto.ClauseMatchResult;
import com.unfinitas.backend.core.analysis.dto.DecisionSupportReport;
import com.unfinitas.backend.core.analysis.dto.GapAnalysisResult;
import com.unfinitas.backend.core.analysis.dto.ParagraphMatch;
import com.unfinitas.backend.core.analysis.model.*;
import com.unfinitas.backend.core.analysis.model.enums.AnalysisType;
import com.unfinitas.backend.core.analysis.model.enums.CoverageStatus;
import com.unfinitas.backend.core.analysis.model.enums.MatchType;
import com.unfinitas.backend.core.analysis.repository.AnalysisResultRepository;
import com.unfinitas.backend.core.analysis.repository.ComplianceOutcomeRepository;
import com.unfinitas.backend.core.ingestion.model.MoeDocument;
import com.unfinitas.backend.core.ingestion.model.Paragraph;
import com.unfinitas.backend.core.ingestion.repository.MoeDocumentRepository;
import com.unfinitas.backend.core.ingestion.repository.ParagraphRepository;
import com.unfinitas.backend.core.regulation.model.Regulation;
import com.unfinitas.backend.core.regulation.model.RegulationClause;
import com.unfinitas.backend.core.regulation.repository.RegulationRepository;
import com.unfinitas.backend.core.regulation.service.RegulationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComplianceAnalysisEngine {

    private final AnalysisResultRepository analysisRepo;
    private final ComplianceOutcomeRepository complianceOutcomeRepo;
    private final MoeDocumentRepository moeDocRepo;
    private final ParagraphRepository paragraphRepo;
    private final RegulationService regulationService;
    private final RegulationRepository regulationRepository;
    private final SemanticAnalyzer semanticAnalyzer;
    private final GapDetector gapDetector;
    private final QuestionGenerator questionGenerator;
    private final DecisionSupportGenerator decisionGenerator;
    private final Gson gson = new Gson();

    @Transactional
    public UUID analyzeCompliance(final UUID moeId, final UUID regulationId) {

        final MoeDocument moeDoc = moeDocRepo.findById(moeId)
                .orElseThrow(() -> new IllegalArgumentException("MOE not found: " + moeId));

        final Regulation regulation = regulationRepository.findById(regulationId)
                .orElseThrow(() -> new IllegalArgumentException("Regulation not found: " + regulationId));

        AnalysisResult analysis = AnalysisResult.builder()
                .moeDocument(moeDoc)
                .analysisType(AnalysisType.REGULATION_COMPLIANCE)
                .regulation(regulation)
                .createdAt(LocalDateTime.now())
                .build();

        analysis = analysisRepo.save(analysis);
        analysis.start();

        try {
            final List<Paragraph> moeParagraphs = paragraphRepo.findByMoeDocument(moeDoc);
            final List<RegulationClause> allClauses = regulationService.loadClauses(regulation.getId());
            final List<RegulationClause> clauses = filterPart145SectionA(allClauses);

            final var semanticResult = semanticAnalyzer.analyze(moeParagraphs, clauses);

            for (final ClauseMatchResult match : semanticResult.clauseMatches()) {
                final CoverageResult cov = createCoverageResult(analysis, match);
                analysis.addCoverageResult(cov);
            }

            saveComplianceOutcomes(semanticResult, analysis);

            final GapAnalysisResult gapResult = gapDetector.detectGaps(semanticResult, clauses);
            gapResult.gaps().forEach(analysis::addGapFinding);

            final List<AuditorQuestion> questions = questionGenerator.generate(
                    analysis.getCoverageResults(), gapResult);
            questions.forEach(analysis::addQuestion);

            final DecisionSupportReport decision =
                    decisionGenerator.generate(analysis.getCoverageResults(), gapResult);

            final int total = clauses.size();
            final int covered = (int) analysis.getCoverageResults().stream()
                    .filter(c -> c.getStatus() == CoverageStatus.COVERED).count();
            final int partial = (int) analysis.getCoverageResults().stream()
                    .filter(c -> c.getStatus() == CoverageStatus.PARTIAL).count();
            final int missing = total - covered - partial;

            final BigDecimal score = calculateComplianceScore(covered, partial, missing, total);

            analysis.complete(total, covered, partial, missing, score);
            analysis.setApprovalRecommendation(decision.recommendation());
            analysis.setExecutiveSummary(decision.executiveSummary());

            analysisRepo.save(analysis);
            return analysis.getId();

        } catch (final Exception e) {
            analysis.fail(e.getMessage());
            analysisRepo.save(analysis);
            throw new RuntimeException("Analysis failed", e);
        }
    }

    private void saveComplianceOutcomes(
            final SemanticAnalyzer.SemanticAnalysisResult semantic,
            final AnalysisResult analysis) {

        for (final ClauseMatchResult match : semantic.clauseMatches()) {
            if (match.complianceResult() == null) continue;

            final var cr = match.complianceResult();

            final ComplianceOutcome o = ComplianceOutcome.builder()
                    .analysis(analysis)
                    .requirementId(cr.requirement_id())
                    .complianceStatus(cr.compliance_status())
                    .findingLevel(cr.finding_level())
                    .justification(cr.justification())
                    .evidenceJson(gson.toJson(cr.evidence()))
                    .missingElementsJson(gson.toJson(cr.missing_elements()))
                    .recommendedActionsJson(gson.toJson(cr.recommended_actions()))
                    .build();

            complianceOutcomeRepo.save(o);
            analysis.getComplianceOutcomes().add(o);
        }
    }

    private List<RegulationClause> filterPart145SectionA(final List<RegulationClause> all) {
        return all.stream()
                .filter(this::isPart145)
                .toList();
    }

    private boolean isPart145(final RegulationClause c) {
        return c.getClauseId() != null && c.getClauseId().contains("145");
    }

    private CoverageResult createCoverageResult(
            final AnalysisResult analysis,
            final ClauseMatchResult matchResult) {

        final CoverageStatus status = classifyMatch(matchResult.bestSimilarity());

        final String moeEvidence = matchResult.matches().stream()
                .limit(3)
                .map(m -> {
                    final String section = m.paragraph().getSection() != null
                            ? m.paragraph().getSection().getSectionNumber()
                            : "N/A";
                    return String.format("Section %s (%.0f%%)", section, m.similarity() * 100);
                })
                .collect(Collectors.joining(", "));

        final List<Paragraph> paragraphs = matchResult.matches().stream()
                .limit(5)
                .map(ParagraphMatch::paragraph)
                .toList();

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
                .moeExcerpt(moeEvidence)
                .explanation(matchResult.evidence())
                .build();
    }

    private CoverageStatus classifyMatch(final double score) {
        if (score >= 0.75) return CoverageStatus.COVERED;
        if (score >= 0.40) return CoverageStatus.PARTIAL;
        return CoverageStatus.MISSING;
    }

    private BigDecimal calculateComplianceScore(final int covered, final int partial, final int missing, final int total) {
        if (total == 0) return BigDecimal.ZERO;
        final double score = ((covered * 1.0) + (partial * 0.5)) / total * 100;
        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }
}
