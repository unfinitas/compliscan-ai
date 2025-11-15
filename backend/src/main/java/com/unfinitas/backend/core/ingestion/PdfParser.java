package com.unfinitas.backend.core.ingestion;

import java.io.IOException;
import java.io.InputStream;

/**
 * Interface for PDF parsing operations
 */
public interface PdfParser {

    /**
     * Parse PDF and extract text content with metadata
     *
     * @param inputStream PDF file input stream
     * @param fileName    Original file name
     * @return Parsed PDF result containing text and metadata
     * @throws IOException if PDF parsing fails
     */
    ParsedPdfResult parse(InputStream inputStream, String fileName) throws IOException;

    /**
     * Result of PDF parsing operation
     */
    record ParsedPdfResult(
            String rawText,
            int pageCount,
            String title,
            String author
    ) {
    }
}
