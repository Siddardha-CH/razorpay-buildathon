package com.recoup.backend.service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.recoup.backend.model.CauseCategory;
import com.recoup.backend.model.Decision;
import com.recoup.backend.model.EventType;
import com.recoup.backend.model.GuardrailCheck;
import com.recoup.backend.model.InterventionType;
import com.recoup.backend.model.RevenueEvent;

/** Bounded, compliant intervention policy.
 *
 *  This is the one class every money-relevant decision passes through. It is
 *  deliberately NOT an LLM: every rule here is a plain method you can unit-test
 *  and a human can audit line by line. DiagnosisService may use an LLM to
 *  narrate *why*; this class never does.
 *
 *  The one number it doesn't compute itself is p_recover: that comes from an
 *  injected {@link RecoveryProbabilityEstimator} (a logistic regression model
 *  trained on real outcomes, falling back to assumed constants until there's
 *  enough history -- see MlRecoveryProbabilityEstimator). Which intervention
 *  gets chosen, and every guardrail applied to it, stays this class's rule
 *  code regardless of where that one number came from. */
@Service
public class PolicyEngine {

    public static final int MAX_RETRIES = 3;
    public static final int MAX_AUTOMATED_PURSUIT_DAYS = 60;
    private static final long B2B_HUMAN_ESCALATION_THRESHOLD_PAISE = 1_000_000; // Rs 10,000
    private static final DateTimeFormatter SCHEDULE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");

    private static final Map<InterventionType, Long> COST_PAISE = new EnumMap<>(InterventionType.class);

    private static final Set<InterventionType> CONTACT_INTERVENTIONS = Set.of(
        InterventionType.SEND_ALT_PAYMENT_LINK, InterventionType.SEND_REMINDER_SMS,
        InterventionType.SEND_REMINDER_WHATSAPP, InterventionType.SEND_REMINDER_EMAIL
    );

    static {
        COST_PAISE.put(InterventionType.RETRY_PAYMENT, 0L);
        COST_PAISE.put(InterventionType.SCHEDULE_MANDATE_RETRY, 0L);
        COST_PAISE.put(InterventionType.SEND_ALT_PAYMENT_LINK, 0L);
        COST_PAISE.put(InterventionType.SEND_REMINDER_SMS, 15L);
        COST_PAISE.put(InterventionType.SEND_REMINDER_WHATSAPP, 35L);
        COST_PAISE.put(InterventionType.SEND_REMINDER_EMAIL, 2L);
        COST_PAISE.put(InterventionType.HUMAN_ESCALATION, 15_000L);
        COST_PAISE.put(InterventionType.ROUTE_TO_RISK_TEAM, 0L);
        COST_PAISE.put(InterventionType.MARK_DO_NOT_CONTACT, 0L);
        COST_PAISE.put(InterventionType.STOP_PURSUIT, 0L);
        COST_PAISE.put(InterventionType.DEFER_QUIET_HOURS, 0L);
    }

    private final int quietHourStart;
    private final int quietHourEnd;
    private final RecoveryProbabilityEstimator probabilityEstimator;

    public PolicyEngine(@Value("${app.quiet-hour-start:21}") int quietHourStart,
                         @Value("${app.quiet-hour-end:8}") int quietHourEnd,
                         RecoveryProbabilityEstimator probabilityEstimator) {
        this.quietHourStart = quietHourStart;
        this.quietHourEnd = quietHourEnd;
        this.probabilityEstimator = probabilityEstimator;
    }

    public boolean withinQuietHours(ZonedDateTime now) {
        int hour = now.getHour();
        return hour >= quietHourStart || hour < quietHourEnd;
    }

    public GuardrailCheck guardrailNoDndContact(RevenueEvent event, InterventionType intervention) {
        boolean violates = event.getContact().isDnd() && CONTACT_INTERVENTIONS.contains(intervention);
        return new GuardrailCheck("no_contact_when_dnd", !violates,
            "dnd=" + event.getContact().isDnd() + ", intervention=" + intervention);
    }

    public GuardrailCheck guardrailQuietHours(InterventionType intervention, ZonedDateTime now) {
        boolean violates = CONTACT_INTERVENTIONS.contains(intervention) && withinQuietHours(now);
        return new GuardrailCheck("respects_quiet_hours", !violates,
            "hour=" + now.getHour() + ", intervention=" + intervention);
    }

    public GuardrailCheck guardrailMaxRetries(RevenueEvent event, InterventionType intervention) {
        boolean isRetry = intervention == InterventionType.RETRY_PAYMENT || intervention == InterventionType.SCHEDULE_MANDATE_RETRY;
        boolean violates = isRetry && event.getAttemptCount() >= MAX_RETRIES;
        return new GuardrailCheck("max_retries_respected", !violates,
            "attemptCount=" + event.getAttemptCount() + ", max=" + MAX_RETRIES);
    }

    public GuardrailCheck guardrailPursuitWindow(RevenueEvent event, InterventionType intervention) {
        boolean stale = event.getType() == EventType.OVERDUE_RECEIVABLE && event.getDaysOverdue() > MAX_AUTOMATED_PURSUIT_DAYS;
        boolean violates = stale && intervention != InterventionType.HUMAN_ESCALATION && intervention != InterventionType.STOP_PURSUIT;
        return new GuardrailCheck("automated_pursuit_window_respected", !violates,
            "daysOverdue=" + event.getDaysOverdue() + ", maxAutomated=" + MAX_AUTOMATED_PURSUIT_DAYS);
    }

