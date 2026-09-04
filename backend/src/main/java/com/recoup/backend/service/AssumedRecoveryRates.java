package com.recoup.backend.service;

import java.util.EnumMap;
import java.util.Map;

import com.recoup.backend.model.CauseCategory;

/** Ground-truth recovery probabilities: what {@link SimulatedPaymentGateway}
 *  actually rolls against to decide whether a simulated attempt succeeds, and
 *  the cold-start fallback {@link MlRecoveryProbabilityEstimator} uses before
 *  it has enough real observed outcomes to train on. Documented assumptions,
 *  not measured -- the whole point of the ML estimator is to learn better
 *  numbers than these from the system's own history instead of trusting them
 *  forever. */
public final class AssumedRecoveryRates {

    private static final Map<CauseCategory, Double> RATES = new EnumMap<>(CauseCategory.class);

    static {
        RATES.put(CauseCategory.TRANSIENT_RETRYABLE, 0.55);
        RATES.put(CauseCategory.HARD_DECLINE_NEEDS_NEW_INSTRUMENT, 0.35);
        RATES.put(CauseCategory.CUSTOMER_ABANDONED, 0.20);
        RATES.put(CauseCategory.RECEIVABLE_GENTLE_STAGE, 0.60);
        RATES.put(CauseCategory.RECEIVABLE_ESCALATION_STAGE, 0.35);
        RATES.put(CauseCategory.RECEIVABLE_LEGAL_STAGE, 0.15);
        RATES.put(CauseCategory.FRAUD_SUSPECTED, 0.0);
    }

    private AssumedRecoveryRates() {
    }

    public static double forCategory(CauseCategory category) {
        return RATES.getOrDefault(category, 0.0);
    }
}
