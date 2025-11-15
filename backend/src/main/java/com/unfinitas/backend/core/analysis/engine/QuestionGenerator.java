package com.unfinitas.backend.core.analysis.engine;

import com.unfinitas.backend.core.analysis.dto.GapAnalysisResult;
import com.unfinitas.backend.core.analysis.model.AuditorQuestion;
import com.unfinitas.backend.core.analysis.model.CoverageResult;
import com.unfinitas.backend.core.analysis.model.GapFinding;
import com.unfinitas.backend.core.analysis.model.enums.CoverageStatus;
import com.unfinitas.backend.core.analysis.model.enums.QuestionPriority;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class QuestionGenerator {

    private static final Map<CoverageStatus, List<String>> QUESTION_TEMPLATES = Map.of(
            CoverageStatus.MISSING, List.of(
                    "How does the organization address the requirement for %s (%s)?",
                    "Where in the MOE is %s (%s) documented?",
                    "What procedures are in place to ensure compliance with %s (%s)?"
            ),
            CoverageStatus.PARTIAL, List.of(
                    "Can you provide additional evidence demonstrating full compliance with %s (%s)?",
                    "Please clarify how the organization fully implements %s (%s).",
                    "The MOE partially addresses %s (%s) - what additional documentation exists?"
            )
    );

    public List<AuditorQuestion> generate(
            final List<CoverageResult> coverageResults,
            final GapAnalysisResult gapResult) {

        final List<AuditorQuestion> questions = new ArrayList<>();
        final Random random = new Random(42); // Deterministic

        for (final CoverageResult result : coverageResults) {
            if (result.getStatus() == CoverageStatus.COVERED) {
                continue; // No questions for covered items
            }

            final List<String> templates = QUESTION_TEMPLATES.get(result.getStatus());
            if (templates == null) continue;

            final String template = templates.get(random.nextInt(templates.size()));
            final String questionText = String.format(
                    template,
                    result.getClauseTitle(),
                    result.getClauseId()
            );

            final QuestionPriority priority = determinePriority(result, gapResult);

            questions.add(AuditorQuestion.builder()
                    .clauseId(result.getClauseId())
                    .questionText(questionText)
                    .priority(priority)
                    .context(result.getExplanation())
                    .build());
        }

        log.info("Generated {} auditor questions", questions.size());
        return questions;
    }

    private QuestionPriority determinePriority(
            final CoverageResult coverage,
            final GapAnalysisResult gapResult) {

        // Find related gap
        final Optional<GapFinding> relatedGap = gapResult.gaps().stream()
                .filter(g -> g.getClauseId().equals(coverage.getClauseId()))
                .findFirst();

        return relatedGap.map(gapFinding -> switch (gapFinding.getSeverity()) {
            case CRITICAL -> QuestionPriority.CRITICAL;
            case MAJOR -> QuestionPriority.HIGH;
            case MINOR -> QuestionPriority.MEDIUM;
            case INFORMATIONAL -> QuestionPriority.LOW;
        }).orElseGet(() -> coverage.getStatus() == CoverageStatus.MISSING
                ? QuestionPriority.HIGH
                : QuestionPriority.MEDIUM);

    }
}
