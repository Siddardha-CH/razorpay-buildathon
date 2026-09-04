package com.recoup.backend.dto;

public record CategoryProbabilityDto(String category, double assumedProbability, double currentEstimate) {
}
