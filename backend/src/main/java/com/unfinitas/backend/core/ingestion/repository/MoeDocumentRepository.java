package com.unfinitas.backend.core.ingestion.repository;

import com.unfinitas.backend.core.ingestion.model.MoeDocument;
import com.unfinitas.backend.core.ingestion.model.ProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for MOE Document persistence
 */
public interface MoeDocumentRepository extends JpaRepository<MoeDocument, UUID> {

    /**
     * Find all documents by processing status
     */
    List<MoeDocument> findByProcessingStatus(ProcessingStatus status);

    /**
     * Find all documents ordered by creation date (newest first)
     */
    List<MoeDocument> findAllByOrderByCreatedAtDesc();
}
