package com.dowglasmaia.financecontroller.identity.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshSessionTests {

    private static final Instant NOW = Instant.parse("2030-01-15T10:00:00Z");

    private final RefreshSession session = RefreshSession.start(
        UserId.generate(), "hash-1", UUID.randomUUID(), NOW, NOW.plus(Duration.ofDays(30)));

    @Test
    void startsActive() {
        assertThat(session.isRevoked()).isFalse();
        assertThat(session.revokedAt()).isNull();
        assertThat(session.createdAt()).isEqualTo(NOW);
        assertThat(session.isExpired(NOW)).isFalse();
    }

    @Test
    void expiresExactlyAtTheExpirationInstant() {
        assertThat(session.isExpired(session.expiresAt().minusNanos(1))).isFalse();
        assertThat(session.isExpired(session.expiresAt())).isTrue();
        assertThat(session.isExpired(session.expiresAt().plusSeconds(1))).isTrue();
    }

    @Test
    void isRevokedWhenRevokedAtIsSet() {
        var revoked = new RefreshSession(session.id(), session.userId(), session.tokenHash(), session.familyId(),
            session.expiresAt(), NOW.plusSeconds(10), session.createdAt());

        assertThat(revoked.isRevoked()).isTrue();
    }

    @Test
    void requiresMandatoryFields() {
        assertThatThrownBy(() -> RefreshSession.start(null, "h", UUID.randomUUID(), NOW, NOW))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> RefreshSession.start(UserId.generate(), null, UUID.randomUUID(), NOW, NOW))
            .isInstanceOf(NullPointerException.class);
    }
}
