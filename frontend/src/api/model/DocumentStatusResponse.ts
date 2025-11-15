import { ProcessingStatus } from "./ProcessingStatus";

/**
 * Response DTO for document status inquiry
 */
export interface DocumentStatusResponse {
  documentId: string;
  fileName: string;
  fileSize: number;
  pageCount: number | null;
  paragraphCount: number;
  status: ProcessingStatus;
  errorMessage: string | null;
  createdAt: string;
}

