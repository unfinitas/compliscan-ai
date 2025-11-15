package com.unfinitas.backend.core.ingestion.exception;

/**
 * Exception thrown when document processing fails during ingestion
 */
public class DocumentProcessingException extends RuntimeException {

    public DocumentProcessingException(String message) {
        super(message);
    }

    public DocumentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
