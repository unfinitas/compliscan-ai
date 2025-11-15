package com.unfinitas.backend.core.llm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.*;
import com.unfinitas.backend.core.analysis.dto.MoeParagraphCandidate;
import com.unfinitas.backend.core.llm.dto.ComplianceResult;
import com.unfinitas.backend.core.llm.dto.RerankedParagraph;
import com.unfinitas.backend.core.regulation.model.RegulationClause;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmJudge {

    private static final Duration JUDGE_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration BATCH_TIMEOUT = Duration.ofSeconds(60);
    private static final int MAX_TEXT_LENGTH = 400;
    private static final int MAX_CANDIDATES = 2;

    private final Client gemini;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    private Schema schemaObject;     // schema for ONE result
    private Schema batchSchema;      // schema for ARRAY of results

    // ============================================================================
    // LOAD BOTH SCHEMAS
    // ============================================================================
    @PostConstruct
    public void loadSchema() {
        try {
            final ClassPathResource resource =
                    new ClassPathResource("schema/COMPLIANCE_RESULT_SCHEMA.json");

            final String schemaJson =
                    new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            // schema for *single* ComplianceResult
            this.schemaObject = Schema.fromJson(schemaJson);

            // schema for *array* of ComplianceResult
            final String schemaArrayJson = """
            {
              "type": "array",
              "items": %s
            }
            """.formatted(schemaJson);

            this.batchSchema = Schema.fromJson(schemaArrayJson);

            log.info("Loaded schemas: single-object + array wrapper");

        } catch (final Exception e) {
            log.error("FATAL: Cannot load compliance result schema", e);
            throw new IllegalStateException("Cannot start LlmJudge", e);
        }
    }

    // ============================================================================
    // SINGLE JUDGE
    // ============================================================================
    public ComplianceResult judge(final RegulationClause clause,
                                  final List<RerankedParagraph> reranked,
                                  final List<MoeParagraphCandidate> allCandidates) {
        try {
            return CompletableFuture.supplyAsync(
                    () -> performJudge(clause, reranked, allCandidates),
                    executor
            ).orTimeout(JUDGE_TIMEOUT.getSeconds(), TimeUnit.SECONDS).get();

        } catch (final Exception e) {
            log.error("Judge failed for {}", clause.getClauseId(), e);
            return ComplianceResult.empty();
        }
    }

    private ComplianceResult performJudge(final RegulationClause clause,
                                          final List<RerankedParagraph> reranked,
                                          final List<MoeParagraphCandidate> allCandidates) {
        try {
            final Map<Long, MoeParagraphCandidate> lookup =
                    allCandidates.stream().collect(
                            Collectors.toMap(MoeParagraphCandidate::paragraphId, c -> c)
                    );

            final List<Map<String, Object>> moe = new ArrayList<>();

            for (final RerankedParagraph r : reranked.stream().limit(MAX_CANDIDATES).toList()) {
                final MoeParagraphCandidate c = lookup.get(r.paragraphId());
                if (c == null) continue;

                moe.add(Map.of(
                        "paragraph_id", r.paragraphId(),
                        "full_text", limitText(c.text()),
                        "similarity_score", c.similarityScore(),
                        "rerank_score", r.relevanceScore()
                ));
            }

            final Map<String, Object> payload = Map.of(
                    "requirement", Map.of(
                            "id", clause.getClauseId(),
                            "text", limitText(clause.getContent())
                    ),
                    "moe_paragraphs", moe
            );

            final String jsonPayload = objectMapper.writeValueAsString(payload);

            final String prompt = """
                    EASA Part-145 compliance evaluation.

                    INPUT:
                    %s

                    RULE:
                    Output ONE ComplianceResult object matching schema.
                    requirement_id MUST equal requirement.id
                    """.formatted(jsonPayload);

            final GenerateContentConfig cfg = GenerateContentConfig.builder()
                    .responseMimeType("application/json")
                    .responseSchema(schemaObject)
                    .candidateCount(1)
                    .temperature(0.0f)
                    .maxOutputTokens(4000)
                    .build();

            final long start = System.currentTimeMillis();

            final GenerateContentResponse resp = gemini.models.generateContent(
                    "gemini-2.5-flash",
                    Content.fromParts(Part.fromText(prompt)),
                    cfg
            );

            final long elapsed = System.currentTimeMillis() - start;
            log.debug("Single judge LLM {}ms for {}", elapsed, clause.getClauseId());

            final String cleaned = cleanJson(resp);
            if (cleaned.isBlank()) return ComplianceResult.empty();

            final ComplianceResult out = objectMapper.readValue(cleaned, ComplianceResult.class);

            if (!clause.getClauseId().equals(out.requirement_id())) {
                return new ComplianceResult(
                        clause.getClauseId(),
                        out.evidence(),
                        out.compliance_status(),
                        out.justification(),
                        out.missing_elements(),
                        out.finding_level(),
                        out.recommended_actions()
                );
            }

            return out;

        } catch (final Exception e) {
            log.error("performJudge failed for {}", clause.getClauseId(), e);
            return ComplianceResult.empty();
        }
    }

    // ============================================================================
    // BATCH JUDGE
    // ============================================================================
    public Map<String, ComplianceResult> judgeBatch(final List<ClauseBatchInput> inputs) {
        if (inputs == null || inputs.isEmpty()) return Collections.emptyMap();

        try {
            return CompletableFuture.supplyAsync(
                    () -> performBatchJudge(inputs),
                    executor
            ).orTimeout(BATCH_TIMEOUT.getSeconds(), TimeUnit.SECONDS).get();

        } catch (final Exception e) {
            log.error("Batch judge failed for {}", inputs.size(), e);
            return Collections.emptyMap();
        }
    }

    private Map<String, ComplianceResult> performBatchJudge(final List<ClauseBatchInput> inputs) {
        try {
            final List<Map<String, Object>> reqs = new ArrayList<>();

            for (final ClauseBatchInput in : inputs) {
                final List<Map<String, Object>> moe = in.candidates().stream()
                        .limit(MAX_CANDIDATES)
                        .map(c -> Map.<String, Object>of(
                                "paragraph_id", c.paragraphId(),
                                "text", limitText(c.text()),
                                "score", c.similarityScore()
                        )).toList();

                reqs.add(Map.of(
                        "id", in.requirementId(),
                        "text", limitText(in.requirementText()),
                        "moe", moe
                ));
            }

            final String jsonPayload = objectMapper.writeValueAsString(Map.of("items", reqs));

            final String prompt = """
                    EASA Part-145 compliance evaluation.

                    INPUT:
                    %s

                    For EACH item:
                    - Output ONE ComplianceResult object.
                    - requirement_id MUST equal item.id

                    CRITICAL:
                    - Output MUST be a JSON ARRAY: [ {...}, ... ]
                    - evidence MUST be array of objects
                    - DO NOT return {"items": ...}
                    - DO NOT echo input
                    """.formatted(jsonPayload);

            final GenerateContentConfig cfg = GenerateContentConfig.builder()
                    .responseMimeType("application/json")
                    .responseSchema(batchSchema)   // ARRAY SCHEMA
                    .candidateCount(1)
                    .temperature(0.0f)
                    .maxOutputTokens(16000)
                    .build();

            final long start = System.currentTimeMillis();

            final GenerateContentResponse resp = gemini.models.generateContent(
                    "gemini-2.5-flash",
                    Content.fromParts(Part.fromText(prompt)),
                    cfg
            );

            final long elapsed = System.currentTimeMillis() - start;
            log.debug("Batch LLM {}ms for {} clauses", elapsed, inputs.size());

            final String cleaned = cleanJson(resp);
            if (!cleaned.startsWith("[")) {
                log.error("Batch returned non-array: {}", cleaned);
                return Collections.emptyMap();
            }

            final List<ComplianceResult> results =
                    objectMapper.readValue(cleaned, new TypeReference<>() {});

            final Map<String, ComplianceResult> out = new HashMap<>();
            final Set<String> validIds = inputs.stream()
                    .map(ClauseBatchInput::requirementId)
                    .collect(Collectors.toSet());

            for (final ComplianceResult r : results) {
                if (r.requirement_id() != null && validIds.contains(r.requirement_id())) {
                    out.put(r.requirement_id(), r);
                }
            }

            return out;

        } catch (final Exception e) {
            log.error("performBatchJudge failed", e);
            return Collections.emptyMap();
        }
    }

    // ============================================================================
    // JSON CLEANUP UTILITIES
    // ============================================================================
    private String limitText(String t) {
        if (t == null) return "";
        t = t.trim();
        return t.length() > MAX_TEXT_LENGTH ?
                t.substring(0, MAX_TEXT_LENGTH) + "..." : t;
    }

    private String cleanJson(final GenerateContentResponse response) {
        final String raw = response.text();
        if (raw != null && !raw.isBlank()) return extractJson(raw);

        final StringBuilder sb = new StringBuilder();
        for (final Candidate c : response.candidates().orElse(List.of())) {
            if (c.content().isEmpty()) continue;
            for (final Part p : c.content().get().parts().orElse(List.of())) {
                p.text().ifPresent(sb::append);
            }
        }
        return extractJson(sb.toString());
    }

    private String extractJson(final String raw) {
        if (raw == null) return "";
        final String cleaned = raw.replace("```json", "")
                .replace("```", "")
                .replace("`", "")
                .trim();

        final int a = cleaned.indexOf('[');
        final int b = cleaned.lastIndexOf(']');
        if (a >= 0 && b > a) return cleaned.substring(a, b + 1);

        final int o = cleaned.indexOf('{');
        final int c = cleaned.lastIndexOf('}');
        if (o >= 0 && c > o) return cleaned.substring(o, c + 1);

        return "";
    }

    public record ClauseBatchInput(
            String requirementId,
            String requirementText,
            List<MoeParagraphCandidate> candidates
    ) {}
}
