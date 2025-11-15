package com.unfinitas.backend.core.ingestion.model;

/**
 * Processing status for MOE document ingestion
 */
public enum ProcessingStatus {
    /**
     * Currently processing the document
     */
    PROCESSING,

    /**
     * Processing completed successfully
     */
    COMPLETED,

    /**
     * Processing failed due to an error
     */
    FAILED
}
