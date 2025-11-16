"use client";

import { sendRequestWithResponse } from "@/utils/gateway";
import { RequestEnum } from "@/utils/requestEnum";
import { config } from "@/config/env";

const API_BASE = `${
  process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080"
}/api/analysis`;

/**
 * Response from starting an analysis
 */
export interface StartAnalysisResponse {
  analysisId: string;
  message: string;
}

/**
 * Paginated response structure from Spring Data
 */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

/**
 * Analysis report response
 */
export interface AnalysisReportResponse {
  analysisId: string;
  moeId: string;
  regulationVersion: string;
  totalRequirements: number;
  compliance: ComplianceOutcomeDto[];
}

/**
 * Compliance outcome DTO from backend
 */
export interface ComplianceOutcomeDto {
  requirement_id: string;
  compliance_status: "full" | "partial" | "non";
  finding_level: string;
  justification: string;
  evidence: string[];
  missing_elements: string[];
  recommended_actions: string[];
}

/**
 * Start a new compliance analysis
 *
 * @param moeId Document UUID (from Redux store - use useMoeId() hook to get it)
 * @param regulationId Optional regulation UUID (defaults to config.defaultRegulationId)
 * @returns Analysis ID
 *
 * Note:
 * - moeId should be obtained from Redux using useMoeId() hook
 * - moeId is included in both URL query params and request body (via gateway)
 * - regulationVersion is automatically included in the request body from config (NEXT_PUBLIC_REGULATION_VERSION)
 * - regulationId is sent as query parameter (from NEXT_PUBLIC_DEFAULT_REGULATION_ID or passed parameter)
 */
export async function startAnalysis(
  moeId: string,
  regulationId?: string
): Promise<StartAnalysisResponse> {
  const regId = regulationId || config.defaultRegulationId;
  const regulationVersion = config.regulationVersion;

  if (!regId) {
    throw new Error(
      "Regulation ID is not configured. Please set NEXT_PUBLIC_DEFAULT_REGULATION_ID in your .env.local file."
    );
  }

  const url = `${API_BASE}?moeId=${moeId}&regulationId=${regId}`;

  // Pass moeId to gateway - it will be added to the request body for POST requests
  // moeId is also in the URL query params for @RequestParam
  return sendRequestWithResponse<StartAnalysisResponse>(
    moeId,
    RequestEnum.POST,
    url,
    {
      regulationVersion: regulationVersion,
    }
  );
}

/**
 * Get analysis report (all compliance outcomes without pagination)
 *
 * @param analysisId Analysis UUID
 * @param moeId Optional moeId to include in the request
 * @returns Full analysis report
 */
export async function getAnalysisReport(
  analysisId: string,
  moeId: string | null = null
): Promise<AnalysisReportResponse> {
  return sendRequestWithResponse<AnalysisReportResponse>(
    moeId,
    RequestEnum.GET,
    `${API_BASE}/${analysisId}`
  );
}

/**
 * Get paginated compliance outcomes with optional filtering
 *
 * @param analysisId Analysis UUID
 * @param page Page number (0-indexed)
 * @param size Page size
 * @param complianceStatus Optional filter by status: "full", "partial", "non"
 * @param findingLevel Optional filter by finding level
 * @param moeId Optional moeId to include in the request
 * @returns Paginated compliance outcomes
 */
export async function getAnalysisOutcomes(
  analysisId: string,
  page: number = 0,
  size: number = 20,
  complianceStatus?: "full" | "partial" | "non",
  findingLevel?: string,
  moeId: string | null = null
): Promise<PageResponse<ComplianceOutcomeDto>> {
  const params = new URLSearchParams({
    page: page.toString(),
    size: size.toString(),
    sort: "requirementId",
  });

  if (complianceStatus) {
    params.append("complianceStatus", complianceStatus);
  }

  if (findingLevel) {
    params.append("findingLevel", findingLevel);
  }

  return sendRequestWithResponse<PageResponse<ComplianceOutcomeDto>>(
    moeId,
    RequestEnum.GET,
    `${API_BASE}/${analysisId}/outcomes?${params.toString()}`
  );
}
