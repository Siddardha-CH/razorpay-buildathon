package com.recoup.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.recoup.backend.model.RevenueEvent;

/** End-to-end proof that a real batch run produces enough genuine outcomes to
 *  train the model, not just that the training math works in isolation
 *  (LogisticRegressionModelTest) or that the estimator wiring is correct
 *  (MlRecoveryProbabilityEstimatorTest). */
@SpringBootTest
class ModelTrainingServiceTest {

    @Autowired
    private SyntheticEventGenerator generator;
    @Autowired
    private RecoveryPipelineService pipelineService;
    @Autowired
    private ModelTrainingService modelTrainingService;
    @Autowired
    private MlRecoveryProbabilityEstimator estimator;

    @Test
    void aRealisticBatchProducesEnoughHistoryToTrainOn() {
        assertThat(estimator.isTrained()).as("nothing has run yet in this test's context").isFalse();

        String batchId = RecoveryPipelineService.newBatchId();
        List<RevenueEvent> events = generator.generateBatch(300, 4242, Instant.now(), batchId);
        pipelineService.runBatch(events, ZonedDateTime.now(ZoneOffset.UTC), batchId);

        modelTrainingService.retrain();

        assertThat(estimator.isTrained()).isTrue();
        assertThat(estimator.trainingSampleCount()).isGreaterThan(0);
    }
}
