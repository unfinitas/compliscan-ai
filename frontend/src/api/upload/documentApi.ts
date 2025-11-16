"use client";

import { MoeIngestResponse } from "../model/MoeIngestResponse";
import { uploadFile } from "@/utils/gateway";

const API_BASE = `${
  process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080"
}/api/moe/documents`;

/**
 * Upload and initiate document processing
 * Returns immediately with PROCESSING status
 *
 * @param file Uploaded PDF file (optional if documentId is provided for status check)
 * @param moeId Optional moeId to include in the request
 * @param documentId Optional documentId to check status without re-uploading
 * @returns Document metadata with PROCESSING status
 */
export async function uploadDocument(
  file: File | null = null,
  moeId: string | null = null,
  documentId: string | null = null
): Promise<MoeIngestResponse> {
  const formData = new FormData();

  // If file is provided, append it (for initial upload)
  if (file) {
    formData.append("file", file);
  }

  // If documentId is provided, append it to check status
  if (documentId) {
    formData.append("documentId", documentId);
  }

  return uploadFile<MoeIngestResponse>(moeId, API_BASE, formData);
}

/**
 * Poll upload endpoint until embedding is 100% complete
 * Re-calls the upload endpoint with documentId to check progress
 *
 * @param file Original file that was uploaded (needed for re-calling endpoint)
 * @param documentId Document UUID to check status
 * @param moeId Optional moeId to include in the request
 * @param onProgress Callback with progress percentage (0-100)
 * @param pollInterval Polling interval in milliseconds (default: 5000ms)
 * @returns Final document response when embedding is complete
 */
export async function pollDocumentStatus(
  file: File,
  documentId: string,
  moeId: string | null = null,
  onProgress?: (progress: number) => void,
  pollInterval: number = 5000
): Promise<MoeIngestResponse> {
  let lastParagraphCount = 0;

  return new Promise((resolve, reject) => {
    const poll = async () => {
      try {
        // Re-call the upload endpoint with documentId to check status
        const response = await uploadDocument(file, moeId, documentId);

        // Track progress based on paragraphCount changes
        // When paragraphCount stabilizes and status is COMPLETED, embedding is done
        const currentParagraphCount = response.paragraphCount || 0;

        // Calculate progress: if paragraphs are being processed, show progress
        // Once status is COMPLETED, we're at 100%
        let progress = 0;
        if (response.status === "COMPLETED") {
          progress = 100;
        } else if (currentParagraphCount > 0) {
          // Estimate progress: if paragraphCount increased, we're making progress
          if (currentParagraphCount > lastParagraphCount) {
            // Progress is increasing, estimate based on status
            progress = Math.min(
              90,
              50 + (currentParagraphCount - lastParagraphCount) * 10
            );
          } else if (
            currentParagraphCount === lastParagraphCount &&
            lastParagraphCount > 0
          ) {
            // ParagraphCount stable, assume we're processing embeddings
            progress = 75;
          } else {
            // Initial state
            progress = 25;
          }
        } else {
          // No paragraphs yet, just started
          progress = 10;
        }

        lastParagraphCount = currentParagraphCount;

        // Call progress callback
        onProgress?.(progress);

        // Check if processing is complete
        if (response.status === "COMPLETED") {
          onProgress?.(100);
          resolve(response);
          return;
        }

        // Check if processing failed
        if (response.status === "FAILED") {
          reject(new Error("Document processing failed"));
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
