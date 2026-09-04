package com.recoup.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import com.recoup.backend.model.CauseCategory;
import com.recoup.backend.model.Decision;
import com.recoup.backend.model.GuardrailCheck;
import com.recoup.backend.model.InterventionType;
import com.recoup.backend.model.RevenueEvent;
import com.recoup.backend.util.TestFixtures;

/** These tests exist because PolicyEngine is where every money-relevant
 *  decision is made -- if a guardrail here has a hole, the pipeline can
 *  contact a DND customer, retry a payment forever, or chase a stale
 *  receivable past the compliant window. Each test locks in one guardrail. */
class PolicyEngineTest {

    private final PolicyEngine policyEngine = new PolicyEngine(21, 8, new StaticRecoveryProbabilityEstimator());

    private static ZonedDateTime daytime() {
        return ZonedDateTime.of(2026, 8, 29, 14, 0, 0, 0, ZoneOffset.UTC); // outside quiet hours
    }

    private static ZonedDateTime nighttime() {
        return ZonedDateTime.of(2026, 8, 29, 23, 0, 0, 0, ZoneOffset.UTC); // inside quiet hours
    }

    @Test
    void neverContactsADndCustomer() {
        RevenueEvent event = TestFixtures.failedPayment("evt_dnd", "card_expired", 0, true);
        Decision decision = policyEngine.decide(event, CauseCategory.HARD_DECLINE_NEEDS_NEW_INSTRUMENT, daytime());

        assertThat(decision.getIntervention()).isEqualTo(InterventionType.MARK_DO_NOT_CONTACT);
        GuardrailCheck check = policyEngine.guardrailNoDndContact(event, decision.getIntervention());
        assertThat(check.isPassed()).isTrue();
    }

    @Test
    void defersContactDuringQuietHoursInsteadOfSending() {
        RevenueEvent event = TestFixtures.failedPayment("evt_quiet", "card_expired", 0, false);
        Decision decision = policyEngine.decide(event, CauseCategory.HARD_DECLINE_NEEDS_NEW_INSTRUMENT, nighttime());

        assertThat(decision.getIntervention()).isEqualTo(InterventionType.DEFER_QUIET_HOURS);
        assertThat(policyEngine.guardrailQuietHours(decision.getIntervention(), nighttime()).isPassed()).isTrue();
    }

    @Test
    void stopsRetryingAfterMaxRetries() {
        RevenueEvent event = TestFixtures.failedPayment("evt_retries", "insufficient_funds", PolicyEngine.MAX_RETRIES, false);
        Decision decision = policyEngine.decide(event, CauseCategory.TRANSIENT_RETRYABLE, daytime());

        assertThat(decision.getIntervention()).isNotEqualTo(InterventionType.RETRY_PAYMENT);
        assertThat(policyEngine.guardrailMaxRetries(event, InterventionType.RETRY_PAYMENT).isPassed()).isFalse();
    }

    @Test
    void retriesWhenUnderTheRetryLimit() {
        RevenueEvent event = TestFixtures.failedPayment("evt_retry_ok", "insufficient_funds", 1, false);
        Decision decision = policyEngine.decide(event, CauseCategory.TRANSIENT_RETRYABLE, daytime());

        assertThat(decision.getIntervention()).isEqualTo(InterventionType.RETRY_PAYMENT);
        assertThat(policyEngine.guardrailMaxRetries(event, decision.getIntervention()).isPassed()).isTrue();
    }

    @Test
    void escalatesToHumanPastTheAutomatedPursuitWindowEvenForAB2cCase() {
        // b2b=false would normally get an automated email reminder (see baseIntervention);
        // the pursuit-window guardrail should override that once it's gone stale.
        RevenueEvent event = TestFixtures.overdueReceivable("evt_stale", PolicyEngine.MAX_AUTOMATED_PURSUIT_DAYS + 5, false, 2_000_000);
        Decision decision = policyEngine.decide(event, CauseCategory.RECEIVABLE_LEGAL_STAGE, daytime());

        assertThat(decision.getIntervention()).isEqualTo(InterventionType.HUMAN_ESCALATION);
        assertThat(policyEngine.guardrailPursuitWindow(event, decision.getIntervention()).isPassed()).isTrue();
    }

    @Test
    void routesFraudSuspectsToRiskTeamWithNoRecoveryAttempt() {
        RevenueEvent event = TestFixtures.failedPayment("evt_fraud", "risk_check_failed", 0, false);
        Decision decision = policyEngine.decide(event, CauseCategory.FRAUD_SUSPECTED, daytime());

        assertThat(decision.getIntervention()).isEqualTo(InterventionType.ROUTE_TO_RISK_TEAM);
        assertThat(decision.getEstimatedRecoveryProbability()).isZero();
        assertThat(decision.getEstimatedCostPaise()).isZero();
    }

    @Test
    void stopsPursuitWhenInterventionCostExceedsExpectedValue() {
        // A tiny receivable in the legal-stage bucket (15% recovery odds) plus a
        // deliberately inflated human-escalation cost makes chasing it a loss.
        RevenueEvent event = TestFixtures.overdueReceivable("evt_uneconomic", 50, true, 500); // Rs 5 at risk
        Decision decision = policyEngine.decide(event, CauseCategory.RECEIVABLE_LEGAL_STAGE, daytime());

        assertThat(decision.getIntervention()).isEqualTo(InterventionType.STOP_PURSUIT);
        assertThat(policyEngine.guardrailCostBounded(decision.getEstimatedCostPaise(), decision.getExpectedValuePaise()).isPassed()).isTrue();
    }

    @Test
    void schedulesRatherThanImmediatelyRetryingAFailedMandate() {
        RevenueEvent event = TestFixtures.failedMandate("evt_mandate", "issuer_unavailable", 0, false);
        Decision decision = policyEngine.decide(event, CauseCategory.TRANSIENT_RETRYABLE, daytime());

        assertThat(decision.getIntervention()).isEqualTo(InterventionType.SCHEDULE_MANDATE_RETRY);
        assertThat(decision.getScheduledFor()).isNotNull();
        assertThat(policyEngine.guardrailMaxRetries(event, decision.getIntervention()).isPassed()).isTrue();
    }

    @Test
    void givesUpAutomatedMandateRetryOnceTheSequenceIsExhausted() {
        RevenueEvent event = TestFixtures.failedMandate("evt_mandate_exhausted", "issuer_unavailable", 3, false);
        Decision decision = policyEngine.decide(event, CauseCategory.TRANSIENT_RETRYABLE, daytime());

        assertThat(decision.getIntervention()).isEqualTo(InterventionType.SEND_ALT_PAYMENT_LINK);
        assertThat(decision.getScheduledFor()).isNull();
    }

    @Test
    void allGuardrailsPassForAnOrdinaryRetryableCase() {
        RevenueEvent event = TestFixtures.failedPayment("evt_ordinary", "issuer_unavailable", 0, false);
        Decision decision = policyEngine.decide(event, CauseCategory.TRANSIENT_RETRYABLE, daytime());

        var checks = policyEngine.allGuardrails(event, decision.getIntervention(), daytime(),
            decision.getEstimatedCostPaise(), decision.getExpectedValuePaise());

        assertThat(checks).allMatch(GuardrailCheck::isPassed);
    }
}
