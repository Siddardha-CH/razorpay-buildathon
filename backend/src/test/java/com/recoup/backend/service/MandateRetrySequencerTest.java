package com.recoup.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

class MandateRetrySequencerTest {

    private static final ZonedDateTime NOW = ZonedDateTime.of(2026, 8, 29, 10, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void spacesRetriesOutInsteadOfSameDay() {
        assertThat(MandateRetrySequencer.nextRetryDate(0, NOW)).isEqualTo(NOW.plusDays(1));
        assertThat(MandateRetrySequencer.nextRetryDate(1, NOW)).isEqualTo(NOW.plusDays(3));
        assertThat(MandateRetrySequencer.nextRetryDate(2, NOW)).isEqualTo(NOW.plusDays(7));
    }

    @Test
    void hasRemainingRetriesOnlyForTheFirstThreeAttempts() {
        assertThat(MandateRetrySequencer.hasRemainingRetries(0)).isTrue();
        assertThat(MandateRetrySequencer.hasRemainingRetries(2)).isTrue();
        assertThat(MandateRetrySequencer.hasRemainingRetries(3)).isFalse();
        assertThat(MandateRetrySequencer.hasRemainingRetries(10)).isFalse();
    }
}
