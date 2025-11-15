package com.unfinitas.backend.api.dto;

import com.unfinitas.backend.core.ingestion.model.MoeDocument;
import com.unfinitas.backend.core.ingestion.model.ProcessingStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for document status inquiry
 */
public record DocumentStatusResponse(
        UUID documentId,
        String fileName,
        Long fileSize,
        Integer pageCount,
        Integer paragraphCount,
        ProcessingStatus status,
        String errorMessage,
        LocalDateTime createdAt
) {
    /**
     * Create response from MoeDocument entity with paragraph count
     */
    public static DocumentStatusResponse from(MoeDocument document, int paragraphCount) {
        return new DocumentStatusResponse(
                document.getId(),
                document.getFileName(),
                document.getFileSize(),
                document.getPageCount(),
                paragraphCount,
                document.getProcessingStatus(),
                document.getErrorMessage(),
                document.getCreatedAt()
        );
    }
}
