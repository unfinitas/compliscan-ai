package com.unfinitas.backend.core.ingestion.model;

public enum ProcessingStatus {
    PROCESSING,   // parsing PDF / extracting sections + paragraphs
    EMBEDDING,    // paragraphs saved, generating embeddings
    COMPLETED,    // everything done
    FAILED        // error in any stage
}
