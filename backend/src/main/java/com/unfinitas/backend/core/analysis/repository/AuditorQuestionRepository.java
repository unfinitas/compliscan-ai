package com.unfinitas.backend.core.analysis.repository;

import com.unfinitas.backend.core.analysis.model.AnalysisResult;
import com.unfinitas.backend.core.analysis.model.AuditorQuestion;
import com.unfinitas.backend.core.analysis.model.enums.QuestionPriority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditorQuestionRepository extends JpaRepository<AuditorQuestion, Long> {

    // Find all questions for an analysis
    List<AuditorQuestion> findByAnalysisResultOrderByPriorityDesc(AnalysisResult analysisResult);

    // Find by priority
    List<AuditorQuestion> findByAnalysisResultAndPriority(
            AnalysisResult analysisResult,
            QuestionPriority priority
    );

    // Find high priority questions
    @Query("SELECT q FROM AuditorQuestion q " +
            "WHERE q.analysisResult = :analysis " +
            "AND q.priority IN ('CRITICAL', 'HIGH') " +
            "ORDER BY q.priority DESC")
    List<AuditorQuestion> findHighPriorityQuestions(@Param("analysis") AnalysisResult analysis);

    // Find questions for specific clause
    List<AuditorQuestion> findByAnalysisResultAndClauseId(
            AnalysisResult analysisResult,
            String clauseId
    );

    // Count by priority
    long countByAnalysisResultAndPriority(AnalysisResult analysisResult, QuestionPriority priority);
}
