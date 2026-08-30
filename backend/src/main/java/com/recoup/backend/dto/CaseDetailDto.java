package com.recoup.backend.dto;

import java.util.List;

import com.recoup.backend.model.RecoveryCase;

public record CaseDetailDto(
    Long id, String eventId, String eventType, String customerName, String customerPhone,
    long amountPaise, boolean b2b, int daysOverdue,
    String diagnosisCategory, double diagnosisConfidence, String diagnosisReasoning, String diagnosisSource,
    String intervention, String decisionReasoning, long estimatedCostPaise, double estimatedRecoveryProbability, String scheduledFor,
    boolean actionSuccess, String actionDetail, long recoveredAmountPaise, String providerRef,
    boolean hasPromise, long promisedAmountPaise, String promisedByDate, Boolean promiseKept,
    String status, String processedAt, List<AuditEntryDto> auditTrail
) {
    public static CaseDetailDto from(RecoveryCase c, List<AuditEntryDto> auditTrail) {
        return new CaseDetailDto(
            c.getId(), c.getEvent().getId(), c.getEvent().getType().name(), c.getEvent().getCustomerName(),
            c.getEvent().getContact().getPhone(), c.getEvent().getAmountPaise(), c.getEvent().isB2b(), c.getEvent().getDaysOverdue(),
            c.getDiagnosis().getCategory().name(), c.getDiagnosis().getConfidence(), c.getDiagnosis().getReasoning(), c.getDiagnosis().getSource(),
            c.getDecision().getIntervention().name(), c.getDecision().getReasoning(), c.getDecision().getEstimatedCostPaise(),
            c.getDecision().getEstimatedRecoveryProbability(), c.getDecision().getScheduledFor(),
            c.getActionResult().isSuccess(), c.getActionResult().getDetail(), c.getActionResult().getRecoveredAmountPaise(), c.getActionResult().getProviderRef(),
            c.getPromise().exists(), c.getPromise().getPromisedAmountPaise(), c.getPromise().getPromisedByDate(), c.getPromise().getKept(),
            c.getStatus().name(), c.getProcessedAt(), auditTrail
        );
    }
}
