package com.unfinitas.backend.core.ingestion.service;

import com.unfinitas.backend.core.analysis.service.EmbeddingService;
import com.unfinitas.backend.core.ingestion.PdfParser;
import com.unfinitas.backend.core.ingestion.exception.DocumentProcessingException;
import com.unfinitas.backend.core.ingestion.model.MoeDocument;
import com.unfinitas.backend.core.ingestion.model.Paragraph;
import com.unfinitas.backend.core.ingestion.model.Section;
import com.unfinitas.backend.core.ingestion.model.ProcessingStatus;
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
import java.nio.file.*;
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
    private final EmbeddingService embeddingService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    // ---------------------------------------------------------
    // INITIATION: save file + create document record
    // ---------------------------------------------------------
    @Transactional
    public MoeDocument initiateIngestion(final MultipartFile file) {
        try {
            fileValidator.validatePdfFile(file);

            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            UUID id = UUID.randomUUID();
            String storedFileName = id + ".pdf";
            Path filePath = uploadPath.resolve(storedFileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            MoeDocument document = new MoeDocument(
                    file.getOriginalFilename(),
                    filePath.toString(),
                    file.getSize()
            );
            document = moeDocumentRepository.save(document);

            processDocumentAsync(document.getId());
            return document;

        } catch (Exception e) {
            throw new DocumentProcessingException("Failed to initiate ingestion: " + e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------
    // ASYNC PROCESSING PIPELINE
    // ---------------------------------------------------------
    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processDocumentAsync(UUID documentId) {

        MoeDocument document = moeDocumentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentProcessingException("Document not found: " + documentId));

        try {
            Path filePath = Paths.get(document.getFilePath());
            if (!Files.exists(filePath)) {
                throw new DocumentProcessingException("File not found: " + filePath);
            }

            // -----------------------------
            // 1) PDF PARSING
            // -----------------------------
            log.info("Parsing PDF for document {}", documentId);

            PdfParser.ParsedPdfResult parsed = pdfParser.parse(
                    new FileInputStream(filePath.toFile()),
                    document.getFileName()
            );

            document.updatePageCount(parsed.pageCount());
            moeDocumentRepository.save(document);

            String[] rawLines = parsed.rawText().split("\n");

            Map<String, Section> sectionMap = new HashMap<>();
            List<ParagraphData> paraData = new ArrayList<>();

            int sectionOrder = 0;
            String currentSection = null;
            StringBuilder buffer = new StringBuilder();

            for (String line : rawLines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                SectionNumberExtractor.SectionInfo sec = sectionNumberExtractor.extract(trimmed);

                if (sec != null) {
                    if (!buffer.isEmpty()) {
                        paraData.add(new ParagraphData(currentSection, buffer.toString().trim()));
                        buffer = new StringBuilder();
                    }

                    currentSection = sec.number();

                    Section parent = sec.parent() != null ? sectionMap.get(sec.parent()) : null;
                    sectionMap.putIfAbsent(
                            sec.number(),
                            new Section(document, sec.number(), sec.title(), sec.depth(), parent, sectionOrder++)
                    );

                    continue;
                }

                if (!buffer.isEmpty()) buffer.append("\n");
                buffer.append(trimmed);
            }

            if (!buffer.isEmpty()) {
                paraData.add(new ParagraphData(currentSection, buffer.toString().trim()));
            }

            // -----------------------------
            // 2) Save SECTIONS + PARAGRAPHS
            // -----------------------------
            sectionRepository.saveAll(sectionMap.values());

            List<Paragraph> paragraphs = new ArrayList<>();
            int order = 0;

            for (ParagraphData data : paraData) {
                Section s = data.sectionNumber != null ? sectionMap.get(data.sectionNumber) : null;
                paragraphs.add(new Paragraph(document, s, order++, data.content));
            }

            if (paragraphs.isEmpty()) {
                document.markAsFailed("No valid paragraphs extracted");
                moeDocumentRepository.save(document);
                return;
            }

            paragraphRepository.saveAll(paragraphs);

            // -----------------------------
            // 3) EMBEDDINGS STAGE
            // -----------------------------
            document.markEmbedding();
            moeDocumentRepository.save(document);

            // async embedding (safe)
            embeddingService.generateParagraphEmbeddingsAsync(paragraphs, document.getId());

        } catch (Exception e) {
            document.markAsFailed(e.getMessage());
            moeDocumentRepository.save(document);
            log.error("Ingestion failed for {}: {}", documentId, e.getMessage(), e);
        }
    }

    private record ParagraphData(String sectionNumber, String content) {}
}
