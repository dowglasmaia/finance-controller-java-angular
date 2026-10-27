package com.dowglasmaia.financecontroller.identity.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class InMemoryLoginAttemptLimiterTest {
    private static final String ORIGIN = "203.0.113.7";

    private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
    private final InMemoryLoginAttemptLimiter limiter = new InMemoryLoginAttemptLimiter(
        new IdentityProperties.Limiter(5, Duration.ofMinutes(15), 100), clock);

    private void fail(int times, String email, String origin) {
        for (int i = 0; i < times; i++) {
            limiter.recordFailure(email, origin);
        }
    }

    @Test
    void blocksAfterConfiguredFailures() {
        fail(4, "ana@example.com", ORIGIN);
        assertFalse(limiter.isBlocked("ana@example.com", ORIGIN));

        limiter.recordFailure("ana@example.com", ORIGIN);
        assertTrue(limiter.isBlocked("ana@example.com", ORIGIN));
    }

    @Test
    void windowExpiresAndEntriesAreForgotten() {
        fail(5, "ana@example.com", ORIGIN);
        assertTrue(limiter.isBlocked("ana@example.com", ORIGIN));

        clock.advance(Duration.ofMinutes(15).plusSeconds(1));

        assertFalse(limiter.isBlocked("ana@example.com", ORIGIN));
    }

    @Test
    void failuresOutsideTheWindowDoNotCount() {
        fail(4, "ana@example.com", ORIGIN);
        clock.advance(Duration.ofMinutes(16));
        fail(4, "ana@example.com", ORIGIN);

        assertFalse(limiter.isBlocked("ana@example.com", ORIGIN));
    }

    @Test
    void successResetsBoth() {
        fail(5, "ana@example.com", ORIGIN);
        limiter.reset("ana@example.com", ORIGIN);

        assertFalse(limiter.isBlocked("ana@example.com", ORIGIN));
    }

    @Test
    void accountAndOriginAreIndependent() {
        for (int i = 0; i < 5; i++) {
            limiter.recordFailure("ana@example.com", "198.51.100." + i);
        }
        assertTrue(limiter.isBlocked("ana@example.com", "198.51.100.200"));
        assertFalse(limiter.isBlocked("bia@example.com", "198.51.100.0"));

        for (int i = 0; i < 5; i++) {
            limiter.recordFailure("user" + i + "@example.com", ORIGIN);
        }
        assertTrue(limiter.isBlocked("novo@example.com", ORIGIN));
        assertFalse(limiter.isBlocked("novo@example.com", "192.0.2.1"));
    }

    @Test
    void emailIsNormalized() {
        fail(5, "  ANA@Example.com ", ORIGIN);

        assertTrue(limiter.isBlocked("ana@example.com", "192.0.2.1"));
    }

    @Test
    void memoryIsBounded() {
        var small = new InMemoryLoginAttemptLimiter(new IdentityProperties.Limiter(1, Duration.ofMinutes(15), 4), clock);
        for (int i = 0; i < 50; i++) {
            small.recordFailure("u" + i + "@example.com", "10.0.0." + i);
        }
        assertTrue(small.isBlocked("u0@example.com", "10.9.9.9"));
        assertFalse(small.isBlocked("u49@example.com", "10.9.9.9"));
    }

    @Test
    void isThreadSafe() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        for (int i = 0; i < 800; i++) {
            pool.submit(() -> limiter.recordFailure("ana@example.com", ORIGIN));
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        assertTrue(limiter.isBlocked("ana@example.com", ORIGIN));
    }

    private static final class MutableClock extends Clock {
        private volatile Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
