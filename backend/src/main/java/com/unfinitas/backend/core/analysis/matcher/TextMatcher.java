package com.unfinitas.backend.core.analysis.matcher;

import com.unfinitas.backend.core.analysis.service.EmbeddingService;
import com.unfinitas.backend.core.analysis.service.TranslationService;
import com.unfinitas.backend.core.ingestion.model.Paragraph;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TextMatcher {

    private final EmbeddingService embeddingService;
    private final TranslationService translationService;

    /**
     * Calculate semantic similarity between two texts
     * Handles translation if needed
     */
    public double calculateSimilarity(final String text1, final String text2) {
        if (text1 == null || text2 == null || text1.isBlank() || text2.isBlank()) {
            return 0.0;
        }

        // Translate if needed
        final TranslationService.TranslationResult tr1 = translationService.translateIfNeeded(text1);
        final TranslationService.TranslationResult tr2 = translationService.translateIfNeeded(text2);

        if (tr1.wasTranslated() || tr2.wasTranslated()) {
            log.debug("Translated text before similarity calculation");
        }

        // Calculate embedding similarity
        return embeddingService.calculateSimilarity(tr1.text(), tr2.text());
    }

    /**
     * Find best matching paragraph for a clause
     */
    public ParagraphMatchResult findBestMatch(final String clauseText, final List<Paragraph> paragraphs) {
        if (paragraphs == null || paragraphs.isEmpty()) {
            return new ParagraphMatchResult(null, 0.0);
        }

        Paragraph bestParagraph = null;
        double bestSimilarity = 0.0;

        for (final Paragraph paragraph : paragraphs) {
            final double similarity = calculateSimilarity(clauseText, paragraph.getContent());

            if (similarity > bestSimilarity) {
                bestSimilarity = similarity;
                bestParagraph = paragraph;
            }
        }

        return new ParagraphMatchResult(bestParagraph, bestSimilarity);
    }

    /**
     * Find all paragraphs above similarity threshold
     */
    public List<ParagraphMatchResult> findAllMatches(
            final String clauseText,
            final List<Paragraph> paragraphs,
            final double threshold) {

        return paragraphs.stream()
                .map(p -> new ParagraphMatchResult(
                        p,
                        calculateSimilarity(clauseText, p.getContent())
                ))
                .filter(r -> r.similarity() >= threshold)
                .sorted((a, b) -> Double.compare(b.similarity(), a.similarity()))
                .toList();
    }

    public record ParagraphMatchResult(Paragraph paragraph, double similarity) {}
}
