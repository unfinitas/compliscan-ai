/**
 * Transforms compliance data from API response format to component format
 */

import type {
  ComplianceRequirement,
  Requirement,
  Evidence,
} from "@/types/compliance";
import type { ComplianceOutcomeDto } from "@/api/analysis/analysisApi";

type TransformedEvidence = Evidence;
type TransformedRequirement = Requirement;

/**
 * Transforms compliance data from API format to component format
 * Handles both the old ComplianceRequirement format and the new ComplianceOutcomeDto format
 */
export function transformComplianceData(
  compliance: ComplianceRequirement[] | ComplianceOutcomeDto[]
): TransformedRequirement[] {
  return compliance.map((req) => {
    // Handle evidence - API returns string[] but we need Evidence[]
    let evidenceArray: Evidence[] = [];

    if (Array.isArray(req.evidence)) {
      if (req.evidence.length > 0) {
        // Check if it's already in the correct format
        if (typeof req.evidence[0] === 'object' && 'moe_paragraph_id' in req.evidence[0]) {
          evidenceArray = req.evidence as unknown as Evidence[];
        } else {
          // It's a string array, convert to Evidence objects
          evidenceArray = (req.evidence as string[]).map((ev, idx) => ({
            moe_paragraph_id: idx,
            relevant_excerpt: ev,
            full_paragraph_excerpt: ev,
            similarity_score: 0,
            rerank_score: 0,
          }));
        }
      }
    }

    const missingElements = req.missing_elements || [];
    const subRequirements = missingElements.map((element) => ({
      text: element,
      covered: false,
      evidence_excerpt: evidenceArray[0]?.relevant_excerpt || "",
    }));

    const justification = req.justification || "";
    const requirementText = `${req.requirement_id}: ${justification.substring(0, 200)}${justification.length > 200 ? "..." : ""}`;

    return {
      requirement_id: req.requirement_id,
      requirement_text: requirementText,
      compliance_status: req.compliance_status,
      sub_requirements: subRequirements,
      evidence: evidenceArray,
      justification: justification,
      missing_elements: missingElements,
      finding_level: req.finding_level || "",
      recommended_actions: req.recommended_actions || [],
    };
  });
}

