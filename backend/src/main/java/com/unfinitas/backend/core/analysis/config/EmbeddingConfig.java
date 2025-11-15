package com.unfinitas.backend.core.analysis.config;

import com.unfinitas.backend.core.analysis.embedding.GeminiEmbedProvider;
import com.unfinitas.backend.core.analysis.embedding.OpenAiEmbeddingProvider;
import com.unfinitas.backend.core.analysis.embedding.VectorEmbeddingProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingConfig {

    @Bean
    public VectorEmbeddingProvider embeddingProvider(
            @Value("${embedding.provider}") final String provider,
            @Value("${embedding.openai.api-key:}") final String openAiKey,
            @Value("${embedding.openai.model:text-embedding-3-small}") final String openAiModel,
            @Value("${embedding.gemini.api-key:}") final String geminiKey,
            @Value("${embedding.gemini.model:gemini-embedding-001}") final String geminiModel
    ) {

        return switch (provider.toLowerCase()) {
            case "openai" -> new OpenAiEmbeddingProvider(openAiKey, openAiModel);
            case "gemini" -> new GeminiEmbedProvider(geminiKey, geminiModel);
            default -> throw new IllegalArgumentException("Unknown embedding provider: " + provider);
        };
    }
}
