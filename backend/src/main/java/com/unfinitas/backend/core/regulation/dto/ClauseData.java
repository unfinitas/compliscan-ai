package com.unfinitas.backend.core.regulation.dto;

public record ClauseData(
        String clauseId,
        String title,
        String content,
        String linkedTo,
        String type,
        Integer displayOrder
) {
}
