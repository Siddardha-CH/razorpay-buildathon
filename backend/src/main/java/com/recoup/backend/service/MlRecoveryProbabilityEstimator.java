package com.recoup.backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.recoup.backend.model.ActionResult;
import com.recoup.backend.model.CauseCategory;
import com.recoup.backend.model.InterventionType;
import com.recoup.backend.model.RecoveryCase;
import com.recoup.backend.model.RevenueEvent;

/** The Spring-managed {@link RecoveryProbabilityEstimator}: a logistic
 *  regression model trained on every past case where a real recovery attempt
 *  was made and a terminal outcome was observed, falling back to
 *  {@link AssumedRecoveryRates} until there's enough history to train on.
 *
 *  Retraining happens synchronously at the start of every batch run (see
 *  ModelTrainingService) -- fine at this scale (thousands of rows, batch
 *  gradient descent in milliseconds), not how you'd do it against a real
 *  production table (see docs/ARCHITECTURE.md, Extension points). */
@Service
public class MlRecoveryProbabilityEstimator implements RecoveryProbabilityEstimator {

    private static final Logger log = LoggerFactory.getLogger(MlRecoveryProbabilityEstimator.class);
    private static final int MIN_TRAINING_SAMPLES = 30;

    /** Interventions where we actually attempted recovery and observed a
     *  terminal outcome. Excludes MARK_DO_NOT_CONTACT / STOP_PURSUIT /
     *  ROUTE_TO_RISK_TEAM / DEFER_QUIET_HOURS (no attempt was made -- there's
     *  nothing to learn from a case we never tried) and
     *  SCHEDULE_MANDATE_RETRY (the outcome is censored: it's scheduled for the
     *  future, "not recovered yet" is not the same as "failed"). */
    private static final Set<InterventionType> TRAINABLE = Set.of(
        InterventionType.RETRY_PAYMENT, InterventionType.SEND_ALT_PAYMENT_LINK,
        InterventionType.SEND_REMINDER_SMS, InterventionType.SEND_REMINDER_WHATSAPP,
        InterventionType.SEND_REMINDER_EMAIL, InterventionType.HUMAN_ESCALATION
    );

    private final LogisticRegressionModel model = new LogisticRegressionModel();
    private final RecoveryProbabilityEstimator fallback = new StaticRecoveryProbabilityEstimator();

    public static Set<InterventionType> trainableInterventions() {
        return TRAINABLE;
    }

    public synchronized void trainOn(List<RecoveryCase> historicalCases) {
        List<double[]> features = new ArrayList<>();
        List<Double> labels = new ArrayList<>();

        for (RecoveryCase c : historicalCases) {
            InterventionType intervention = c.getDecision().getIntervention();
            if (!TRAINABLE.contains(intervention)) {
                continue;
            }
            RevenueEvent event = c.getEvent();
            CauseCategory category = c.getDiagnosis().getCategory();
            ActionResult result = c.getActionResult();

            features.add(RecoveryFeatures.build(event, category));
            labels.add(result.getRecoveredAmountPaise() > 0 ? 1.0 : 0.0);
        }

        if (features.size() < MIN_TRAINING_SAMPLES) {
            log.info("Only {} trainable outcomes available (need {}) -- staying on assumed rates", features.size(), MIN_TRAINING_SAMPLES);
            return;
        }
        model.train(features, labels, 0.5, 300, 0.01);
        log.info("Trained recovery-probability model on {} historical outcomes", model.getTrainingSamples());
    }

    @Override
    public double estimate(RevenueEvent event, CauseCategory category) {
        // FRAUD_SUSPECTED is never actively pursued (see PolicyEngine.baseIntervention
        // -> ROUTE_TO_RISK_TEAM), so it never appears in trainOn()'s data -- its one-hot
        // weight sits at exactly zero forever, and the model would otherwise extrapolate
        // an arbitrary number from unrelated features instead of admitting it has never
        // seen this category. There's nothing to learn here; don't pretend otherwise.
        if (category == CauseCategory.FRAUD_SUSPECTED) {
            return AssumedRecoveryRates.forCategory(category);
        }
        if (!model.isTrained()) {
            return fallback.estimate(event, category);
        }
        return model.predict(RecoveryFeatures.build(event, category));
    }

    public boolean isTrained() {
        return model.isTrained();
    }

    public int trainingSampleCount() {
        return model.getTrainingSamples();
    }
}
