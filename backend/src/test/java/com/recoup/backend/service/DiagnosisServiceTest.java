package com.recoup.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.recoup.backend.model.CauseCategory;
import com.recoup.backend.model.Diagnosis;
import com.recoup.backend.model.RevenueEvent;
import com.recoup.backend.util.TestFixtures;

class DiagnosisServiceTest {

    // No GROQ_API_KEY in the test environment, so GroqClient.complete() short-circuits
    // to empty and every diagnosis here comes from the rule engine -- exactly what we
    // want to lock in, since diagnosis controls a money-relevant decision downstream.
    private final DiagnosisService diagnosisService = new DiagnosisService(new GroqClient());

    @Test
    void classifiesGatewayTimeoutsAsTransientRetryable() {
        RevenueEvent event = TestFixtures.failedPayment("evt_1", "issuer_unavailable", 0, false);
        Diagnosis diagnosis = diagnosisService.diagnose(event);
        assertThat(diagnosis.getCategory()).isEqualTo(CauseCategory.TRANSIENT_RETRYABLE);
        assertThat(diagnosis.getSource()).isEqualTo("rule_engine");
    }

    @Test
    void classifiesExpiredCardAsHardDecline() {
        RevenueEvent event = TestFixtures.failedPayment("evt_2", "card_expired", 0, false);
        Diagnosis diagnosis = diagnosisService.diagnose(event);
        assertThat(diagnosis.getCategory()).isEqualTo(CauseCategory.HARD_DECLINE_NEEDS_NEW_INSTRUMENT);
    }

    @Test
    void classifiesRiskCheckFailureAsFraudSuspected() {
        RevenueEvent event = TestFixtures.failedPayment("evt_3", "risk_check_failed", 0, false);
        Diagnosis diagnosis = diagnosisService.diagnose(event);
        assertThat(diagnosis.getCategory()).isEqualTo(CauseCategory.FRAUD_SUSPECTED);
    }

    @Test
    void bucketsReceivablesByDaysOverdue() {
        assertThat(diagnosisService.diagnose(TestFixtures.overdueReceivable("r1", 5, true, 100000)).getCategory())
            .isEqualTo(CauseCategory.RECEIVABLE_GENTLE_STAGE);
        assertThat(diagnosisService.diagnose(TestFixtures.overdueReceivable("r2", 30, true, 100000)).getCategory())
            .isEqualTo(CauseCategory.RECEIVABLE_ESCALATION_STAGE);
        assertThat(diagnosisService.diagnose(TestFixtures.overdueReceivable("r3", 60, true, 100000)).getCategory())
            .isEqualTo(CauseCategory.RECEIVABLE_LEGAL_STAGE);
    }
}
