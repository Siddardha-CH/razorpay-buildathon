package com.recoup.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.recoup.backend.model.ActionResult;
import com.recoup.backend.model.CauseCategory;
import com.recoup.backend.model.RevenueEvent;
import com.recoup.backend.util.TestFixtures;

class SimulatedPaymentGatewayTest {

    private final SimulatedPaymentGateway gateway = new SimulatedPaymentGateway();

    @Test
    void isDeterministicForTheSameEventId() {
        RevenueEvent event = TestFixtures.failedPayment("evt_repeatable", "issuer_unavailable", 0, false);
        ActionResult first = gateway.retryPayment(event, CauseCategory.TRANSIENT_RETRYABLE);
        ActionResult second = gateway.retryPayment(event, CauseCategory.TRANSIENT_RETRYABLE);
        assertThat(first.isSuccess()).isEqualTo(second.isSuccess());
        assertThat(first.getRecoveredAmountPaise()).isEqualTo(second.getRecoveredAmountPaise());
    }

    @Test
    void neverFabricatesRecoveredMoneyForAFraudCategory() {
        RevenueEvent event = TestFixtures.failedPayment("evt_fraud", "risk_check_failed", 0, false);
        // A fraud-suspected case should never reach the gateway per PolicyEngine, but if it
        // did, recovery probability for that category is 0 -- confirm the simulator honors it.
        for (int i = 0; i < 20; i++) {
            ActionResult result = gateway.createPaymentLink(TestFixtures.failedPayment("evt_fraud_" + i, "risk_check_failed", 0, false),
                CauseCategory.FRAUD_SUSPECTED);
            assertThat(result.isSuccess()).isFalse();
        }
    }
}
