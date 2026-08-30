package com.recoup.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import com.recoup.backend.model.ActionResult;
import com.recoup.backend.model.CauseCategory;
import com.recoup.backend.model.RevenueEvent;

/** Produces plausible outcomes with zero external credentials, so the whole
 *  pipeline is runnable and demoable without a Razorpay account. Outcomes are
 *  seeded from the event's contentKey (SHA-256 -> long seed) -- NOT its row id,
 *  which is salted per batch to avoid primary-key collisions and would
 *  otherwise make "same seed" batches recover different amounts on every run.
 *  contentKey is derived from (seed, index) alone, so a batch run is fully
 *  reproducible: same seed in, same recovered-amount metrics out, every time. */
public class SimulatedPaymentGateway implements PaymentGateway {

    @Override
    public String name() {
        return "simulated";
    }

    private Random seededRandom(String contentKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(contentKey.getBytes(StandardCharsets.UTF_8));
            long seed = 0;
            for (int i = 0; i < 8; i++) {
                seed = (seed << 8) | (hash[i] & 0xff);
            }
            return new Random(seed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private ActionResult resolve(RevenueEvent event, CauseCategory category, String label) {
        Random rng = seededRandom(event.getContentKey());
        double prob = PolicyEngine.recoveryProbabilityFor(category);
        boolean success = rng.nextDouble() < prob;
        String ref = "sim_" + label + "_" + event.getId();
        if (success) {
            return new ActionResult(true, "[SIMULATED] " + label + " succeeded", event.getAmountPaise(), ref);
        }
        return new ActionResult(false, "[SIMULATED] " + label + " did not convert", 0, ref);
    }

    @Override
    public ActionResult retryPayment(RevenueEvent event, CauseCategory category) {
        return resolve(event, category, "retry");
    }

    @Override
    public ActionResult createPaymentLink(RevenueEvent event, CauseCategory category) {
        return resolve(event, category, "alt_link");
    }
}
