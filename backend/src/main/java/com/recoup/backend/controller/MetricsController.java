package com.recoup.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.recoup.backend.dto.MetricsDto;
import com.recoup.backend.service.MetricsService;

@RestController
public class MetricsController {

    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/api/metrics")
    public MetricsDto metrics(@RequestParam String batchId) {
        return metricsService.forBatch(batchId);
    }
}
