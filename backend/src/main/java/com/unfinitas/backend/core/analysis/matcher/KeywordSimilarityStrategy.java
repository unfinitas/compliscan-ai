package com.unfinitas.backend.core.analysis.matcher;


import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component("keywordMatcher")
public class KeywordSimilarityStrategy implements SimilarityStrategy {

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for"
    );

    @Override
    public double calculateSimilarity(final String text1, final String text2) {
        final Set<String> tokens1 = tokenize(text1);
        final Set<String> tokens2 = tokenize(text2);

        final Set<String> intersection = new HashSet<>(tokens1);
        intersection.retainAll(tokens2);

        final Set<String> union = new HashSet<>(tokens1);
        union.addAll(tokens2);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    @Override
    public String getName() {
        return "Keyword Matching (Jaccard)";
    }

    private Set<String> tokenize(final String text) {
        return Arrays.stream(text.toLowerCase()
                        .replaceAll("[^a-z0-9\\s]", " ")
                        .split("\\s+"))
                .filter(w -> w.length() > 2)
                .filter(w -> !STOP_WORDS.contains(w))
                .collect(Collectors.toSet());
    }
}
