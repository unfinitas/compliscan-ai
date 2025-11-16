package com.unfinitas.backend.core.ingestion.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

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

    public MoeDocument(String fileName, String filePath, Long fileSize) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.processingStatus = ProcessingStatus.PROCESSING;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // --- STATE TRANSITIONS ---------------------------------------------------

    public void updatePageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    /** Called after paragraphs are extracted and saved */
    public void markEmbedding() {
        if (this.processingStatus != ProcessingStatus.PROCESSING) {
            throw new IllegalStateException("Can only mark EMBEDDING from PROCESSING");
        }
        this.processingStatus = ProcessingStatus.EMBEDDING;
    }

    public void markAsCompleted() {
        if (this.processingStatus != ProcessingStatus.EMBEDDING &&
                this.processingStatus != ProcessingStatus.PROCESSING)
        {
            throw new IllegalStateException("Invalid transition to COMPLETED");
        }
        this.processingStatus = ProcessingStatus.COMPLETED;
    }

    /** Error state */
    public void markAsFailed(final String errorMessage) {
        this.processingStatus = ProcessingStatus.FAILED;
        this.errorMessage = errorMessage;
    }
}
