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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {
    private static final String CURRENT_MODEL = "gemini-embedding-001";
    private static final int BATCH_SIZE = 100; // Match provider batch size
    private static final int PARALLEL_BATCHES = 4; // Process 4 batches in parallel

    private final VectorEmbeddingProvider provider;
    private final ParagraphRepository paragraphRepository;
    private final RegulationClauseRepository clauseRepository;

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

        processParagraphsInParallel(toEmbed);
        log.info("Completed embedding generation for {} paragraphs", toEmbed.size());
    }

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

        processClausesInParallel(toEmbed);
        log.info("Completed embedding generation for {} clauses", toEmbed.size());
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void generateMissingEmbeddings() {
        log.info("Running scheduled embedding generation");

        final List<Paragraph> paragraphsNeedingEmbedding = paragraphRepository.findByEmbeddingIsNull();
        if (!paragraphsNeedingEmbedding.isEmpty()) {
            log.info("Found {} paragraphs without embeddings", paragraphsNeedingEmbedding.size());
            processParagraphsInParallel(paragraphsNeedingEmbedding);
        }

        final List<RegulationClause> clausesNeedingEmbedding = clauseRepository.findByEmbeddingIsNull();
        if (!clausesNeedingEmbedding.isEmpty()) {
            log.info("Found {} clauses without embeddings", clausesNeedingEmbedding.size());
            processClausesInParallel(clausesNeedingEmbedding);
        }

        log.info("Scheduled embedding generation complete");
    }

    private void processParagraphsInParallel(final List<Paragraph> paragraphs) {
        final long startTime = System.currentTimeMillis();

        // Split into super-batches for parallel processing
        final List<List<Paragraph>> superBatches = partitionList(paragraphs, BATCH_SIZE);

        // Process PARALLEL_BATCHES at a time
        for (int i = 0; i < superBatches.size(); i += PARALLEL_BATCHES) {
            final int end = Math.min(i + PARALLEL_BATCHES, superBatches.size());
            final List<CompletableFuture<Void>> futures = superBatches.subList(i, end).stream()
                    .map(batch -> CompletableFuture.runAsync(() ->
                            processParagraphBatch(batch)))
                    .toList();

            // Wait for this round to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            log.info("Processed {}/{} batches", end, superBatches.size());
        }

        final long elapsed = System.currentTimeMillis() - startTime;
        log.info("Embedded {} paragraphs in {}ms ({}ms/paragraph)",
                paragraphs.size(), elapsed, elapsed / paragraphs.size());
    }

    private void processClausesInParallel(final List<RegulationClause> clauses) {
        final long startTime = System.currentTimeMillis();

        final List<List<RegulationClause>> superBatches = partitionList(clauses, BATCH_SIZE);

        for (int i = 0; i < superBatches.size(); i += PARALLEL_BATCHES) {
            final int end = Math.min(i + PARALLEL_BATCHES, superBatches.size());
            final List<CompletableFuture<Void>> futures = superBatches.subList(i, end).stream()
                    .map(batch -> CompletableFuture.runAsync(() ->
                            processClauseBatch(batch)))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            log.info("Processed {}/{} batches", end, superBatches.size());
        }

        final long elapsed = System.currentTimeMillis() - startTime;
        log.info("Embedded {} clauses in {}ms ({}ms/clause)",
                clauses.size(), elapsed, elapsed / clauses.size());
    }

    private void processParagraphBatch(final List<Paragraph> batch) {
        try {
            final List<String> texts = batch.stream()
                    .map(Paragraph::getContent)
                    .collect(Collectors.toList());

            final Map<String, List<Double>> embeddings = provider.embedBatch(texts);

            for (final Paragraph p : batch) {
                final List<Double> embedding = embeddings.get(p.getContent());
                if (embedding != null && !embedding.isEmpty()) {
                    p.setEmbeddingFromArray(toFloatArray(embedding));
                    p.setEmbeddingModel(CURRENT_MODEL);
                    p.setEmbeddedAt(Instant.now());
                }
            }

            paragraphRepository.saveAll(batch);

        } catch (final Exception ex) {
            log.error("Error processing paragraph batch: {}", ex.getMessage(), ex);
        }
    }

    private void processClauseBatch(final List<RegulationClause> batch) {
        try {
            final List<String> texts = batch.stream()
                    .map(RegulationClause::getContent)
                    .collect(Collectors.toList());

            final Map<String, List<Double>> embeddings = provider.embedBatch(texts);

            for (final RegulationClause c : batch) {
                final List<Double> embedding = embeddings.get(c.getContent());
                if (embedding != null && !embedding.isEmpty()) {
                    c.setEmbeddingFromArray(toFloatArray(embedding));
                    c.setEmbeddingModel(CURRENT_MODEL);
                    c.setEmbeddedAt(Instant.now());
                }
            }

            clauseRepository.saveAll(batch);

        } catch (final Exception ex) {
            log.error("Error processing clause batch: {}", ex.getMessage(), ex);
        }
    }

    private <T> List<List<T>> partitionList(final List<T> list, final int size) {
        return new ArrayList<>(list.stream()
                .collect(Collectors.groupingBy(item -> list.indexOf(item) / size))
                .values());
    }

    private float[] toFloatArray(final List<Double> doubles) {
        final float[] floats = new float[doubles.size()];
        for (int i = 0; i < doubles.size(); i++) {
            floats[i] = doubles.get(i).floatValue();
        }
        return floats;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getEmbeddingStats() {
        final long totalParagraphs = paragraphRepository.count();
        final long embeddedParagraphs = totalParagraphs - paragraphRepository.countByEmbeddingIsNull();

        final long totalClauses = clauseRepository.count();
        final long embeddedClauses = totalClauses - clauseRepository.countByEmbeddingIsNull();

        return Map.of(
                "model", CURRENT_MODEL,
                "dimension", 768,
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
