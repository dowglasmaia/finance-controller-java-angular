package com.dowglasmaia.financecontroller.identity.application.usecase;

import com.dowglasmaia.financecontroller.identity.application.port.out.AccessTokenIssuer;
import com.dowglasmaia.financecontroller.identity.application.port.out.LoginAttemptLimiter;
import com.dowglasmaia.financecontroller.identity.application.port.out.PasswordHasher;
import com.dowglasmaia.financecontroller.identity.application.port.out.RefreshSessionRepository;
import com.dowglasmaia.financecontroller.identity.application.port.out.RefreshTokenGenerator;
import com.dowglasmaia.financecontroller.identity.application.port.out.UserRepository;
import com.dowglasmaia.financecontroller.identity.domain.Email;
import com.dowglasmaia.financecontroller.identity.domain.RefreshSession;
import com.dowglasmaia.financecontroller.identity.domain.User;
import com.dowglasmaia.financecontroller.identity.domain.UserId;
import com.dowglasmaia.financecontroller.shared.error.DomainException;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Fakes em memória das portas de saída e fixture partilhada pelos testes dos casos de uso (sem Spring nem Docker). */
final class IdentityFakes {

    static final Instant NOW = Instant.parse("2030-01-15T10:00:00Z");
    static final Duration REFRESH_LIFETIME = Duration.ofDays(30);
    static final String ORIGIN = "198.51.100.7";

    final MutableClock clock = new MutableClock(NOW);
    final InMemoryUsers users = new InMemoryUsers();
    final InMemorySessions sessions = new InMemorySessions();
    final FakeHasher hasher = new FakeHasher();
    final FakeTokens tokens = new FakeTokens();
    final FakeAccessTokens accessTokens = new FakeAccessTokens();
    final FakeLimiter limiter = new FakeLimiter();
    final RefreshSessionIssuer issuer = new RefreshSessionIssuer(sessions, tokens, clock);

    User existingUser(String email, String rawPassword) {
        return users.save(new User(UserId.generate(), Email.of(email), hasher.hash(rawPassword), NOW));
    }

    static void assertCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(DomainException.class, e -> assertThat(e.code()).isEqualTo(expected));
    }

    static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    static final class InMemoryUsers implements UserRepository {
        final Map<UserId, User> byId = new HashMap<>();

        @Override
        public Optional<User> findByEmail(Email email) {
            return byId.values().stream().filter(u -> u.email().equals(email)).findFirst();
        }

        @Override
        public Optional<User> findById(UserId id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public boolean existsByEmail(Email email) {
            return findByEmail(email).isPresent();
        }

        @Override
        public User save(User user) {
            byId.put(user.id(), user);
            return user;
        }
    }

    static final class InMemorySessions implements RefreshSessionRepository {
        final Map<UUID, RefreshSession> byId = new HashMap<>();

        @Override
        public RefreshSession save(RefreshSession session) {
            byId.put(session.id(), session);
            return session;
        }

        @Override
        public Optional<RefreshSession> findByTokenHash(String tokenHash) {
            return byId.values().stream().filter(s -> s.tokenHash().equals(tokenHash)).findFirst();
        }

        @Override
        public void revoke(UUID sessionId, Instant at) {
            var s = byId.get(sessionId);
            if (s != null && !s.isRevoked()) {
                byId.put(sessionId, withRevokedAt(s, at));
            }
        }

        @Override
        public void revokeFamily(UUID familyId, Instant at) {
            List.copyOf(byId.values()).stream()
                .filter(s -> s.familyId().equals(familyId) && !s.isRevoked())
                .forEach(s -> byId.put(s.id(), withRevokedAt(s, at)));
        }

        List<RefreshSession> family(UUID familyId) {
            return byId.values().stream().filter(s -> s.familyId().equals(familyId)).toList();
        }

        private static RefreshSession withRevokedAt(RefreshSession s, Instant at) {
            return new RefreshSession(s.id(), s.userId(), s.tokenHash(), s.familyId(), s.expiresAt(), at, s.createdAt());
        }
    }

    static final class FakeHasher implements PasswordHasher {
        @Override
        public String hash(String rawPassword) {
            return "hashed:" + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String passwordHash) {
            return hash(rawPassword).equals(passwordHash);
        }
    }

    static final class FakeTokens implements RefreshTokenGenerator {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public String newToken() {
            return "refresh-token-" + counter.incrementAndGet();
        }

        @Override
        public String hash(String token) {
            return "h(" + token + ")";
        }

        @Override
        public Duration lifetime() {
            return REFRESH_LIFETIME;
        }
    }

    static final class FakeAccessTokens implements AccessTokenIssuer {
        @Override
        public String issue(User user) {
            return "access-for-" + user.id().value();
        }
    }

    static final class FakeLimiter implements LoginAttemptLimiter {
        final Map<String, Integer> failures = new HashMap<>();
        final Set<String> blocked = new HashSet<>();
        final List<String> resets = new ArrayList<>();

        @Override
        public boolean isBlocked(String email, String origin) {
            return blocked.contains(email);
        }

        @Override
        public void recordFailure(String email, String origin) {
            failures.merge(email, 1, Integer::sum);
        }

        @Override
        public void reset(String email, String origin) {
            resets.add(email);
            failures.remove(email);
        }
    }
}
