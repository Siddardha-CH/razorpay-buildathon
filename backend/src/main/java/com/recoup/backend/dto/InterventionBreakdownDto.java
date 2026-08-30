package com.recoup.backend.dto;

public record InterventionBreakdownDto(String intervention, int count, long atRiskAmountPaise, long costPaise, long recoveredAmountPaise) {
}
