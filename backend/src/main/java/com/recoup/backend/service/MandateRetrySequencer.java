package com.recoup.backend.service;

import java.time.ZonedDateTime;

/** Schedules UPI-mandate retry attempts on a fixed backoff cadence instead of
 *  retrying immediately in the same pass -- modeled on common UPI AutoPay
 *  retry guidance: a same-day repeat usually fails for the same reason (e.g.
 *  salary not yet credited), so spacing attempts out actually improves
 *  recovery odds instead of just spamming the bank. This is what
 *  distinguishes a failed *mandate* from a failed one-off card payment, which
 *  a customer can reasonably retry themselves in the same session. */
public final class MandateRetrySequencer {

    private static final int[] BACKOFF_DAYS = {1, 3, 7};

    private MandateRetrySequencer() {
    }

    public static boolean hasRemainingRetries(int attemptCount) {
        return attemptCount < BACKOFF_DAYS.length;
    }

    public static ZonedDateTime nextRetryDate(int attemptCount, ZonedDateTime now) {
        int index = Math.min(Math.max(attemptCount, 0), BACKOFF_DAYS.length - 1);
        return now.plusDays(BACKOFF_DAYS[index]);
    }
}
