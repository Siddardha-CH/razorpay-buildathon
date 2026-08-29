package com.recoup.backend.dto;

public record CauseBreakdownDto(String category, int count, long atRiskAmountPaise, long recoveredAmountPaise) {
}
