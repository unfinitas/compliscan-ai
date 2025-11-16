/**
 * Shared types for compliance requirements
 * These types match the transformed data structure from the API
 */

export interface SubRequirement {
  text: string;
  covered: boolean;
  evidence_excerpt: string;
}

export interface Evidence {
  moe_paragraph_id: number;
  relevant_excerpt: string;
  full_paragraph_excerpt: string;
  similarity_score: number;
  rerank_score: number;
}

export interface Requirement {
  requirement_id: string;
  requirement_text: string;
  compliance_status: "full" | "partial" | "non";
  sub_requirements: SubRequirement[];
  evidence: Evidence[];
  justification: string;
  missing_elements: string[];
  finding_level: string;
  recommended_actions: string[];
}


export interface ComplianceEvidence {
  moe_paragraph_id: number;
  relevant_excerpt: string;
  similarity_score: number;
  rerank_score: number;
}

export interface ComplianceRequirement {
  recommended_actions: string[] | null;
  evidence: ComplianceEvidence[] | null;
  finding_level: string;
  justification: string;
  missing_elements: string[] | null;
  requirement_id: string;
  compliance_status: "full" | "partial" | "non";
}

export interface AnalysisResponse {
  moeId: string;
  analysisId: string;
  totalRequirements: number;
  compliance: ComplianceRequirement[];
  regulationVersion: string;
}

