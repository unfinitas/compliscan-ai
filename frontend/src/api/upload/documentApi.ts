"use client";

import { MoeIngestResponse } from "../model/MoeIngestResponse";
import { DocumentStatusResponse } from "../model/DocumentStatusResponse";
import { uploadFile, sendRequestWithResponse } from "@/utils/gateway";
import { RequestEnum } from "@/utils/requestEnum";
import { ApiResponse } from "@/utils/ApiResponse";

const API_BASE = `${process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080"}/api/moe/documents`;

/**
 * Upload and initiate document processing
 * Returns immediately with PROCESSING status
 *
 * @param file Uploaded PDF file
 * @param moeId Optional moeId to include in the request
 * @returns ApiResponse with document metadata and PROCESSING status
 */
export async function uploadDocument(
  file: File,
  moeId: string | null = null
): Promise<ApiResponse<MoeIngestResponse>> {
  const formData = new FormData();
  formData.append("file", file);

  return uploadFile<MoeIngestResponse>(moeId, API_BASE, formData);
}

/**
 * Get document processing status
 *
 * @param documentId Document UUID
 * @param moeId Optional moeId to include in the request
 * @returns ApiResponse with document status and metadata
 */
export async function getDocumentStatus(
  documentId: string,
  moeId: string | null = null
): Promise<ApiResponse<DocumentStatusResponse>> {
  return sendRequestWithResponse<DocumentStatusResponse>(
    moeId,
    RequestEnum.GET,
    `${API_BASE}/${documentId}/status`
  );
}
