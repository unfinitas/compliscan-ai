package com.unfinitas.backend.core.analysis.service;

import com.unfinitas.backend.core.analysis.embedding.VectorEmbeddingProvider;
import com.unfinitas.backend.core.ingestion.model.Paragraph;
import com.unfinitas.backend.core.ingestion.repository.ParagraphRepository;
import com.unfinitas.backend.core.regulation.model.RegulationClause;
import com.unfinitas.backend.core.regulation.repository.RegulationClauseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {
    private static final String CURRENT_MODEL = "gemini-embedding-001";
    private static final int BATCH_SIZE = 100;

    private final VectorEmbeddingProvider provider;
    private final ParagraphRepository paragraphRepository;
    private final RegulationClauseRepository clauseRepository;

    /**
     * Generate embeddings for paragraphs asynchronously during ingestion
     */
    @Async
    @Transactional
    public void generateParagraphEmbeddingsAsync(final List<Paragraph> paragraphs) {
        if (paragraphs == null || paragraphs.isEmpty()) {
            return;
        }

        log.info("Starting async embedding generation for {} paragraphs", paragraphs.size());

        final List<Paragraph> toEmbed = paragraphs.stream()
                .filter(p -> p.needsEmbedding(CURRENT_MODEL))
                .toList();

        if (toEmbed.isEmpty()) {
            log.info("All paragraphs already have embeddings");
            return;
        }

        processParagraphsInBatches(toEmbed);
        log.info("Completed embedding generation for {} paragraphs", toEmbed.size());
    }

    /**
     * Generate embeddings for regulation clauses asynchronously
     */
    @Async
    @Transactional
    public void generateClauseEmbeddingsAsync(final List<RegulationClause> clauses) {
        if (clauses == null || clauses.isEmpty()) {
            return;
        }

        log.info("Starting async embedding generation for {} clauses", clauses.size());

        final List<RegulationClause> toEmbed = clauses.stream()
                .filter(c -> c.needsEmbedding(CURRENT_MODEL))
                .toList();

        if (toEmbed.isEmpty()) {
            log.info("All clauses already have embeddings");
            return;
        }

        processClausesInBatches(toEmbed);
        log.info("Completed embedding generation for {} clauses", toEmbed.size());
    }

    /**
     * Scheduled job to generate missing embeddings (runs daily at 2 AM)
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void generateMissingEmbeddings() {
        log.info("Running scheduled embedding generation");

        // Process paragraphs without embeddings
        final List<Paragraph> paragraphsNeedingEmbedding = paragraphRepository.findByEmbeddingIsNull();
        if (!paragraphsNeedingEmbedding.isEmpty()) {
            log.info("Found {} paragraphs without embeddings", paragraphsNeedingEmbedding.size());
            processParagraphsInBatches(paragraphsNeedingEmbedding);
        }

        // Process clauses without embeddings
        final List<RegulationClause> clausesNeedingEmbedding = clauseRepository.findByEmbeddingIsNull();
        if (!clausesNeedingEmbedding.isEmpty()) {
            log.info("Found {} clauses without embeddings", clausesNeedingEmbedding.size());
            processClausesInBatches(clausesNeedingEmbedding);
        }

        log.info("Scheduled embedding generation complete");
    }

    private void processParagraphsInBatches(final List<Paragraph> paragraphs) {
        for (int i = 0; i < paragraphs.size(); i += BATCH_SIZE) {
            final int end = Math.min(i + BATCH_SIZE, paragraphs.size());
            final List<Paragraph> batch = paragraphs.subList(i, end);

            try {
                final Map<String, List<Double>> embeddings = provider.embedBatch(
                        batch.stream().map(Paragraph::getContent).toList()
                );

                for (final Paragraph p : batch) {
                    final List<Double> embedding = embeddings.get(p.getContent());
                    if (embedding != null && !embedding.isEmpty()) {
                        p.setEmbeddingFromArray(toFloatArray(embedding));
                        p.setEmbeddingModel(CURRENT_MODEL);
                        p.setEmbeddedAt(Instant.now());
                    }
                }

                paragraphRepository.saveAll(batch);
                log.debug("Processed batch {}-{} of {} paragraphs", i, end, paragraphs.size());

            } catch (final Exception ex) {
                log.error("Error processing paragraph batch {}-{}: {}", i, end, ex.getMessage(), ex);
            }
        }
    }

    private void processClausesInBatches(final List<RegulationClause> clauses) {
        for (int i = 0; i < clauses.size(); i += BATCH_SIZE) {
            final int end = Math.min(i + BATCH_SIZE, clauses.size());
            final List<RegulationClause> batch = clauses.subList(i, end);

            try {
                final Map<String, List<Double>> embeddings = provider.embedBatch(
                        batch.stream().map(RegulationClause::getContent).toList()
                );

                for (final RegulationClause c : batch) {
                    final List<Double> embedding = embeddings.get(c.getContent());
                    if (embedding != null && !embedding.isEmpty()) {
                        c.setEmbeddingFromArray(toFloatArray(embedding));
                        c.setEmbeddingModel(CURRENT_MODEL);
                        c.setEmbeddedAt(Instant.now());
                    }
                }

                clauseRepository.saveAll(batch);
                log.debug("Processed batch {}-{} of {} clauses", i, end, clauses.size());

            } catch (final Exception ex) {
                log.error("Error processing clause batch {}-{}: {}", i, end, ex.getMessage(), ex);
            }
        }
    }

    private float[] toFloatArray(final List<Double> doubles) {
        final float[] floats = new float[doubles.size()];
        for (int i = 0; i < doubles.size(); i++) {
            floats[i] = doubles.get(i).floatValue();
        }
        return floats;
    }

    /**
     * Get embedding statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getEmbeddingStats() {
        final long totalParagraphs = paragraphRepository.count();
        final long embeddedParagraphs = totalParagraphs - paragraphRepository.countByEmbeddingIsNull();

        final long totalClauses = clauseRepository.count();
        final long embeddedClauses = totalClauses - clauseRepository.countByEmbeddingIsNull();

        return Map.of(
                "model", CURRENT_MODEL,
                "paragraphs", Map.of(
                        "total", totalParagraphs,
                        "embedded", embeddedParagraphs,
                        "pending", totalParagraphs - embeddedParagraphs
                ),
                "clauses", Map.of(
                        "total", totalClauses,
                        "embedded", embeddedClauses,
                        "pending", totalClauses - embeddedClauses
                )
        );
    }
}
