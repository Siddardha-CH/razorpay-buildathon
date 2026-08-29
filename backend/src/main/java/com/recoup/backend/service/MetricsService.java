package com.recoup.backend.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.recoup.backend.dto.CauseBreakdownDto;
import com.recoup.backend.dto.InterventionBreakdownDto;
import com.recoup.backend.dto.MetricsDto;
import com.recoup.backend.model.AuditEntry;
import com.recoup.backend.model.CaseStatus;
import com.recoup.backend.model.InterventionType;
import com.recoup.backend.model.RecoveryCase;
import com.recoup.backend.repository.AuditEntryRepository;
import com.recoup.backend.repository.RecoveryCaseRepository;

/** Aggregates the headline KPIs the submission bar asks for: money recovered
 *  across the batch, an honest breakdown by cause/intervention, and a
 *  guardrail-violation count that should always read zero. */
@Service
public class MetricsService {

    private final RecoveryCaseRepository caseRepository;
    private final AuditEntryRepository auditEntryRepository;

    public MetricsService(RecoveryCaseRepository caseRepository, AuditEntryRepository auditEntryRepository) {
        this.caseRepository = caseRepository;
        this.auditEntryRepository = auditEntryRepository;
    }

    @Transactional(readOnly = true)
    public MetricsDto forBatch(String batchId) {
        List<RecoveryCase> cases = caseRepository.findByBatchId(batchId);
        if (cases.isEmpty()) {
            return new MetricsDto(batchId, 0, 0, 0, 0.0, 0, 0, 0, 0, 0, 0, 0, 0, 0, List.of(), List.of());
        }

        long atRisk = cases.stream().mapToLong(c -> c.getEvent().getAmountPaise()).sum();
        long recovered = cases.stream().mapToLong(c -> c.getActionResult().getRecoveredAmountPaise()).sum();

        Map<CaseStatus, Long> byStatus = cases.stream()
            .collect(Collectors.groupingBy(RecoveryCase::getStatus, Collectors.counting()));

        List<AuditEntry> auditEntries = auditEntryRepository.findByCaseIdIn(cases.stream().map(RecoveryCase::getId).toList());
        long guardrailChecks = auditEntries.stream().mapToLong(e -> e.getGuardrails().size()).sum();
        long guardrailViolations = auditEntries.stream()
            .flatMap(e -> e.getGuardrails().stream())
            .filter(g -> !g.isPassed())
            .count();

        int stoppingRuleTriggers = (int) cases.stream()
            .filter(c -> c.getDecision().getIntervention() == InterventionType.STOP_PURSUIT)
            .count();
        int humanEscalations = (int) cases.stream()
            .filter(c -> c.getDecision().getIntervention() == InterventionType.HUMAN_ESCALATION)
            .count();

        List<CauseBreakdownDto> byCause = cases.stream()
            .collect(Collectors.groupingBy(c -> c.getDiagnosis().getCategory()))
            .entrySet().stream()
            .map(entry -> new CauseBreakdownDto(
                entry.getKey().name(),
                entry.getValue().size(),
                entry.getValue().stream().mapToLong(c -> c.getEvent().getAmountPaise()).sum(),
                entry.getValue().stream().mapToLong(c -> c.getActionResult().getRecoveredAmountPaise()).sum()
            ))
            .sorted((a, b) -> Long.compare(b.atRiskAmountPaise(), a.atRiskAmountPaise()))
            .toList();

        List<InterventionBreakdownDto> byIntervention = cases.stream()
            .collect(Collectors.groupingBy(c -> c.getDecision().getIntervention()))
            .entrySet().stream()
            .map(entry -> new InterventionBreakdownDto(
                entry.getKey().name(),
                entry.getValue().size(),
                entry.getValue().stream().mapToLong(c -> c.getDecision().getEstimatedCostPaise()).sum(),
                entry.getValue().stream().mapToLong(c -> c.getActionResult().getRecoveredAmountPaise()).sum()
            ))
            .sorted((a, b) -> Long.compare(b.recoveredAmountPaise(), a.recoveredAmountPaise()))
            .toList();

        return new MetricsDto(
            batchId, cases.size(), atRisk, recovered, atRisk == 0 ? 0.0 : (double) recovered / atRisk,
            byStatus.getOrDefault(CaseStatus.RECOVERED, 0L).intValue(),
            byStatus.getOrDefault(CaseStatus.PENDING, 0L).intValue(),
            byStatus.getOrDefault(CaseStatus.STOPPED, 0L).intValue(),
            byStatus.getOrDefault(CaseStatus.ESCALATED, 0L).intValue(),
            byStatus.getOrDefault(CaseStatus.ROUTED_TO_RISK, 0L).intValue(),
            guardrailChecks, guardrailViolations, stoppingRuleTriggers, humanEscalations,
            byCause, byIntervention
        );
    }
}
