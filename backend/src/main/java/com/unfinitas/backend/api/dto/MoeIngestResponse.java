package com.unfinitas.backend.api.dto;

import com.unfinitas.backend.core.ingestion.model.MoeDocument;
import com.unfinitas.backend.core.ingestion.model.ProcessingStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for MOE document ingestion (initial upload)
 */
public record MoeIngestResponse(
        UUID documentId,
        String fileName,
        Long fileSize,
        Integer paragraphCount,
        ProcessingStatus status,
        LocalDateTime createdAt
) {
    /**
     * Create response from MoeDocument entity with paragraph count
     */
    public static MoeIngestResponse from(MoeDocument document, int paragraphCount) {
        return new MoeIngestResponse(
                document.getId(),
                document.getFileName(),
                document.getFileSize(),
                paragraphCount,
                document.getProcessingStatus(),
                document.getCreatedAt()
        );
    }
}
