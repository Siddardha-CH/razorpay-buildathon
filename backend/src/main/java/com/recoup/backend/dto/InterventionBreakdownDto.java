package com.recoup.backend.dto;

public record InterventionBreakdownDto(String intervention, int count, long costPaise, long recoveredAmountPaise) {
}
