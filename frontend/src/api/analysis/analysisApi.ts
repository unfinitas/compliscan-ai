"use client";

import { sendRequestWithResponse } from "@/utils/gateway";
import { RequestEnum } from "@/utils/requestEnum";

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
 * Backend endpoint: POST /api/analysis?moeId={uuid}
 * Backend only requires moeId as @RequestParam (no regulationId needed - it auto-detects)
 *
 * @param moeId MOE ID (should come from Redux using useMoeId() hook)
 * @returns Analysis ID
 */
export async function startAnalysis(
  moeId: string | null = null
): Promise<StartAnalysisResponse> {
  if (!moeId) {
    throw new Error(
      "MOE ID is required. Please ensure a document is uploaded and moeId is available in Redux store."
    );
  }

  // Build URL with query parameter - backend expects moeId as @RequestParam
  // Backend auto-detects regulationId from database (expects exactly one regulation)
  const url = `${API_BASE}?moeId=${encodeURIComponent(moeId)}`;

  // Use fetch directly to ensure POST method with query params works correctly
  const baseUrl =
    process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080";
  const fullUrl = url.startsWith("http")
    ? url
    : `${baseUrl.replace(/\/$/, "")}/${url.replace(/^\//, "")}`;

  const response = await fetch(fullUrl, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: undefined,
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(
      `Request failed: ${response.status} ${response.statusText}. ${errorText}`
    );
  }

  return response.json();
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
