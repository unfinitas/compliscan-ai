package com.unfinitas.backend.core.regulation.dto;

import java.time.LocalDate;
import java.util.List;

public record RegulationData(
        String code,
        String version,
        String name,
        LocalDate effectiveDate,
        List<ClauseData> clauses
) {
}
