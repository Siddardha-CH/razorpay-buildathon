package com.recoup.backend.dto;

import java.util.List;

public record ModelStatusDto(boolean trained, int trainingSamples, List<CategoryProbabilityDto> byCategory) {
}
