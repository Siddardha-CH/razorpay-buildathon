package com.recoup.backend.controller;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.recoup.backend.dto.BatchRunResponse;
import com.recoup.backend.model.RevenueEvent;
import com.recoup.backend.service.PaymentGateway;
import com.recoup.backend.service.RecoveryPipelineService;
import com.recoup.backend.service.SyntheticEventGenerator;

@RestController
public class BatchController {

    private final SyntheticEventGenerator generator;
    private final RecoveryPipelineService pipelineService;
    private final PaymentGateway gateway;

    public BatchController(SyntheticEventGenerator generator, RecoveryPipelineService pipelineService, PaymentGateway gateway) {
        this.generator = generator;
        this.pipelineService = pipelineService;
        this.gateway = gateway;
    }

    @PostMapping("/api/batches/run")
    public BatchRunResponse run(@RequestParam(defaultValue = "60") int size, @RequestParam(defaultValue = "42") long seed) {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        List<RevenueEvent> events = generator.generateBatch(size, seed, Instant.now());
        String batchId = pipelineService.runBatch(events, now);
        return new BatchRunResponse(batchId, events.size(), gateway.name());
    }
}
