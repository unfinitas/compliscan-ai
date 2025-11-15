package com.unfinitas.backend.api.dto;

import com.unfinitas.backend.core.ingestion.model.MoeDocument;
import com.unfinitas.backend.core.ingestion.model.ProcessingStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record DocumentStatusResponse(
        UUID documentId,
        String filename,
        ProcessingStatus status,
        String errorMessage,
        int totalParagraphs,
        int embeddedParagraphs,
        boolean embeddingComplete,
        LocalDateTime uploadedAt,
        LocalDateTime processedAt
) {
    public static DocumentStatusResponse from(
            final MoeDocument document,
            final int paragraphCount,
            final int embeddedCount
    ) {
        return DocumentStatusResponse.builder()
                .documentId(document.getId())
                .filename(document.getFileName())
                .status(document.getProcessingStatus())
                .errorMessage(document.getErrorMessage())
                .totalParagraphs(paragraphCount)
                .embeddedParagraphs(embeddedCount)
                .embeddingComplete(paragraphCount > 0 && paragraphCount == embeddedCount)
                .build();
    }
}
