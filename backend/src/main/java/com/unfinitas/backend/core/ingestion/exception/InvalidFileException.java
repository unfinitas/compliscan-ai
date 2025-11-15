package com.unfinitas.backend.core.ingestion.exception;

/**
 * Exception thrown when uploaded file is invalid (wrong type, empty, corrupted)
 */
public class InvalidFileException extends RuntimeException {

    public InvalidFileException(String message) {
        super(message);
    }

    public InvalidFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