    public GuardrailCheck guardrailCostBounded(long costPaise, double expectedValuePaise) {
        boolean passed = costPaise == 0 || costPaise <= expectedValuePaise;
        return new GuardrailCheck("intervention_cost_bounded_by_expected_value", passed,
            "cost=" + costPaise + "p, expectedValue=" + Math.round(expectedValuePaise) + "p");
    }

    public List<GuardrailCheck> allGuardrails(RevenueEvent event, InterventionType intervention, ZonedDateTime now,
                                               long costPaise, double expectedValuePaise) {
        return List.of(
            guardrailNoDndContact(event, intervention),
            guardrailQuietHours(intervention, now),
            guardrailMaxRetries(event, intervention),
            guardrailPursuitWindow(event, intervention),
            guardrailCostBounded(costPaise, expectedValuePaise)
        );
    }

    private InterventionType baseIntervention(RevenueEvent event, CauseCategory category) {
        // A failed mandate is a bank-initiated background debit, not a live customer
        // session -- it gets a scheduled, spaced-out retry cadence instead of an
        // immediate same-pass retry (see MandateRetrySequencer).
        if (event.getType() == EventType.FAILED_MANDATE && category == CauseCategory.TRANSIENT_RETRYABLE) {
            return MandateRetrySequencer.hasRemainingRetries(event.getAttemptCount())
                ? InterventionType.SCHEDULE_MANDATE_RETRY
                : InterventionType.SEND_ALT_PAYMENT_LINK;
        }
        return switch (category) {
            case FRAUD_SUSPECTED -> InterventionType.ROUTE_TO_RISK_TEAM;
            case TRANSIENT_RETRYABLE -> event.getAttemptCount() < MAX_RETRIES
                ? InterventionType.RETRY_PAYMENT : InterventionType.SEND_ALT_PAYMENT_LINK;
            case HARD_DECLINE_NEEDS_NEW_INSTRUMENT -> InterventionType.SEND_ALT_PAYMENT_LINK;
            case CUSTOMER_ABANDONED -> event.getContact().isWhatsappOptIn()
                ? InterventionType.SEND_REMINDER_WHATSAPP : InterventionType.SEND_REMINDER_EMAIL;
            case RECEIVABLE_GENTLE_STAGE -> InterventionType.SEND_REMINDER_SMS;
            case RECEIVABLE_ESCALATION_STAGE -> (event.isB2b() && event.getAmountPaise() > B2B_HUMAN_ESCALATION_THRESHOLD_PAISE)
                ? InterventionType.HUMAN_ESCALATION
                : (event.getContact().isWhatsappOptIn() ? InterventionType.SEND_REMINDER_WHATSAPP : InterventionType.SEND_REMINDER_EMAIL);
            case RECEIVABLE_LEGAL_STAGE -> event.isB2b() ? InterventionType.HUMAN_ESCALATION : InterventionType.SEND_REMINDER_EMAIL;
        };
    }

    public Decision decide(RevenueEvent event, CauseCategory category, ZonedDateTime now) {
        InterventionType intervention = baseIntervention(event, category);

        // Compliance overrides always win over the policy's first choice.
        if (event.getContact().isDnd() && CONTACT_INTERVENTIONS.contains(intervention)) {
            intervention = InterventionType.MARK_DO_NOT_CONTACT;
        } else if (CONTACT_INTERVENTIONS.contains(intervention) && withinQuietHours(now)) {
            intervention = InterventionType.DEFER_QUIET_HOURS;
        } else if (event.getType() == EventType.OVERDUE_RECEIVABLE && event.getDaysOverdue() > MAX_AUTOMATED_PURSUIT_DAYS
                && intervention != InterventionType.HUMAN_ESCALATION) {
            intervention = InterventionType.HUMAN_ESCALATION;
        }

        long cost = COST_PAISE.get(intervention);
        boolean active = intervention != InterventionType.MARK_DO_NOT_CONTACT
            && intervention != InterventionType.STOP_PURSUIT
            && intervention != InterventionType.ROUTE_TO_RISK_TEAM
            && intervention != InterventionType.DEFER_QUIET_HOURS;
        double prob = active ? probabilityEstimator.estimate(event, category) : 0.0;
        double expectedValue = event.getAmountPaise() * prob;

        if (active && cost > expectedValue) {
            intervention = InterventionType.STOP_PURSUIT;
            cost = 0L;
            prob = 0.0;
            expectedValue = 0.0;
        }

        String scheduledFor = null;
        if (intervention == InterventionType.SCHEDULE_MANDATE_RETRY) {
            scheduledFor = SCHEDULE_FORMAT.format(
                MandateRetrySequencer.nextRetryDate(event.getAttemptCount(), now).withZoneSameInstant(ZoneOffset.ofHoursMinutes(5, 30)));
        }

        String reasoning = String.format(
            "category=%s -> %s (attempt=%d, cost=%dp, p_recover=%.2f, expected_value=%.0fp)%s",
            category, intervention, event.getAttemptCount(), cost, prob, expectedValue,
            scheduledFor != null ? ", next_retry=" + scheduledFor : ""
        );
        return new Decision(intervention, reasoning, cost, prob, expectedValue, scheduledFor);
    }
}
