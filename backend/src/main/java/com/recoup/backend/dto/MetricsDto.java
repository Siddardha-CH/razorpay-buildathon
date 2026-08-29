package com.recoup.backend.dto;

import java.util.List;

public record MetricsDto(
    String batchId, int totalCases,
    long atRiskAmountPaise, long recoveredAmountPaise, double recoveryRate,
    int recoveredCount, int pendingCount, int stoppedCount, int escalatedCount, int routedToRiskCount,
    long guardrailChecksEvaluated, long guardrailViolations, int stoppingRuleTriggers, int humanEscalations,
    List<CauseBreakdownDto> byCause, List<InterventionBreakdownDto> byIntervention
) {
}
