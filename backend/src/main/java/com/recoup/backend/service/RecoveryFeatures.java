package com.recoup.backend.service;

import com.recoup.backend.model.CauseCategory;
import com.recoup.backend.model.RevenueEvent;

/** Turns a (RevenueEvent, CauseCategory) pair into the fixed-length numeric
 *  feature vector the logistic regression model trains and predicts on.
 *
 *  Layout: [bias, one-hot(category)..., log(amount), daysOverdue, attemptCount, isB2b].
 *  Everything is scaled to roughly [0, 1.5] so gradient descent converges
 *  without per-feature learning rates. */
public final class RecoveryFeatures {

    private static final CauseCategory[] CATEGORIES = CauseCategory.values();
    public static final int DIMENSION = 1 + CATEGORIES.length + 4;

    private RecoveryFeatures() {
    }

    public static double[] build(RevenueEvent event, CauseCategory category) {
        double[] x = new double[DIMENSION];
        x[0] = 1.0; // bias
        for (int i = 0; i < CATEGORIES.length; i++) {
            x[1 + i] = CATEGORIES[i] == category ? 1.0 : 0.0;
        }
        int base = 1 + CATEGORIES.length;
        x[base] = Math.log1p(event.getAmountPaise() / 100.0) / 12.0;          // log(rupees), ~0-1.3 for typical amounts
        x[base + 1] = Math.min(event.getDaysOverdue(), 90) / 90.0;            // 0 for non-receivables
        x[base + 2] = Math.min(event.getAttemptCount(), 5) / 5.0;
        x[base + 3] = event.isB2b() ? 1.0 : 0.0;
        return x;
    }
}
