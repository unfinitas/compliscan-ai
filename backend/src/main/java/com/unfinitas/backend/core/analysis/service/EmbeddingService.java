package com.unfinitas.backend.core.analysis.service;

import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class EmbeddingService {

    private final OpenAiService openAiService;
    private final Map<String, List<Double>> embeddingCache = new ConcurrentHashMap<>();
    private static final String MODEL = "text-embedding-3-small";

    public EmbeddingService(@Value("${openai.api.key}") final String apiKey) {
        this.openAiService = new OpenAiService(apiKey);
        log.info("Initialized OpenAI Embedding Service with model: {}", MODEL);
    }

    /**
     * Get embedding for text (with cache)
     */
    public List<Double> embed(final String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        // Check cache
        final String cacheKey = text.trim();
        if (embeddingCache.containsKey(cacheKey)) {
            log.debug("Cache hit for text: {}...", text.substring(0, Math.min(50, text.length())));
            return embeddingCache.get(cacheKey);
        }

        // Generate embedding
        try {
            final List<Double> embedding = generateEmbedding(text);
            embeddingCache.put(cacheKey, embedding);
            log.debug("Generated and cached embedding for text: {}...",
                    text.substring(0, Math.min(50, text.length())));
            return embedding;

        } catch (final Exception e) {
            log.error("Failed to generate embedding", e);
            throw new RuntimeException("Embedding generation failed", e);
        }
    }

    /**
     * Batch embed multiple texts
     */
    public Map<String, List<Double>> batchEmbed(final List<String> texts) {
        final Map<String, List<Double>> results = new HashMap<>();

        for (final String text : texts) {
            if (text != null && !text.isBlank()) {
                results.put(text, embed(text));
            }
        }

        return results;
    }

    /**
     * Calculate cosine similarity between two embeddings
     */
    public double cosineSimilarity(final List<Double> vec1, final List<Double> vec2) {
        if (vec1.isEmpty() || vec2.isEmpty() || vec1.size() != vec2.size()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vec1.size(); i++) {
            dotProduct += vec1.get(i) * vec2.get(i);
            norm1 += vec1.get(i) * vec1.get(i);
            norm2 += vec2.get(i) * vec2.get(i);
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * Calculate similarity between two texts
     */
    public double calculateSimilarity(final String text1, final String text2) {
        final List<Double> emb1 = embed(text1);
        final List<Double> emb2 = embed(text2);
        return cosineSimilarity(emb1, emb2);
    }

    /**
     * Clear cache (for testing or memory management)
     */
    public void clearCache() {
        embeddingCache.clear();
        log.info("Embedding cache cleared");
    }

    /**
     * Get cache statistics
     */
    public Map<String, Object> getCacheStats() {
        return Map.of(
                "size", embeddingCache.size(),
                "model", MODEL
        );
    }

    private List<Double> generateEmbedding(final String text) {
        final EmbeddingRequest request = EmbeddingRequest.builder()
                .model(MODEL)
                .input(List.of(text))
                .build();

        return openAiService.createEmbeddings(request)
                .getData()
                .get(0)
                .getEmbedding();
    }
}
