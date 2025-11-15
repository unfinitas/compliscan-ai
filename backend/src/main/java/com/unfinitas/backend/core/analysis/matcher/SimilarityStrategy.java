package com.unfinitas.backend.core.analysis.matcher;

public interface SimilarityStrategy {
    double calculateSimilarity(String text1, String text2);

    String getName();
}
