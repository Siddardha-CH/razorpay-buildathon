package com.recoup.backend.controller;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.recoup.backend.dto.BatchRunResponse;
import com.recoup.backend.model.RevenueEvent;
import com.recoup.backend.service.PaymentGateway;
import com.recoup.backend.service.RecoveryPipelineService;
import com.recoup.backend.service.SyntheticEventGenerator;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@Validated
public class BatchController {

    // Upper bound exists because this whole batch runs inside one HTTP request and
    // one transaction -- an unbounded size is a self-inflicted DoS on a single-instance demo server.
    private static final int MAX_BATCH_SIZE = 2000;

    private final SyntheticEventGenerator generator;
    private final RecoveryPipelineService pipelineService;
    private final PaymentGateway gateway;

    public BatchController(SyntheticEventGenerator generator, RecoveryPipelineService pipelineService, PaymentGateway gateway) {
        this.generator = generator;
        this.pipelineService = pipelineService;
        this.gateway = gateway;
    }

    @PostMapping("/api/batches/run")
    public BatchRunResponse run(@RequestParam(defaultValue = "60") @Min(1) @Max(MAX_BATCH_SIZE) int size,
                                 @RequestParam(defaultValue = "42") long seed) {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String batchId = RecoveryPipelineService.newBatchId();
        List<RevenueEvent> events = generator.generateBatch(size, seed, Instant.now(), batchId);
        pipelineService.runBatch(events, now, batchId);
        return new BatchRunResponse(batchId, events.size(), gateway.name());
    }
}
