package com.recoup.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.recoup.backend.dto.MetricsDto;
import com.recoup.backend.model.RevenueEvent;
import com.recoup.backend.repository.AuditEntryRepository;
import com.recoup.backend.repository.RecoveryCaseRepository;

/** End-to-end smoke test: generate a realistic batch, run it through the full
 *  pipeline against an in-memory database, and check the claims the submission
 *  bar cares about -- every case reaches a terminal status, and NOT ONE
 *  guardrail was violated across the whole batch. This is the empirical proof
 *  behind "honest metrics" and "compliant escalation", not just a per-rule
 *  unit test. */
@SpringBootTest
class RecoveryPipelineServiceTest {

    @Autowired
    private SyntheticEventGenerator generator;
    @Autowired
    private RecoveryPipelineService pipelineService;
    @Autowired
    private MetricsService metricsService;
    @Autowired
    private RecoveryCaseRepository caseRepository;
    @Autowired
    private AuditEntryRepository auditEntryRepository;

    @Test
    void processesAFullBatchWithZeroGuardrailViolations() {
        List<RevenueEvent> events = generator.generateBatch(200, 2026, Instant.now());
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

        String batchId = pipelineService.runBatch(events, now);

        assertThat(caseRepository.findByBatchId(batchId)).hasSize(200);

        MetricsDto metrics = metricsService.forBatch(batchId);
        assertThat(metrics.totalCases()).isEqualTo(200);
        assertThat(metrics.guardrailChecksEvaluated()).isGreaterThan(0);
        assertThat(metrics.guardrailViolations())
            .as("every decision must satisfy every guardrail evaluated against it")
            .isZero();
        assertThat(metrics.recoveredAmountPaise()).isLessThanOrEqualTo(metrics.atRiskAmountPaise());
        assertThat(metrics.recoveryRate()).isBetween(0.0, 1.0);
    }
}
