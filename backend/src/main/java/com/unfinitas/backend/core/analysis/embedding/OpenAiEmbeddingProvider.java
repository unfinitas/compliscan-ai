package com.unfinitas.backend.core.analysis.embedding;

import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class OpenAiEmbeddingProvider implements VectorEmbeddingProvider {

    private final OpenAiService openAiService;
    private final String model;

    public OpenAiEmbeddingProvider(final String apiKey, final String model) {
        this.openAiService = new OpenAiService(apiKey);
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

        final EmbeddingRequest req = EmbeddingRequest.builder()
                .model(model)
                .input(List.of(text))
                .build();

        return openAiService.createEmbeddings(req)
                .getData()
                .get(0)
                .getEmbedding();
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
