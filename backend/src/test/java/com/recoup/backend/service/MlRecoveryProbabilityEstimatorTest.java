package com.recoup.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.recoup.backend.model.ActionResult;
import com.recoup.backend.model.CauseCategory;
import com.recoup.backend.model.Decision;
import com.recoup.backend.model.Diagnosis;
import com.recoup.backend.model.InterventionType;
import com.recoup.backend.model.Promise;
import com.recoup.backend.model.RecoveryCase;
import com.recoup.backend.util.TestFixtures;

class MlRecoveryProbabilityEstimatorTest {

    private final MlRecoveryProbabilityEstimator estimator = new MlRecoveryProbabilityEstimator();

    private RecoveryCase historicalCase(String id, CauseCategory category, InterventionType intervention, boolean recovered) {
        RecoveryCase c = new RecoveryCase();
        c.setEvent(TestFixtures.failedPayment(id, "issuer_unavailable", 0, false));
        c.setDiagnosis(new Diagnosis(category, 0.9, "r", "rule_engine"));
        c.setDecision(new Decision(intervention, "r", 0, 0.5, 500));
        c.setActionResult(new ActionResult(recovered, "d", recovered ? 100_000 : 0, null));
        c.setPromise(new Promise(0, null, null));
        return c;
    }

    @Test
    void staysOnAssumedRatesBeforeThereIsEnoughHistory() {
        assertThat(estimator.isTrained()).isFalse();
        var event = TestFixtures.failedPayment("evt_cold", "issuer_unavailable", 0, false);
        assertThat(estimator.estimate(event, CauseCategory.TRANSIENT_RETRYABLE))
            .isEqualTo(AssumedRecoveryRates.forCategory(CauseCategory.TRANSIENT_RETRYABLE));
    }

    @Test
    void ignoresCensoredAndUnattemptedCasesWhenTraining() {
        // Only 5 genuine trainable outcomes; the rest are scheduled/skipped/routed cases
        // that were never actually attempted and must not count toward the sample size.
        List<RecoveryCase> history = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            history.add(historicalCase("evt_real_" + i, CauseCategory.TRANSIENT_RETRYABLE, InterventionType.RETRY_PAYMENT, i % 2 == 0));
        }
        for (int i = 0; i < 50; i++) {
            history.add(historicalCase("evt_scheduled_" + i, CauseCategory.TRANSIENT_RETRYABLE, InterventionType.SCHEDULE_MANDATE_RETRY, false));
            history.add(historicalCase("evt_skipped_" + i, CauseCategory.RECEIVABLE_LEGAL_STAGE, InterventionType.MARK_DO_NOT_CONTACT, false));
        }

        estimator.trainOn(history);

        assertThat(estimator.isTrained()).as("5 real outcomes is below the minimum sample threshold").isFalse();
    }

    @Test
    void neverExtrapolatesANonZeroProbabilityForFraudSuspectedEvenAfterTraining() {
        // Regression test: found live by inspecting /api/model/status after two real
        // batch runs -- the trained model predicted ~37% recovery for FRAUD_SUSPECTED
        // despite zero training examples in that category, because its one-hot weight
        // stays at zero and the bias/amount weights (learned from everything else)
        // filled in a meaningless extrapolation instead.
        List<RecoveryCase> history = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            history.add(historicalCase("evt_" + i, CauseCategory.TRANSIENT_RETRYABLE, InterventionType.RETRY_PAYMENT, i % 3 != 0));
        }
        estimator.trainOn(history);
        assertThat(estimator.isTrained()).isTrue();

        var event = TestFixtures.failedPayment("evt_fraud", "risk_check_failed", 0, false);
        assertThat(estimator.estimate(event, CauseCategory.FRAUD_SUSPECTED)).isZero();
    }

    @Test
    void trainsAndLearnsHigherAmountsRecoverLessOftenWhenThatIsWhatTheDataShows() {
        Random rng = new Random(42);
        List<RecoveryCase> history = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            boolean smallAmount = rng.nextBoolean();
            RecoveryCase c = historicalCase("evt_train_" + i, CauseCategory.HARD_DECLINE_NEEDS_NEW_INSTRUMENT,
                InterventionType.SEND_ALT_PAYMENT_LINK, smallAmount ? rng.nextDouble() < 0.8 : rng.nextDouble() < 0.1);
            c.getEvent().setAmountPaise(smallAmount ? 50_000 : 5_000_000);
            history.add(c);
        }

        estimator.trainOn(history);

        assertThat(estimator.isTrained()).isTrue();
        assertThat(estimator.trainingSampleCount()).isEqualTo(200);

        var smallAmountEvent = TestFixtures.failedPayment("evt_small", "card_expired", 0, false);
        smallAmountEvent.setAmountPaise(50_000);
        var largeAmountEvent = TestFixtures.failedPayment("evt_large", "card_expired", 0, false);
        largeAmountEvent.setAmountPaise(5_000_000);

        double smallAmountProb = estimator.estimate(smallAmountEvent, CauseCategory.HARD_DECLINE_NEEDS_NEW_INSTRUMENT);
        double largeAmountProb = estimator.estimate(largeAmountEvent, CauseCategory.HARD_DECLINE_NEEDS_NEW_INSTRUMENT);

        assertThat(smallAmountProb)
            .as("model should have learned that small amounts recovered far more often in the training data")
            .isGreaterThan(largeAmountProb);
    }
}
