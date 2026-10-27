package com.dowglasmaia.financecontroller.identity.application.usecase;

import com.dowglasmaia.financecontroller.identity.application.port.out.RefreshSessionRepository;
import com.dowglasmaia.financecontroller.identity.application.port.out.RefreshTokenGenerator;
import com.dowglasmaia.financecontroller.identity.domain.RefreshSession;
import com.dowglasmaia.financecontroller.identity.domain.UserId;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Abre uma sessão de renovação (nova família ou rotação dentro da família) e devolve o token em claro uma única vez. */
@Component
class RefreshSessionIssuer {

    record Issued(String token, Instant expiresAt) {
    }

    private final RefreshSessionRepository sessions;
    private final RefreshTokenGenerator tokens;
    private final Clock clock;

    RefreshSessionIssuer(RefreshSessionRepository sessions, RefreshTokenGenerator tokens, Clock clock) {
        this.sessions = sessions;
        this.tokens = tokens;
        this.clock = clock;
    }

    Issued openNewFamily(UserId userId) {
        return open(userId, UUID.randomUUID());
    }

    Issued open(UserId userId, UUID familyId) {
        var token = tokens.newToken();
        var now = clock.instant();
        var expiresAt = now.plus(tokens.lifetime());
        sessions.save(RefreshSession.start(userId, tokens.hash(token), familyId, now, expiresAt));
        return new Issued(token, expiresAt);
    }
}
