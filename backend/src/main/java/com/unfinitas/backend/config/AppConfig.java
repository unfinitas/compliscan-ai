package com.unfinitas.backend.config;

import com.google.genai.Client;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class AppConfig {

    @Bean
    public Client geminiClient(@Value("${embedding.gemini.api-key:dump}") final String geminiKey) {
        return Client.builder()
                .apiKey(geminiKey)
                .build();
    }
}
