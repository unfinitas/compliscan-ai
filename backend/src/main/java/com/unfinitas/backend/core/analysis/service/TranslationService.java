package com.unfinitas.backend.core.analysis.service;

import com.deepl.api.DeepLException;
import com.deepl.api.TextResult;
import com.deepl.api.Translator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class TranslationService {

    private final Translator translator;
    private final Map<String, String> translationCache = new ConcurrentHashMap<>();
    @Getter
    private final boolean enabled;

    public TranslationService(
            @Value("${deepl.api.key:}") final String apiKey,
            @Value("${translation.enabled:false}") final boolean enabled) {

        this.enabled = enabled && apiKey != null && !apiKey.isBlank();

        if (this.enabled) {
            this.translator = new Translator(apiKey);
            log.info("DeepL Translation Service initialized");
        } else {
            this.translator = null;
            log.warn("Translation service DISABLED - set deepl.api.key to enable");
        }
    }

    /**
     * Detect language of text
     */
    public String detectLanguage(final String text) {
        if (!enabled || text == null || text.isBlank()) {
            return "en";
        }

        try {
            // Use DeepL's built-in language detection
            final var languages = translator.getSourceLanguages();
            // For simple detection, we'll let translateText auto-detect
            return null; // null means auto-detect in DeepL API
        } catch (final Exception e) {
            log.error("Language detection failed", e);
            return "en";
        }
    }

    /**
     * Translate text to English
     */
    public String translateToEnglish(final String text, final String sourceLang) {
        if (!enabled) {
            log.warn("Translation disabled, returning original text");
            return text;
        }

        if ("en".equalsIgnoreCase(sourceLang)) {
            return text;
        }

        // Check cache
        final String cacheKey = sourceLang + ":" + text;
        if (translationCache.containsKey(cacheKey)) {
            return translationCache.get(cacheKey);
        }

        try {
            final TextResult result = translator.translateText(
                    text,
                    sourceLang,
                    "en-US"
            );

            final String translated = result.getText();
            translationCache.put(cacheKey, translated);

            log.debug("Translated {} chars from {} to English", text.length(), sourceLang);
            return translated;

        } catch (final DeepLException | InterruptedException e) {
            log.error("Translation failed from {} to English", sourceLang, e);
            return text;
        }
    }

    /**
     * Translate if needed (auto-detect source language)
     */
    public TranslationResult translateIfNeeded(final String text) {
        if (!enabled || text == null || text.isBlank()) {
            return new TranslationResult(text, "en", false);
        }

        try {
            final TextResult result = translator.translateText(
                    text,
                    null, // Auto-detect source language
                    "en-US"
            );

            final String translated = result.getText();
            final String detectedLang = result.getDetectedSourceLanguage();
            final boolean wasTranslated = !detectedLang.equalsIgnoreCase("en");

            if (wasTranslated) {
                final String cacheKey = detectedLang + ":" + text;
                translationCache.put(cacheKey, translated);
            }

            return new TranslationResult(translated, detectedLang, wasTranslated);

        } catch (final DeepLException | InterruptedException e) {
            log.error("Auto-translate failed", e);
            return new TranslationResult(text, "en", false);
        }
    }

    public record TranslationResult(String text, String detectedLanguage, boolean wasTranslated) {
    }
}
