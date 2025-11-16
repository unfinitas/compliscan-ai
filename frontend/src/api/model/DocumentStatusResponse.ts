import { ProcessingStatus } from "./ProcessingStatus";

/**
 * Response DTO for document status inquiry
 */
export interface DocumentStatusResponse {
  documentId: string;
  filename: string;
  status: ProcessingStatus;
  errorMessage: string | null;
  totalParagraphs: number;
  embeddedParagraphs: number;
  embeddingComplete: boolean;
  uploadedAt: string;
  processedAt: string | null;
}
