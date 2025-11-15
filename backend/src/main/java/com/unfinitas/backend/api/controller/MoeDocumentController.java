package com.unfinitas.backend.api.controller;

import com.unfinitas.backend.api.dto.DocumentStatusResponse;
import com.unfinitas.backend.api.dto.MoeIngestResponse;
import com.unfinitas.backend.core.ingestion.model.MoeDocument;
import com.unfinitas.backend.core.ingestion.repository.MoeDocumentRepository;
import com.unfinitas.backend.core.ingestion.repository.ParagraphRepository;
import com.unfinitas.backend.core.ingestion.service.MoeIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * REST API Controller for MOE document management
 */
@Slf4j
@RestController
@RequestMapping("/api/moe/documents")
@RequiredArgsConstructor
public class MoeDocumentController {

    private final MoeIngestionService moeIngestionService;
    private final MoeDocumentRepository moeDocumentRepository;
    private final ParagraphRepository paragraphRepository;

    /**
     * Upload and initiate document processing
     * Returns immediately with PROCESSING status
     *
     * @param file Uploaded PDF file
     * @return Document metadata with PROCESSING status
     */
    @PostMapping
    public ResponseEntity<MoeIngestResponse> uploadDocument(
            @RequestParam("file") MultipartFile file
    ) {
        log.info("Received document upload request: {}", file.getOriginalFilename());

        MoeDocument document = moeIngestionService.initiateIngestion(file);
        int paragraphCount = (int) paragraphRepository.countByMoeDocumentId(document.getId());
        MoeIngestResponse response = MoeIngestResponse.from(document, paragraphCount);


        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(response);
    }

    /**
     * Get document processing status
     *
     * @param documentId Document UUID
     * @return Document status and metadata
     */
    @GetMapping("/{documentId}/status")
    public ResponseEntity<DocumentStatusResponse> getDocumentStatus(
            @PathVariable UUID documentId
    ) {
        log.debug("Getting status for document: {}", documentId);

        MoeDocument document = moeDocumentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        int paragraphCount = (int) paragraphRepository.countByMoeDocumentId(documentId);
        DocumentStatusResponse response = DocumentStatusResponse.from(document, paragraphCount);

        return ResponseEntity.ok(response);
    }
}
