package com.recoup.backend.service;

import com.recoup.backend.model.CauseCategory;
import com.recoup.backend.model.RevenueEvent;

/** Supplies the p_recover PolicyEngine uses for its expected-value guardrail
 *  and stopping rule. Deliberately narrow: this returns a number, never a
 *  decision -- what PolicyEngine does with that number stays deterministic
 *  rule code regardless of which implementation is wired in. */
public interface RecoveryProbabilityEstimator {

    double estimate(RevenueEvent event, CauseCategory category);
}
