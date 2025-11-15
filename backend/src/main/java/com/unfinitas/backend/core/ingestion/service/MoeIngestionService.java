package com.unfinitas.backend.core.ingestion.service;

import com.unfinitas.backend.core.ingestion.PdfParser;
import com.unfinitas.backend.core.ingestion.exception.DocumentProcessingException;
import com.unfinitas.backend.core.ingestion.model.MoeDocument;
import com.unfinitas.backend.core.ingestion.model.Paragraph;
import com.unfinitas.backend.core.ingestion.model.Section;
import com.unfinitas.backend.core.ingestion.repository.MoeDocumentRepository;
import com.unfinitas.backend.core.ingestion.repository.ParagraphRepository;
import com.unfinitas.backend.core.ingestion.repository.SectionRepository;
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
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MoeIngestionService {

    private final PdfParser pdfParser;
    private final MoeDocumentRepository moeDocumentRepository;
    private final ParagraphRepository paragraphRepository;
    private final SectionRepository sectionRepository;
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

                // Log raw text preview
                String rawText = parsedResult.rawText();
                log.info("PDF raw text length: {} chars", rawText.length());
                log.debug("First 500 chars of raw text: {}", rawText.substring(0, Math.min(500, rawText.length())));

                // Split by single newline since PDFBox doesn't preserve paragraph breaks
                String[] rawLines = parsedResult.rawText().split("\n");
                log.info("Split into {} lines for processing", rawLines.length);

                Map<String, Section> sectionMap = new HashMap<>();
                List<ParagraphData> paragraphDataList = new ArrayList<>();

                int sectionOrder = 0;
                String currentSectionNumber = null;
                StringBuilder currentParagraph = new StringBuilder();

                for (String line : rawLines) {
                    String trimmed = line.trim();

                    // Skip empty lines
                    if (trimmed.isEmpty()) {
                        continue;
                    }

                    // Try to extract section info from this line
                    SectionNumberExtractor.SectionInfo sectionInfo = sectionNumberExtractor.extract(trimmed);

                    if (sectionInfo != null) {
                        // Found a section header!
                        log.info("FOUND SECTION: {} - {} (depth: {})",
                            sectionInfo.number(), sectionInfo.title(), sectionInfo.depth());

                        // Save accumulated paragraph before starting new section
                        if (currentParagraph.length() > 0) {
                            paragraphDataList.add(new ParagraphData(currentSectionNumber, currentParagraph.toString().trim()));
                            currentParagraph = new StringBuilder();
                        }

                        currentSectionNumber = sectionInfo.number();

                        // Create Section entity if not already exists
                        if (!sectionMap.containsKey(currentSectionNumber)) {
                            Section parentSection = null;
                            if (sectionInfo.parent() != null) {
                                parentSection = sectionMap.get(sectionInfo.parent());
                            }

                            Section section = new Section(
                                document,
                                sectionInfo.number(),
                                sectionInfo.title(),
                                sectionInfo.depth(),
                                parentSection,
                                sectionOrder++
                            );
                            sectionMap.put(currentSectionNumber, section);
                        }

                        // Add the section header line itself as content if it has extra text
                        if (hasContentBeyondHeading(trimmed, sectionInfo)) {
                            currentParagraph.append(trimmed).append("\n");
                        }

                    } else {
                        // Regular content line - accumulate into current paragraph
                        if (currentParagraph.length() > 0) {
                            currentParagraph.append("\n");
                        }
                        currentParagraph.append(trimmed);

                        // If paragraph is getting large, save it and start new one
                        if (currentParagraph.length() > 1000) {
                            paragraphDataList.add(new ParagraphData(currentSectionNumber, currentParagraph.toString().trim()));
                            currentParagraph = new StringBuilder();
                        }
                    }
                }

                // Don't forget the last accumulated paragraph
                if (currentParagraph.length() > 0) {
                    paragraphDataList.add(new ParagraphData(currentSectionNumber, currentParagraph.toString().trim()));
                }

                log.info("Accumulated {} paragraphs from {} lines", paragraphDataList.size(), rawLines.length);

                List<Section> sections = new ArrayList<>(sectionMap.values());
                sectionRepository.saveAll(sections);
                log.info("Saved {} sections for document {}", sections.size(), documentId);

                // Log section details
                if (sections.isEmpty()) {
                    log.warn("WARNING: No sections were extracted from the document!");
                } else {
                    log.info("Section extraction successful:");
                    sections.stream().limit(10).forEach(s ->
                        log.info("  - {} : {} (depth: {})", s.getSectionNumber(), s.getSectionTitle(), s.getDepth())
                    );
                    if (sections.size() > 10) {
                        log.info("  ... and {} more sections", sections.size() - 10);
                    }
                }

                List<Paragraph> paragraphList = new ArrayList<>();
                int paragraphOrder = 0;

                for (ParagraphData data : paragraphDataList) {
                    Section section = data.sectionNumber != null ? sectionMap.get(data.sectionNumber) : null;
                    Paragraph paragraph = new Paragraph(document, section, paragraphOrder++, data.content);
                    paragraphList.add(paragraph);
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

                log.info("Successfully processed document: {}, sections: {}, paragraphs: {}",
                    documentId, sections.size(), paragraphList.size());
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

    private record ParagraphData(String sectionNumber, String content) {}
}
