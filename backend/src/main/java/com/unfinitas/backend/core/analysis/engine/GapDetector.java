package com.unfinitas.backend.core.analysis.engine;

import com.unfinitas.backend.core.analysis.dto.ClauseMatchResult;
import com.unfinitas.backend.core.analysis.dto.GapAnalysisResult;
import com.unfinitas.backend.core.analysis.model.GapFinding;
import com.unfinitas.backend.core.analysis.model.enums.GapSeverity;
import com.unfinitas.backend.core.regulation.model.RegulationClause;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GapDetector {

    public GapAnalysisResult detectGaps(
            final SemanticAnalyzer.SemanticAnalysisResult semanticResult,
            final List<RegulationClause> clauses) {

        final List<GapFinding> gaps = new ArrayList<>();
        final Map<String, RegulationClause> clauseMap = clauses.stream()
                .collect(Collectors.toMap(RegulationClause::getClauseId, c -> c));

        for (final ClauseMatchResult match : semanticResult.clauseMatches()) {
            final RegulationClause clause = clauseMap.get(match.clauseId());

            if (clause == null) continue;

            final GapSeverity severity = determineSeverity(match, clause);

            if (severity != null) {
                final GapFinding gap = GapFinding.builder()
                        .clauseId(match.clauseId())
                        .clauseTitle(match.clauseTitle())
                        .severity(severity)
                        .description(generateDescription(match, clause))
                        .missingElements(identifyMissingElements(match, clause))
                        .suggestedActions(generateActions(match, clause))
                        .estimatedEffort(estimateEffort(severity))
                        .build();

                gaps.add(gap);
            }
        }

        log.info("Detected {} gaps", gaps.size());
        return new GapAnalysisResult(gaps);
    }

    private GapSeverity determineSeverity(final ClauseMatchResult match, final RegulationClause clause) {
        final boolean isMandatory = clause.isRequirement();
        final double similarity = match.bestSimilarity();

        if (similarity >= 0.75) {
            return null; // No gap
        }

        if (similarity < 0.30) {
            return isMandatory ? GapSeverity.CRITICAL : GapSeverity.MINOR;
        }

        if (similarity < 0.60) {
            return isMandatory ? GapSeverity.MAJOR : GapSeverity.MINOR;
        }

        return GapSeverity.INFORMATIONAL;
    }

    private String generateDescription(final ClauseMatchResult match, final RegulationClause clause) {
        final double similarity = match.bestSimilarity();

        if (match.matches().isEmpty() || similarity < 0.30) {
            return String.format(
                    "Required clause %s not found in MOE",
                    clause.getClauseId()
            );
        }

        if (similarity < 0.60) {
            return String.format(
                    "Insufficient coverage of %s (%.0f%% match)",
                    clause.getClauseId(),
                    similarity * 100
            );
        }

        return String.format(
                "Partial coverage of %s",
                clause.getClauseId()
        );
    }

    private String identifyMissingElements(final ClauseMatchResult match, final RegulationClause clause) {
        if (match.matches().isEmpty()) {
            return "Complete requirement missing";
        }
        return "Specific procedures and responsibilities need clarification";
    }

    private String generateActions(final ClauseMatchResult match, final RegulationClause clause) {
        final List<String> actions = new ArrayList<>();

        if (match.matches().isEmpty()) {
            actions.add("Add new section addressing " + clause.getTitle());
            actions.add("Reference regulation " + clause.getClauseId());
        } else {
            actions.add("Expand existing content in sections: " +
                    match.matches().stream()
                            .map(m -> m.paragraph().getSection().getSectionNumber())
                            .distinct()
                            .limit(3)
                            .collect(Collectors.joining(", ")));
            actions.add("Add explicit reference to " + clause.getClauseId());
        }

        return String.join("\n", actions);
    }

    private String estimateEffort(final GapSeverity severity) {
        return switch (severity) {
            case CRITICAL -> "1-2 weeks";
            case MAJOR -> "3-5 days";
            case MINOR -> "1-2 days";
            case INFORMATIONAL -> "Few hours";
        };
    }
}
