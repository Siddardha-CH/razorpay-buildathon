package com.recoup.backend.service;

import com.recoup.backend.model.CauseCategory;
import com.recoup.backend.model.RevenueEvent;

/** The cold-start / always-available estimator: the same assumed constants
 *  the simulator itself uses. Deliberately NOT a Spring bean -- it's a plain
 *  value object, used directly in tests and as {@link MlRecoveryProbabilityEstimator}'s
 *  internal fallback, so there's exactly one Spring-managed
 *  {@link RecoveryProbabilityEstimator} for PolicyEngine to autowire. */
public class StaticRecoveryProbabilityEstimator implements RecoveryProbabilityEstimator {

    @Override
    public double estimate(RevenueEvent event, CauseCategory category) {
        return AssumedRecoveryRates.forCategory(category);
    }
}
