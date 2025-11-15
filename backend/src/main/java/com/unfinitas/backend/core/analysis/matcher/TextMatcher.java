package com.unfinitas.backend.core.analysis.matcher;

import com.unfinitas.backend.core.analysis.service.EmbeddingService;
import com.unfinitas.backend.core.analysis.service.TranslationService;
import com.unfinitas.backend.core.ingestion.model.Paragraph;
import com.unfinitas.backend.core.regulation.model.RegulationClause;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TextMatcher {
    private static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors();

    @SuppressWarnings("unused")
    private final TranslationService translationService;
    private final EmbeddingService embeddingService;
    private final ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

    private double fastCosineSimilarity(final float[] a, final float[] b) {
        if (a == null || b == null || a.length != b.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        final int len = a.length;
        int i = 0;

        // Unroll by 8 for maximum CPU cache efficiency
        for (; i <= len - 8; i += 8) {
            final float a0 = a[i], a1 = a[i + 1], a2 = a[i + 2], a3 = a[i + 3];
            final float a4 = a[i + 4], a5 = a[i + 5], a6 = a[i + 6], a7 = a[i + 7];
            final float b0 = b[i], b1 = b[i + 1], b2 = b[i + 2], b3 = b[i + 3];
            final float b4 = b[i + 4], b5 = b[i + 5], b6 = b[i + 6], b7 = b[i + 7];

            dotProduct += a0 * b0 + a1 * b1 + a2 * b2 + a3 * b3 + a4 * b4 + a5 * b5 + a6 * b6 + a7 * b7;
            normA += a0 * a0 + a1 * a1 + a2 * a2 + a3 * a3 + a4 * a4 + a5 * a5 + a6 * a6 + a7 * a7;
            normB += b0 * b0 + b1 * b1 + b2 * b2 + b3 * b3 + b4 * b4 + b5 * b5 + b6 * b6 + b7 * b7;
        }

        // Handle remaining elements
        for (; i < len; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public Map<UUID, List<ParagraphMatchResult>> batchFindMatches(
            final List<RegulationClause> clauses,
            final List<Paragraph> paragraphs,
            final double threshold) {

        final long startTime = System.currentTimeMillis();

        log.info("Starting PARALLEL batch processing: {} clauses × {} paragraphs on {} threads",
                clauses.size(), paragraphs.size(), THREAD_COUNT);

        // Pre-filter and convert to arrays once
        final List<ClauseWithEmbedding> validClauses = clauses.stream()
                .filter(c -> c.getEmbedding() != null)
                .map(c -> new ClauseWithEmbedding(c.getId(), c.getClauseId(), c.getEmbeddingArray()))
                .filter(c -> c.embedding != null)
                .toList();

        final List<ParagraphWithEmbedding> validParagraphs = paragraphs.stream()
                .filter(p -> p.getEmbedding() != null)
                .map(p -> new ParagraphWithEmbedding(p, p.getEmbeddingArray()))
                .filter(p -> p.embedding != null)
                .toList();

        log.info("Valid items: {} clauses, {} paragraphs (Total: {} comparisons)",
                validClauses.size(), validParagraphs.size(),
                validClauses.size() * validParagraphs.size());

        // Process in parallel
        final ConcurrentHashMap<UUID, List<ParagraphMatchResult>> results = new ConcurrentHashMap<>();

        final List<CompletableFuture<Void>> futures = validClauses.stream()
                .map(clause -> CompletableFuture.runAsync(() -> {
                    processClause(clause, validParagraphs, threshold, results);
                }, executor))
                .toList();

        // Wait for completion
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        final long duration = System.currentTimeMillis() - startTime;
        final long totalComparisons = (long) validClauses.size() * validParagraphs.size();

        log.info("PARALLEL processing completed in {}ms ({} comparisons, {} comp/sec)",
                duration, totalComparisons, (totalComparisons * 1000L) / Math.max(duration, 1));

        return results;
    }

    private void processClause(
            final ClauseWithEmbedding clause,
            final List<ParagraphWithEmbedding> paragraphs,
            final double threshold,
            final ConcurrentHashMap<UUID, List<ParagraphMatchResult>> results) {

        final List<ParagraphMatchResult> matches = new ArrayList<>();

        for (final ParagraphWithEmbedding p : paragraphs) {
            final double similarity = fastCosineSimilarity(clause.embedding, p.embedding);

            if (similarity >= threshold) {
                matches.add(new ParagraphMatchResult(p.paragraph, similarity));
            }
        }

        // Sort by similarity descending
        matches.sort((a, b) -> Double.compare(b.similarity(), a.similarity()));

        results.put(clause.id, matches);
    }

    private record ClauseWithEmbedding(UUID id, String clauseId, float[] embedding) {
    }

    private record ParagraphWithEmbedding(Paragraph paragraph, float[] embedding) {
    }

    public record ParagraphMatchResult(Paragraph paragraph, double similarity) {
    }
}
