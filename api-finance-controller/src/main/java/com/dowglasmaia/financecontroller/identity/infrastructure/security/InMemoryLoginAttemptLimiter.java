package com.dowglasmaia.financecontroller.identity.infrastructure.security;

import com.dowglasmaia.financecontroller.identity.application.port.out.LoginAttemptLimiter;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Janela deslizante em memória, por conta (e-mail normalizado) e por origem, de forma independente. O estado perde-se
 * ao reiniciar e não é partilhado entre instâncias (aceitável para uma única instância; ver ADR-012). A memória é
 * limitada: ao atingir {@code maxTrackedKeys} as entradas expiradas são removidas e, se continuar cheio, novas chaves
 * deixam de ser registadas até haver espaço.
 */
@Component
class InMemoryLoginAttemptLimiter implements LoginAttemptLimiter {

    private final ConcurrentHashMap<String, Failures> entries = new ConcurrentHashMap<>();
    private final IdentityProperties.Limiter config;
    private final Clock clock;

    @Autowired
    InMemoryLoginAttemptLimiter(IdentityProperties properties, Clock clock) {
        this(properties.limiter(), clock);
    }

    InMemoryLoginAttemptLimiter(IdentityProperties.Limiter config, Clock clock) {
        this.config = config;
        this.clock = clock;
    }

    @Override
    public boolean isBlocked(String email, String origin) {
        Instant now = clock.instant();
        return blocked(accountKey(email), now) || blocked(originKey(origin), now);
    }

    @Override
    public void recordFailure(String email, String origin) {
        Instant now = clock.instant();
        record(accountKey(email), now);
        record(originKey(origin), now);
    }

    @Override
    public void reset(String email, String origin) {
        entries.remove(accountKey(email));
        entries.remove(originKey(origin));
    }

    private boolean blocked(String key, Instant now) {
        Failures failures = entries.get(key);
        if (failures == null) {
            return false;
        }
        Instant cutoff = now.minus(config.window());
        if (failures.countSince(cutoff) >= config.maxFailures()) {
            return true;
        }
        entries.computeIfPresent(key, (k, f) -> f.isEmptyAfter(cutoff) ? null : f);
        return false;
    }

    private void record(String key, Instant now) {
        Instant cutoff = now.minus(config.window());
        if (entries.size() >= config.maxTrackedKeys() && !entries.containsKey(key)) {
            entries.entrySet().removeIf(e -> e.getValue().isEmptyAfter(cutoff));
            if (entries.size() >= config.maxTrackedKeys()) {
                return;
            }
        }
        entries.compute(key, (k, f) -> {
            Failures failures = f == null ? new Failures(config.maxFailures()) : f;
            failures.add(now, cutoff);
            return failures;
        });
    }

    private static String accountKey(String email) {
        return "account:" + (email == null ? "" : email.strip().toLowerCase(Locale.ROOT));
    }

    private static String originKey(String origin) {
        return "origin:" + (origin == null ? "" : origin.strip());
    }

    /** Instantes das falhas recentes; no máximo {@code capacity} (as mais novas bastam para decidir o bloqueio). */
    private static final class Failures {
        private final Deque<Instant> instants = new ArrayDeque<>();
        private final int capacity;

        Failures(int capacity) {
            this.capacity = capacity;
        }

        synchronized void add(Instant now, Instant cutoff) {
            prune(cutoff);
            instants.addLast(now);
            while (instants.size() > capacity) {
                instants.removeFirst();
            }
        }

        synchronized int countSince(Instant cutoff) {
            prune(cutoff);
            return instants.size();
        }

        synchronized boolean isEmptyAfter(Instant cutoff) {
            prune(cutoff);
            return instants.isEmpty();
        }

        private void prune(Instant cutoff) {
            while (!instants.isEmpty() && !instants.peekFirst().isAfter(cutoff)) {
                instants.removeFirst();
            }
        }
    }
}
