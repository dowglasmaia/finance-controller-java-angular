package com.dowglasmaia.financecontroller.identity.application.port.out;

import com.dowglasmaia.financecontroller.identity.domain.RefreshSession;
import com.dowglasmaia.financecontroller.identity.domain.UserId;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshSessionRepository {

    RefreshSession save(RefreshSession session);

    Optional<RefreshSession> findByTokenHash(String tokenHash);

    /** Revoga uma sessão ainda não revogada. */
    void revoke(UUID sessionId, Instant at);

    /** Revoga todas as sessões ainda não revogadas da família. */
    void revokeFamily(UUID familyId, Instant at);

    /** Revoga todas as sessões ainda não revogadas do utilizador (senha redefinida). */
    void revokeAllOfUser(UserId userId, Instant at);
}
