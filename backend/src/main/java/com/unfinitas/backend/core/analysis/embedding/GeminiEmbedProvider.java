package com.unfinitas.backend.core.analysis.embedding;

import com.google.genai.Client;
import com.google.genai.types.ContentEmbedding;
import com.google.genai.types.EmbedContentResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class GeminiEmbedProvider implements VectorEmbeddingProvider {
    private final Client client;
    private final String model;

    public GeminiEmbedProvider(final String apiKey, final String model) {
        this.client = Client.builder()
                .apiKey(apiKey)
                .build();
        this.model = model;
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    public List<Double> embed(final String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        final EmbedContentResponse response = client.models.embedContent(model, text, null);

        return response.embeddings()
                .flatMap(embeddings -> embeddings.isEmpty() ? Optional.empty() : Optional.of(embeddings.get(0)))
                .flatMap(ContentEmbedding::values)
                .orElse(Collections.emptyList())
                .stream()
                .map(Float::doubleValue)
                .toList();
    }

    @Override
    public Map<String, List<Double>> embedBatch(final List<String> texts) {
        final Map<String, List<Double>> out = new HashMap<>();
        for (final String t : texts) {
            if (t != null && !t.isBlank()) {
                out.put(t, embed(t));
            }
        }
        return out;
    }
}
