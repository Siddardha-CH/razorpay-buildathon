package com.recoup.backend.dto;

import java.util.List;

import com.recoup.backend.model.AuditEntry;

public record AuditEntryDto(String stage, String detail, String timestamp, List<GuardrailCheckDto> guardrails) {
    public static AuditEntryDto from(AuditEntry e) {
        return new AuditEntryDto(e.getStage(), e.getDetail(), e.getTimestamp(),
            e.getGuardrails().stream().map(GuardrailCheckDto::from).toList());
    }
}
