package com.recoup.backend.dto;

import com.recoup.backend.model.RecoveryCase;

public record CaseSummaryDto(
    Long id, String eventId, String eventType, String customerName, long amountPaise,
    String category, String intervention, String status, long recoveredAmountPaise, String processedAt
) {
    public static CaseSummaryDto from(RecoveryCase c) {
        return new CaseSummaryDto(
            c.getId(), c.getEvent().getId(), c.getEvent().getType().name(), c.getEvent().getCustomerName(),
            c.getEvent().getAmountPaise(), c.getDiagnosis().getCategory().name(),
            c.getDecision().getIntervention().name(), c.getStatus().name(),
            c.getActionResult().getRecoveredAmountPaise(), c.getProcessedAt()
        );
    }
}
