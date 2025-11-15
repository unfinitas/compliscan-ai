package com.unfinitas.backend.util;

import com.unfinitas.backend.core.ingestion.exception.InvalidFileException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Utility for validating uploaded files
 */
@Component
public class FileValidator {

    private static final String PDF_CONTENT_TYPE = "application/pdf";

    /**
     * Validate that uploaded file is a PDF
     */
    public void validatePdfFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File is empty or null");
        }

        // Check content type
        String contentType = file.getContentType();
        if (!PDF_CONTENT_TYPE.equals(contentType)) {
            throw new InvalidFileException(
                    "Invalid file type: " + contentType + ". Only PDF files are allowed."
            );
        }

        // Check file extension
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".pdf")) {
            throw new InvalidFileException("File must have .pdf extension");
        }
    }
}
