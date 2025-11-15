package com.unfinitas.backend.core.ingestion;

import com.unfinitas.backend.core.ingestion.exception.DocumentProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * PDF parser implementation using Apache PDFBox
 */
@Slf4j
@Component
public class PdfBoxParser implements PdfParser {

    @Override
    public ParsedPdfResult parse(InputStream inputStream, String fileName) {
        log.debug("Starting PDF parsing for file: {}", fileName);

        Path tempFile = null;
        try {
            // Create temporary file for safe PDF loading
            tempFile = Files.createTempFile("moe-", ".pdf");
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);

            // Load PDF from file (memory-efficient)
            try (PDDocument document = Loader.loadPDF(tempFile.toFile())) {
                // Extract text content
                PDFTextStripper stripper = new PDFTextStripper();
                String rawText = stripper.getText(document);

                // Extract metadata
                int pageCount = document.getNumberOfPages();
                PDDocumentInformation info = document.getDocumentInformation();

                String title = info.getTitle();
                String author = info.getAuthor();

                log.debug("PDF parsing completed. File: {}, Pages: {}, Text length: {}",
                        fileName, pageCount, rawText.length());

                return new ParsedPdfResult(rawText, pageCount, title, author);
            }
        } catch (IOException e) {
            log.error("Failed to parse PDF file: {}", fileName, e);
            throw new DocumentProcessingException("Failed to parse PDF file: " + fileName, e);
        } finally {
            // Clean up temporary file
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                    log.debug("Temporary file deleted: {}", tempFile);
                } catch (IOException e) {
                    log.warn("Failed to delete temporary file: {}", tempFile, e);
                }
            }
        }
    }
}
