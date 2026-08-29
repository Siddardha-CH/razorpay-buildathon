package com.recoup.backend.service;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.recoup.backend.model.CauseCategory;
import com.recoup.backend.model.Diagnosis;
import com.recoup.backend.model.EventType;
import com.recoup.backend.model.RevenueEvent;

/** Root-cause diagnosis. Deterministic rules do the actual classification; an
 *  optional Groq call only refines the free-text reasoning shown to a human,
 *  and never changes the category on its own -- money-relevant decisions stay
 *  rule-driven and testable. */
@Service
public class DiagnosisService {

    private static final Set<String> RETRYABLE_REASONS = Set.of("issuer_unavailable", "bank_server_down", "insufficient_funds");
    private static final Set<String> HARD_DECLINE_REASONS = Set.of("card_expired", "invalid_card_number", "authentication_failed");
    private static final Set<String> FRAUD_REASONS = Set.of("risk_check_failed", "blocklisted_instrument");

    private final GroqClient groqClient;

    public DiagnosisService(GroqClient groqClient) {
        this.groqClient = groqClient;
    }

    public Diagnosis diagnose(RevenueEvent event) {
        if (event.getType() == EventType.FAILED_PAYMENT || event.getType() == EventType.FAILED_MANDATE) {
            return diagnosePaymentFailure(event);
        }
        if (event.getType() == EventType.ABANDONED_CHECKOUT) {
            return diagnoseAbandonment(event);
        }
        return diagnoseReceivable(event);
    }

    private Diagnosis diagnosePaymentFailure(RevenueEvent event) {
        String reason = event.getDeclineReason() == null ? "" : event.getDeclineReason();
        if (FRAUD_REASONS.contains(reason)) {
            return new Diagnosis(CauseCategory.FRAUD_SUSPECTED, 0.9,
                "decline reason '" + reason + "' matches known fraud/risk signal", "rule_engine");
        }
        if (RETRYABLE_REASONS.contains(reason)) {
            return new Diagnosis(CauseCategory.TRANSIENT_RETRYABLE, 0.85,
                "decline reason '" + reason + "' is a transient gateway/bank-side failure", "rule_engine");
        }
        if (HARD_DECLINE_REASONS.contains(reason)) {
            return new Diagnosis(CauseCategory.HARD_DECLINE_NEEDS_NEW_INSTRUMENT, 0.85,
                "decline reason '" + reason + "' requires a new payment instrument", "rule_engine");
        }
        return new Diagnosis(CauseCategory.CUSTOMER_ABANDONED, 0.5,
            "decline reason '" + reason + "' does not match a known retryable/hard pattern", "rule_engine");
    }

    private Diagnosis diagnoseAbandonment(RevenueEvent event) {
        String reasoning = "customer exited at checkout stage '" + event.getCheckoutStage() + "'";
        String source = "rule_engine";
        if (event.getSupportNote() != null && !event.getSupportNote().isBlank()) {
            String prompt = "A customer abandoned checkout at stage '" + event.getCheckoutStage()
                + "'. Support note: \"" + event.getSupportNote() + "\". In one short sentence, "
                + "state the most likely reason they left, for an internal ops dashboard.";
            var llmNote = groqClient.complete(prompt, 60);
            if (llmNote.isPresent()) {
                reasoning = llmNote.get();
                source = "llm_assisted";
            }
        }
        return new Diagnosis(CauseCategory.CUSTOMER_ABANDONED, 0.6, reasoning, source);
    }

    private Diagnosis diagnoseReceivable(RevenueEvent event) {
        CauseCategory category;
        if (event.getDaysOverdue() <= 15) {
            category = CauseCategory.RECEIVABLE_GENTLE_STAGE;
        } else if (event.getDaysOverdue() <= 45) {
            category = CauseCategory.RECEIVABLE_ESCALATION_STAGE;
        } else {
            category = CauseCategory.RECEIVABLE_LEGAL_STAGE;
        }
        String reasoning = event.getDaysOverdue() + " days overdue on a " + (event.isB2b() ? "B2B" : "B2C") + " invoice";
        return new Diagnosis(category, 1.0, reasoning, "rule_engine");
    }
}
