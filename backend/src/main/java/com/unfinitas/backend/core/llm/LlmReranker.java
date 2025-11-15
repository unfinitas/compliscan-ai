package com.unfinitas.backend.core.llm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.unfinitas.backend.core.analysis.dto.MoeParagraphCandidate;
import com.unfinitas.backend.core.llm.dto.RerankedParagraph;
import com.unfinitas.backend.core.regulation.model.RegulationClause;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LlmReranker {

    private static final double SIMILARITY_THRESHOLD = 0.6;
    private static final String MODEL = "gemini-2.5-flash";
    private final Client gemini;

    public List<RerankedParagraph> rerank(
            final RegulationClause clause,
            final List<MoeParagraphCandidate> candidates,
            final int topN
    ) {
        // Filter by similarity before LLM
        final List<MoeParagraphCandidate> filtered = candidates.stream()
                .filter(c -> c.similarityScore() >= SIMILARITY_THRESHOLD)
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            return List.of();
        }

        final String prompt = buildPrompt(clause, filtered);

        final var response = gemini.models.generateContent(
                MODEL,
                Content.fromParts(Part.fromText(prompt)),
                null
        );

        try {
            String raw = response.text();
            if (raw == null) raw = "[]";

            final String cleaned = raw
                    .replace("```json", "")
                    .replace("```", "")
                    .replace("`", "")
                    .trim();

            final List<RerankedParagraph> ranked = new ObjectMapper()
                    .readValue(cleaned, new TypeReference<>() {
                    });

            return ranked.stream().limit(topN).collect(Collectors.toList());

        } catch (final Exception e) {
            return List.of();
        }
    }

    private String buildPrompt(
            final RegulationClause clause,
            final List<MoeParagraphCandidate> candidates
    ) {
        final StringBuilder sb = new StringBuilder();

        sb.append("You are a relevance ranker for aviation regulation.\n\n");
        sb.append("Requirement:\n").append(clause.getContent()).append("\n\n");
        sb.append("Candidates:\n");

        for (final MoeParagraphCandidate c : candidates) {
            sb.append("- ID ").append(c.paragraphId())
                    .append(": ").append(c.text()).append("\n\n");
        }

        sb.append("""
                Task:
                Rank candidates by how relevant they are to the requirement.
                
                Output strictly valid JSON array:
                [
                  {"paragraphId": 123, "relevanceScore": 0.92},
                  {"paragraphId": 456, "relevanceScore": 0.71}
                ]
                
                Rules:
                - relevanceScore must be between 0 and 1
                - Do not include extra fields
                - Output only JSON
                """);

        return sb.toString();
    }
}
