package com.unfinitas.backend.api.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.unfinitas.backend.api.dto.DocumentStatusResponse;
import com.unfinitas.backend.api.dto.MoeIngestResponse;
import com.unfinitas.backend.core.analysis.service.EmbeddingService;
import com.unfinitas.backend.core.ingestion.model.MoeDocument;
import com.unfinitas.backend.core.ingestion.repository.MoeDocumentRepository;
import com.unfinitas.backend.core.ingestion.repository.ParagraphRepository;
import com.unfinitas.backend.core.ingestion.service.MoeIngestionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST API Controller for MOE document management
 */
@Slf4j
@RestController
@RequestMapping("/api/moe/documents")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class MoeDocumentController {
    private final MoeIngestionService moeIngestionService;
    private final MoeDocumentRepository moeDocumentRepository;
    private final ParagraphRepository paragraphRepository;
    private final EmbeddingService embeddingService;

    /**
     * Upload and initiate document processing
     * Returns immediately with PROCESSING status
     * Embeddings are generated asynchronously
     *
     * @param file Uploaded PDF file
     * @return Document metadata with PROCESSING status
     */
    @PostMapping
    public ResponseEntity<MoeIngestResponse> uploadDocument(
            @RequestParam("file") final MultipartFile file
    ) {
        log.info("Received document upload request: {}", file.getOriginalFilename());

        final MoeDocument document = moeIngestionService.initiateIngestion(file);
        final int paragraphCount = (int) paragraphRepository.countByMoeDocumentId(document.getId());

        final MoeIngestResponse response = MoeIngestResponse.from(document, paragraphCount);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(response);
    }

    /**
     * Get document processing status including embedding progress
     *
     * @param documentId Document UUID
     * @return Document status and metadata with embedding statistics
     */
    @GetMapping("/{documentId}/status")
    public ResponseEntity<DocumentStatusResponse> getDocumentStatus(
            @PathVariable final UUID documentId
    ) {
        log.debug("Getting status for document: {}", documentId);

        final MoeDocument document = moeDocumentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        final int paragraphCount = (int) paragraphRepository.countByMoeDocumentId(documentId);
        final int embeddedCount = paragraphRepository.countByMoeDocumentIdAndEmbeddingIsNotNull(documentId);

        final DocumentStatusResponse response = DocumentStatusResponse.from(
                document,
                paragraphCount,
                embeddedCount
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get overall embedding statistics
     *
     * @return Embedding statistics for all documents
     */
    @GetMapping("/embeddings/stats")
    public ResponseEntity<Map<String, Object>> getEmbeddingStats() {
        log.debug("Getting embedding statistics");
        final Map<String, Object> stats = embeddingService.getEmbeddingStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Manually trigger embedding generation for a specific document
     * Useful for retry or regeneration scenarios
     *
     * @param documentId Document UUID
     * @return Success message
     */
    @PostMapping("/{documentId}/embeddings/generate")
    public ResponseEntity<Map<String, Object>> generateEmbeddings(
            @PathVariable final UUID documentId
    ) {
        log.info("Manual embedding generation requested for document: {}", documentId);

        final MoeDocument document = moeDocumentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        final var paragraphs = paragraphRepository.findByMoeDocumentId(documentId);

        if (paragraphs.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No paragraphs found for document"));
        }

        embeddingService.generateParagraphEmbeddingsAsync(paragraphs);

        return ResponseEntity.accepted()
                .body(Map.of(
                        "message", "Embedding generation started",
                        "documentId", documentId,
                        "paragraphCount", paragraphs.size()
                ));
    }
}
