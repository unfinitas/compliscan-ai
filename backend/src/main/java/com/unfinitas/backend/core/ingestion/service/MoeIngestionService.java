package com.unfinitas.backend.core.ingestion.service;

import com.unfinitas.backend.core.ingestion.PdfParser;
import com.unfinitas.backend.core.ingestion.exception.DocumentProcessingException;
import com.unfinitas.backend.core.ingestion.model.ContentType;
import com.unfinitas.backend.core.ingestion.model.MoeDocument;
import com.unfinitas.backend.core.ingestion.model.Paragraph;
import com.unfinitas.backend.core.ingestion.repository.MoeDocumentRepository;
import com.unfinitas.backend.core.ingestion.repository.ParagraphRepository;
import com.unfinitas.backend.util.FileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MoeIngestionService {

    private final PdfParser pdfParser;
    private final MoeDocumentRepository moeDocumentRepository;
    private final ParagraphRepository paragraphRepository;
    private final FileValidator fileValidator;
    private final SectionNumberExtractor sectionNumberExtractor;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Transactional
    public MoeDocument initiateIngestion(MultipartFile file) {
        try {
            fileValidator.validatePdfFile(file);

            String fileName = file.getOriginalFilename();
            Long fileSize = file.getSize();

            log.info("Initiating ingestion for file: {}, size: {} bytes", fileName, fileSize);

            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Created upload directory: {}", uploadPath.toAbsolutePath());
            }

            UUID documentId = UUID.randomUUID();
            String storedFileName = documentId + ".pdf";
            Path filePath = uploadPath.resolve(storedFileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Saved PDF file to: {}", filePath.toAbsolutePath());

            MoeDocument document = new MoeDocument(fileName, filePath.toString(), fileSize);
            document = moeDocumentRepository.save(document);
            log.info("Created document record with ID: {}", document.getId());

            processDocumentAsync(document.getId());

            return document;

        } catch (IOException e) {
            log.error("Failed to save uploaded file: {}", e.getMessage(), e);
            throw new DocumentProcessingException("Failed to save uploaded file: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to initiate ingestion: {}", e.getMessage(), e);
            throw new DocumentProcessingException("Failed to initiate ingestion: " + e.getMessage(), e);
        }
    }

    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processDocumentAsync(UUID documentId) {
        log.info("Starting async processing for document: {}", documentId);

        MoeDocument document = moeDocumentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentProcessingException("Document not found: " + documentId));

        try {
            Path filePath = Paths.get(document.getFilePath());
            
            if (!Files.exists(filePath)) {
                throw new DocumentProcessingException("PDF file not found at: " + filePath);
            }

            try (FileInputStream fileInputStream = new FileInputStream(filePath.toFile())) {
                PdfParser.ParsedPdfResult parsedResult = pdfParser.parse(fileInputStream, document.getFileName());
                document.updatePageCount(parsedResult.pageCount());

                String[] rawParagraphs = parsedResult.rawText().split("\n\n+");
                List<Paragraph> paragraphList = new ArrayList<>();
                int order = 0;

                String currentSectionNumber = null;
                String currentSectionTitle = null;
                String currentPartNumber = null;

                for (String content : rawParagraphs) {
                    String trimmed = content.trim();
                    if (trimmed.isEmpty()) continue;

                    SectionNumberExtractor.SectionInfo section = sectionNumberExtractor.extract(trimmed);

                    if (section != null) {
                        currentSectionNumber = section.number();
                        currentSectionTitle = section.title();

                        if (section.isPart()) {
                            currentPartNumber = section.number();
                        }

                        String parent = section.parent();
                        if (parent == null && !section.isPart() && currentPartNumber != null) {
                            parent = currentPartNumber;
                        }

                        ContentType type = hasContentBeyondHeading(trimmed, section) 
                            ? ContentType.PARAGRAPH 
                            : ContentType.HEADING;

                        Paragraph p = new Paragraph(
                            document, section.number(), section.title(), section.depth(), 
                            parent, order++, trimmed, type
                        );
                        paragraphList.add(p);

                    } else {
                        Paragraph p = new Paragraph(
                            document, currentSectionNumber, currentSectionTitle, 
                            null, null, order++, trimmed, ContentType.PARAGRAPH
                        );
                        paragraphList.add(p);
                    }
                }

                if (paragraphList.isEmpty()) {
                    document.markAsFailed("Document contains no valid paragraphs");
                    moeDocumentRepository.save(document);
                    log.warn("Document {} has no valid paragraphs", documentId);
                    return;
                }

                paragraphRepository.saveAll(paragraphList);
                document.markAsCompleted();
                moeDocumentRepository.save(document);

                log.info("Successfully processed document: {}, paragraphs: {}, sections: {}", 
                    documentId, paragraphList.size(), 
                    paragraphList.stream().filter(p -> p.getSectionDepth() != null).count());
            }

        } catch (Exception e) {
            String errorMessage = "Failed to process document: " + e.getMessage();
            document.markAsFailed(errorMessage);
            moeDocumentRepository.save(document);
            log.error("Failed to process document {}: {}", documentId, e.getMessage(), e);
        }
    }

    private boolean hasContentBeyondHeading(String text, SectionNumberExtractor.SectionInfo section) {
        String header = section.number() + " " + section.title();
        int headerEndIndex = text.indexOf(header);
        
        if (headerEndIndex == -1) {
            return true;
        }
        
        int contentStartIndex = headerEndIndex + header.length();
        if (contentStartIndex >= text.length()) {
            return false;
        }
        
        String remainingText = text.substring(contentStartIndex).trim();
        return !remainingText.isEmpty();
    }
}
