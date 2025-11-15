package com.unfinitas.backend.core.regulation.service;

import com.unfinitas.backend.core.regulation.dto.ClauseData;
import com.unfinitas.backend.core.regulation.dto.RegulationData;
import com.unfinitas.backend.core.regulation.model.Regulation;
import com.unfinitas.backend.core.regulation.model.RegulationClause;
import com.unfinitas.backend.core.regulation.parser.EasaXmlParser;
import com.unfinitas.backend.core.regulation.parser.EasaPdfParser;
import com.unfinitas.backend.core.regulation.repository.RegulationRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class RegulationLoaderService {

    private final RegulationRepository regulationRepo;
    private final EasaXmlParser xmlParser;
    private final EasaPdfParser pdfParser;

    @PostConstruct
    public void loadRegulationsOnStartup() {
        if (regulationRepo.count() == 0) {
            log.info("Starting async regulation loading...");
            loadRegulationsAsync();
        }
    }

    @Async
    @Transactional
    public void loadRegulationsAsync() {
        try {
            log.info("Loading regulations from XML and PDF...");
            loadFromResources();
            log.info("Regulation loading completed successfully");
        } catch (final Exception e) {
            log.error("Failed to load regulations", e);
        }
    }

    private void loadFromResources() throws Exception {
        final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        final Resource[] xmlResources = resolver.getResources("classpath:regulation-data/xml/*.xml");
        for (final Resource resource : xmlResources) {
            loadFromXml(resource);
        }

        final Resource[] pdfResources = resolver.getResources("classpath:regulation-data/pdf/*.pdf");
        for (final Resource resource : pdfResources) {
            loadFromPdf(resource);
        }
    }

    private void loadFromXml(final Resource resource) throws Exception {
        log.info("Loading from XML: {}", resource.getFilename());

        final File tempFile = File.createTempFile("regulation", ".xml");
        try {
            resource.getInputStream().transferTo(new FileOutputStream(tempFile));

            final long startTime = System.currentTimeMillis();
            final RegulationData data = xmlParser.parseXml(tempFile.getAbsolutePath());
            final long parseTime = System.currentTimeMillis() - startTime;

            log.info("Parsed {} clauses in {}ms", data.clauses().size(), parseTime);

            final Regulation regulation = mapToEntity(data);
            regulationRepo.save(regulation);

            final long totalTime = System.currentTimeMillis() - startTime;
            log.info("Loaded: {} {} ({} clauses) in {}ms",
                    data.code(), data.version(), data.clauses().size(), totalTime);
        } finally {
            tempFile.delete();
        }
    }

    /**
     * Corrected mapToEntity method.
     * This maps the DTOs from the parser to your JPA entities.
     */
    private Regulation mapToEntity(final RegulationData data) {
        final Regulation regulation = Regulation.builder()
                .code(data.code())
                .version(data.version())
                .name(data.name())
                .effectiveDate(data.effectiveDate())
                .active(true)
                .build();

        int order = 0;
        for (final ClauseData clauseData : data.clauses()) {
            final RegulationClause clause = RegulationClause.builder()
                    .clauseId(clauseData.clauseId())
                    .title(clauseData.title())
                    .content(clauseData.content())
                    .linkedTo(clauseData.linkedTo())
                    .clauseType(clauseData.type())
                    .displayOrder(clauseData.displayOrder() != null ? clauseData.displayOrder() : order)
                    .clauseNumber(order++)
                    .regulationVersion(data.version())
                    .build();

            regulation.addClause(clause);
        }

        return regulation;
    }

    /**
     * Load regulation from PDF file
     */
    private void loadFromPdf(final Resource resource) {
        try {
            final String filename = resource.getFilename();
            log.info("Loading from PDF: {}", filename);

            // Parse PDF
            final long startTime = System.currentTimeMillis();
            final RegulationData data;
            try (InputStream inputStream = resource.getInputStream()) {
                data = pdfParser.parsePdf(inputStream, filename);
            }
            final long parseTime = System.currentTimeMillis() - startTime;
            log.info("Parsed {} clauses from PDF in {}ms", data.clauses().size(), parseTime);

            // Check if regulation already exists (from XML or previous PDF)
            if (regulationRepo.existsByCodeAndVersion(data.code(), data.version())) {
                log.info("Regulation {} {} already exists, skipping PDF import", data.code(), data.version());
                return;
            }

            // Map to entity and save
            final Regulation regulation = mapToEntity(data);
            regulationRepo.save(regulation);

            final long totalTime = System.currentTimeMillis() - startTime;
            log.info("Loaded from PDF: {} {} ({} clauses) in {}ms",
                    data.code(), data.version(), data.clauses().size(), totalTime);

        } catch (final Exception e) {
            log.error("Failed to load PDF: {}", resource.getFilename(), e);
            // Continue with next file instead of failing completely
        }
    }
}
