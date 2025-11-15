package com.unfinitas.backend.core.ingestion.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * MOE (Maintenance Organization Exposition) Document entity
 * Immutable entity - once uploaded, never updated (except processing status)
 */
@Entity
@Table(name = "moe_documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MoeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "page_count")
    private Integer pageCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 50)
    private ProcessingStatus processingStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // Constructor for creating new MOE document
    public MoeDocument(String fileName, String filePath, Long fileSize) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.processingStatus = ProcessingStatus.PROCESSING;
        this.createdAt = LocalDateTime.now();
    }

    // Lifecycle callbacks

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // Domain methods

    public void updatePageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public void markAsCompleted() {
        if (this.processingStatus != ProcessingStatus.PROCESSING) {
            throw new IllegalStateException("Can only mark PROCESSING documents as COMPLETED");
        }
        this.processingStatus = ProcessingStatus.COMPLETED;
    }

    public void markAsFailed(String errorMessage) {
        if (this.processingStatus != ProcessingStatus.PROCESSING) {
            throw new IllegalStateException("Can only mark PROCESSING documents as FAILED");
        }
        this.processingStatus = ProcessingStatus.FAILED;
        this.errorMessage = errorMessage;
    }
}
