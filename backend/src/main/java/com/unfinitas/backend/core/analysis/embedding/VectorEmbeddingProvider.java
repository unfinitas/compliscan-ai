package com.unfinitas.backend.core.analysis.embedding;

import java.util.List;
import java.util.Map;

public interface VectorEmbeddingProvider {
    List<Double> embed(String text);

    Map<String, List<Double>> embedBatch(List<String> texts);

    String model();
}
