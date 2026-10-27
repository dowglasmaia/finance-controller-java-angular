package com.dowglasmaia.financecontroller.identity.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Sessão de renovação: guarda só o hash do token. Cada token pertence a uma família; o reuso de um token já
 * rotacionado revoga a família inteira.
 */
public record RefreshSession(UUID id, UserId userId, String tokenHash, UUID familyId, Instant expiresAt, Instant revokedAt,
                             Instant createdAt) {

    public RefreshSession {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(tokenHash, "tokenHash");
        Objects.requireNonNull(familyId, "familyId");
        Objects.requireNonNull(expiresAt, "expiresAt");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public static RefreshSession start(UserId userId, String tokenHash, UUID familyId, Instant now, Instant expiresAt) {
        return new RefreshSession(UUID.randomUUID(), userId, tokenHash, familyId, expiresAt, null, now);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }
}
