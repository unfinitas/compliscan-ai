package com.unfinitas.backend.core.analysis.service;

import com.unfinitas.backend.core.analysis.embedding.VectorEmbeddingProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class EmbeddingService {

    private final VectorEmbeddingProvider provider;
    private final Map<String, List<Double>> cache = new ConcurrentHashMap<>();

    public EmbeddingService(final VectorEmbeddingProvider provider) {
        this.provider = provider;
        log.info("EmbeddingService initialized with provider model: {}", provider.model());
    }

    public List<Double> embed(final String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        return cache.computeIfAbsent(text.trim(), provider::embed);
    }

    public Map<String, List<Double>> batchEmbed(final List<String> texts) {
        final Map<String, List<Double>> result = new HashMap<>();
        for (final String t : texts) {
            if (t != null && !t.isBlank()) {
                result.put(t, embed(t));
            }
        }
        return result;
    }

    public double cosineSimilarity(final List<Double> v1, final List<Double> v2) {
        if (v1.isEmpty() || v2.isEmpty() || v1.size() != v2.size()) {
            return 0.0;
        }

        double dot = 0.0;
        double n1 = 0.0;
        double n2 = 0.0;

        for (int i = 0; i < v1.size(); i++) {
            dot += v1.get(i) * v2.get(i);
            n1 += v1.get(i) * v1.get(i);
            n2 += v2.get(i) * v2.get(i);
        }

        return dot / (Math.sqrt(n1) * Math.sqrt(n2));
    }

    public double calculateSimilarity(final String a, final String b) {
        return cosineSimilarity(embed(a), embed(b));
    }

    public void clearCache() {
        cache.clear();
    }

    public Map<String, Object> cacheStats() {
        return Map.of(
                "size", cache.size(),
                "providerModel", provider.model()
        );
    }
}
