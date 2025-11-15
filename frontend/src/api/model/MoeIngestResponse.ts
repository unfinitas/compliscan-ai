import { ProcessingStatus } from "./ProcessingStatus";

/**
 * Response DTO for MOE document ingestion (initial upload)
 */
export interface MoeIngestResponse {
  documentId: string;
  fileName: string;
  fileSize: number;
  paragraphCount: number;
  status: ProcessingStatus;
  createdAt: string;
}

