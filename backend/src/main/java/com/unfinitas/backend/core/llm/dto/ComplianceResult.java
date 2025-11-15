package com.unfinitas.backend.core.llm.dto;

import java.util.List;

public record ComplianceResult(
        String requirement_id,
        List<EvidenceItem> evidence,
        String compliance_status,
        String justification,
        List<String> missing_elements,
        String finding_level,
        List<String> recommended_actions
) {
    public static ComplianceResult empty() {
        return new ComplianceResult(
                "",
                List.of(),
                "non",
                "",
                List.of(),
                "Recommendation",
                List.of()
        );
    }

    public record EvidenceItem(
            Long moe_paragraph_id,
            String relevant_excerpt,
            Double similarity_score,
            Double rerank_score
    ) {
    }
}
