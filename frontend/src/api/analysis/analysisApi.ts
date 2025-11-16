"use client";

import { sendRequestWithResponse, fetchRequest } from "@/utils/gateway";
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
 * @param moeId Document UUID
 * @param regulationId Regulation UUID
 * @returns Analysis ID
 */
export async function startAnalysis(
  moeId: string,
  regulationId: string
): Promise<StartAnalysisResponse> {
  const baseUrl = process.env.NEXT_PUBLIC_BACKEND_URL ?? "";
  const url = `${baseUrl.replace(/\/$/, "")}/api/analysis?moeId=${moeId}&regulationId=${regulationId}`;

  const response = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(
      `Failed to start analysis: ${response.status} ${response.statusText}. ${errorText}`
    );
  }

  return response.json();
}

/**
 * Get analysis report (all compliance outcomes without pagination)
 *
 * @param analysisId Analysis UUID
 * @returns Full analysis report
 */
export async function getAnalysisReport(
  analysisId: string
): Promise<AnalysisReportResponse> {
  const baseUrl = process.env.NEXT_PUBLIC_BACKEND_URL ?? "";
  const url = `${baseUrl.replace(/\/$/, "")}/api/analysis/${analysisId}`;

  const response = await fetch(url, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(
      `Failed to get analysis report: ${response.status} ${response.statusText}. ${errorText}`
    );
  }

  return response.json();
}

/**
 * Get paginated compliance outcomes with optional filtering
 *
 * @param analysisId Analysis UUID
 * @param page Page number (0-indexed)
 * @param size Page size
 * @param complianceStatus Optional filter by status: "full", "partial", "non"
 * @param findingLevel Optional filter by finding level
 * @returns Paginated compliance outcomes
 */
export async function getAnalysisOutcomes(
  analysisId: string,
  page: number = 0,
  size: number = 20,
  complianceStatus?: "full" | "partial" | "non",
  findingLevel?: string
): Promise<PageResponse<ComplianceOutcomeDto>> {
  const baseUrl = process.env.NEXT_PUBLIC_BACKEND_URL ?? "";
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

  const url = `${baseUrl.replace(/\/$/, "")}/api/analysis/${analysisId}/outcomes?${params.toString()}`;

  const response = await fetch(url, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(
      `Failed to get analysis outcomes: ${response.status} ${response.statusText}. ${errorText}`
    );
  }

  return response.json();
}

