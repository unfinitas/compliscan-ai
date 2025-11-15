package com.unfinitas.backend.core.analysis.embedding;

import com.google.genai.Client;
import com.google.genai.types.ContentEmbedding;
import com.google.genai.types.EmbedContentConfig;
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

        try {
            final EmbedContentResponse response =
                    client.models.embedContent(model,
                            text,
                            EmbedContentConfig.builder()
                                    .taskType("CLUSTERING")
                                    .build()
                    );

            final Optional<List<ContentEmbedding>> embeddingsOpt = response.embeddings();
            if (embeddingsOpt.isEmpty() || embeddingsOpt.get().isEmpty()) {
                return Collections.emptyList();
            }

            final ContentEmbedding embedding = embeddingsOpt.get().get(0);

            final Optional<List<Float>> values = embedding.values();
            return values.map(floats -> floats.stream()
                    .map(Number::doubleValue)
                    .toList()).orElse(Collections.emptyList());

        } catch (final Exception ex) {
            log.error("Embedding failed for text: {}", ex.getMessage(), ex);
            return Collections.emptyList();
        }
    }

    @Override
    public Map<String, List<Double>> embedBatch(final List<String> texts) {
        final Map<String, List<Double>> out = new LinkedHashMap<>();

        for (final String txt : texts) {
            if (txt == null || txt.isBlank()) {
                out.put(txt, Collections.emptyList());
                continue;
            }
            out.put(txt, embed(txt));
        }

        return out;
    }
}
