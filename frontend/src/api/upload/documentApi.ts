"use client";

import { MoeIngestResponse } from "../model/MoeIngestResponse";
import { DocumentStatusResponse } from "../model/DocumentStatusResponse";
import { uploadFile, sendRequestWithResponse } from "@/utils/gateway";
import { RequestEnum } from "@/utils/requestEnum";

const API_BASE = `${
  process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080"
}/api/moe/documents`;

/**
 * Upload and initiate document processing
 * Returns immediately with PROCESSING status
 *
 * @param file Uploaded PDF file
 * @param moeId Optional moeId to include in the request
 * @returns Document metadata with PROCESSING status
 */
export async function uploadDocument(
  file: File,
  moeId: string | null = null
): Promise<MoeIngestResponse> {
  const formData = new FormData();
  formData.append("file", file);

  return uploadFile<MoeIngestResponse>(moeId, API_BASE, formData);
}

/**
 * Get document processing status
 *
 * @param documentId Document UUID
 * @param moeId Optional moeId to include in the request
 * @returns Document status and metadata
 */
export async function getDocumentStatus(
  documentId: string,
  moeId: string | null = null
): Promise<DocumentStatusResponse> {
  return sendRequestWithResponse<DocumentStatusResponse>(
    moeId,
    RequestEnum.GET,
    `${API_BASE}/${documentId}/status`
  );
}

/**
 * Poll document status until processing is complete
 *
 * @param documentId Document UUID
 * @param moeId Optional moeId to include in the request
 * @param onProgress Callback with progress percentage (0-100) and status
 * @param pollInterval Polling interval in milliseconds (default: 5000ms)
 * @returns Final document status when complete
 */
export async function pollDocumentStatus(
  documentId: string,
  moeId: string | null = null,
  onProgress?: (progress: number, status: DocumentStatusResponse) => void,
  pollInterval: number = 5000
): Promise<DocumentStatusResponse> {
  return new Promise((resolve, reject) => {
    const poll = async () => {
      try {
        const status = await getDocumentStatus(documentId, moeId);

        // Calculate progress percentage
        const progress =
          status.totalParagraphs > 0
            ? Math.min(
                100,
                Math.round(
                  (status.embeddedParagraphs / status.totalParagraphs) * 100
                )
              )
            : 0;

        // Call progress callback
        onProgress?.(progress, status);

        // Check if embedding is complete
        if (status.embeddingComplete || status.status === "COMPLETED") {
          resolve(status);
          return;
        }

        // Check if processing failed
        if (status.status === "FAILED") {
          reject(
            new Error(status.errorMessage || "Document processing failed")
          );
          return;
        }

        // Continue polling
        setTimeout(poll, pollInterval);
      } catch (error) {
        reject(error);
      }
    };

    // Start polling immediately, then continue every pollInterval
    poll();
  });
}
