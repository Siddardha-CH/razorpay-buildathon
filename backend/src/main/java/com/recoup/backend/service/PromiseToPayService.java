package com.recoup.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.recoup.backend.model.CauseCategory;
import com.recoup.backend.model.Promise;
import com.recoup.backend.model.RevenueEvent;

/** Promise-to-pay tracking for human-escalated B2B receivables.
 *
 *  A collections call/chat can end in a verbal commitment ("I'll pay by the
 *  15th"). We simulate whether that commitment materializes so the pipeline
 *  has a concrete, auditable terminal state instead of leaving escalated
 *  cases open forever. */
@Service
public class PromiseToPayService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");

    private static final Map<CauseCategory, Double> KEEP_RATE = new EnumMap<>(CauseCategory.class);
    private static final Map<CauseCategory, Double> PROMISE_LIKELIHOOD = new EnumMap<>(CauseCategory.class);

    static {
        KEEP_RATE.put(CauseCategory.RECEIVABLE_ESCALATION_STAGE, 0.60);
        KEEP_RATE.put(CauseCategory.RECEIVABLE_LEGAL_STAGE, 0.30);
        PROMISE_LIKELIHOOD.put(CauseCategory.RECEIVABLE_ESCALATION_STAGE, 0.55);
        PROMISE_LIKELIHOOD.put(CauseCategory.RECEIVABLE_LEGAL_STAGE, 0.35);
    }

    private Random seededRandom(String contentKey, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((contentKey + ":" + salt).getBytes(StandardCharsets.UTF_8));
            long seed = 0;
            for (int i = 0; i < 8; i++) {
                seed = (seed << 8) | (hash[i] & 0xff);
            }
            return new Random(seed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public Promise maybeRecordPromise(RevenueEvent event, CauseCategory category, ZonedDateTime now) {
        Random rng = seededRandom(event.getContentKey(), "ptp");
        double likelihood = PROMISE_LIKELIHOOD.getOrDefault(category, 0.0);
        if (rng.nextDouble() >= likelihood) {
            return new Promise(0, null, null);
        }
        int daysAhead = 3 + rng.nextInt(12);
        ZonedDateTime promisedDate = now.plusDays(daysAhead);
        boolean kept = rng.nextDouble() < KEEP_RATE.getOrDefault(category, 0.4);
        return new Promise(event.getAmountPaise(), ISO.format(promisedDate.withZoneSameInstant(ZoneOffset.ofHoursMinutes(5, 30))), kept);
    }
}
