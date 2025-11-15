package com.unfinitas.backend.core.analysis.engine;

import com.unfinitas.backend.core.analysis.dto.DecisionSupportReport;
import com.unfinitas.backend.core.analysis.dto.GapAnalysisResult;
import com.unfinitas.backend.core.analysis.model.CoverageResult;
import com.unfinitas.backend.core.analysis.model.enums.ApprovalRecommendation;
import com.unfinitas.backend.core.analysis.model.enums.CoverageStatus;
import com.unfinitas.backend.core.analysis.model.enums.GapSeverity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class DecisionSupportGenerator {

    public DecisionSupportReport generate(
            final List<CoverageResult> coverageResults,
            final GapAnalysisResult gapResult) {

        // Count gaps by severity
        final long criticalGaps = gapResult.gaps().stream()
                .filter(g -> g.getSeverity() == GapSeverity.CRITICAL)
                .count();

        final long majorGaps = gapResult.gaps().stream()
                .filter(g -> g.getSeverity() == GapSeverity.MAJOR)
                .count();

        // Determine recommendation
        final ApprovalRecommendation recommendation = determineRecommendation(
                criticalGaps,
                majorGaps,
                coverageResults
        );

        // Generate summary
        final String summary = buildExecutiveSummary(
                coverageResults,
                gapResult,
                criticalGaps,
                majorGaps,
                recommendation
        );

        return new DecisionSupportReport(recommendation, summary);
    }

    private ApprovalRecommendation determineRecommendation(
            final long criticalGaps,
            final long majorGaps,
            final List<CoverageResult> coverageResults) {

        if (criticalGaps > 3) {
            return ApprovalRecommendation.REJECT;
        }

        if (criticalGaps > 0 || majorGaps > 5) {
            return ApprovalRecommendation.MAJOR_REVISIONS_REQUIRED;
        }

        if (majorGaps > 0) {
            return ApprovalRecommendation.MINOR_REVISIONS_REQUIRED;
        }

        final long covered = coverageResults.stream()
                .filter(c -> c.getStatus() == CoverageStatus.COVERED)
                .count();

        final double coverageRate = (double) covered / coverageResults.size();

        if (coverageRate >= 0.95) {
            return ApprovalRecommendation.APPROVE;
        }

        return ApprovalRecommendation.CONDITIONAL_APPROVAL;
    }

    private String buildExecutiveSummary(
            final List<CoverageResult> results,
            final GapAnalysisResult gaps,
            final long critical,
            final long major,
            final ApprovalRecommendation recommendation) {

        final long covered = results.stream()
                .filter(r -> r.getStatus() == CoverageStatus.COVERED)
                .count();

        final long partial = results.stream()
                .filter(r -> r.getStatus() == CoverageStatus.PARTIAL)
                .count();

        final long missing = results.size() - covered - partial;

        return String.format("""
                        COMPLIANCE ANALYSIS SUMMARY
                        
                        Overall Assessment: %s
                        
                        Coverage Statistics:
                        - Total Requirements: %d
                        - Fully Covered: %d (%.0f%%)
                        - Partially Covered: %d (%.0f%%)
                        - Missing: %d (%.0f%%)
                        
                        Critical Findings:
                        - Critical Gaps: %d
                        - Major Gaps: %d
                        
                        Recommendation: %s
                        
                        Next Steps: %s
                        """,
                recommendation,
                results.size(),
                covered, (double) covered / results.size() * 100,
                partial, (double) partial / results.size() * 100,
                missing, (double) missing / results.size() * 100,
                critical,
                major,
                recommendation,
                getNextSteps(recommendation, critical, major)
        );
    }

    private String getNextSteps(final ApprovalRecommendation rec, final long critical, final long major) {
        return switch (rec) {
            case APPROVE -> "MOE approved. Proceed with certification.";
            case CONDITIONAL_APPROVAL -> "Address minor findings before final approval.";
            case MINOR_REVISIONS_REQUIRED -> String.format(
                    "Address %d findings and resubmit.", major
            );
            case MAJOR_REVISIONS_REQUIRED -> String.format(
                    "Comprehensive revision required. %d critical and %d major gaps identified.",
                    critical, major
            );
            case REJECT -> "MOE does not meet minimum requirements. Complete restructure needed.";
        };
    }

}
