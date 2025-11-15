package com.unfinitas.backend.api.controller;

import com.unfinitas.backend.core.analysis.model.enums.AnalysisStatus;
import com.unfinitas.backend.core.analysis.repository.AnalysisResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class HealthController {

    private final AnalysisResultRepository analysisRepo;

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        try {
            // Test database connectivity
            final long totalAnalyses = analysisRepo.count();

            return ResponseEntity.ok(Map.of(
                    "status", "UP",
                    "timestamp", LocalDateTime.now(),
                    "totalAnalyses", totalAnalyses,
                    "service", "CompliScan-AI Analysis Engine"
            ));

        } catch (final Exception e) {
            log.error("Health check failed", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "status", "DOWN",
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        final LocalDateTime since = LocalDateTime.now().minusDays(30);

        final long total = analysisRepo.count();
        final long recent = analysisRepo.countByStatusSince(AnalysisStatus.COMPLETED, since);
        final Double avgScore = analysisRepo.getAverageComplianceScoreSince(since);

        return ResponseEntity.ok(Map.of(
                "totalAnalyses", total,
                "last30Days", recent,
                "averageComplianceScore", avgScore != null ? avgScore : 0.0,
                "period", "last 30 days"
        ));
    }
}
