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
        String batchId = RecoveryPipelineService.newBatchId();
        List<RevenueEvent> events = generator.generateBatch(200, 2026, Instant.now(), batchId);
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

        pipelineService.runBatch(events, now, batchId);

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

    @Test
    void runningTheSameSeedTwiceGivesIdenticalResultsUnderDifferentRowIds() {
        // Regression test, two bugs in one: (1) event ids used to be derived from
        // (seed, index) alone, so a second run with the same seed against a database
        // that already held the first run's rows violated RecoveryCase's unique
        // event_id constraint; (2) fixing that by salting ids per batch then broke
        // reproducibility, because SimulatedPaymentGateway/PromiseToPayService keyed
        // their outcome RNG off that same salted id -- "same seed" stopped meaning
        // "same recovered amount". RevenueEvent.contentKey (seed+index only, no batch
        // salt) now backs the RNG instead, so both bugs stay fixed at once.
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

        String firstBatchId = RecoveryPipelineService.newBatchId();
        pipelineService.runBatch(generator.generateBatch(20, 555, Instant.now(), firstBatchId), now, firstBatchId);

        String secondBatchId = RecoveryPipelineService.newBatchId();
        pipelineService.runBatch(generator.generateBatch(20, 555, Instant.now(), secondBatchId), now, secondBatchId);

        assertThat(caseRepository.findByBatchId(firstBatchId)).hasSize(20);
        assertThat(caseRepository.findByBatchId(secondBatchId)).hasSize(20);

        MetricsDto first = metricsService.forBatch(firstBatchId);
        MetricsDto second = metricsService.forBatch(secondBatchId);
        assertThat(second.atRiskAmountPaise()).isEqualTo(first.atRiskAmountPaise());
        assertThat(second.recoveredAmountPaise())
            .as("same seed must recover the same amount every run, even though row ids differ")
            .isEqualTo(first.recoveredAmountPaise());
        assertThat(second.recoveredCount()).isEqualTo(first.recoveredCount());
    }
}
