package com.dowglasmaia.financecontroller.identity.application.usecase;

import com.dowglasmaia.financecontroller.identity.application.port.out.RefreshSessionRepository;
import com.dowglasmaia.financecontroller.identity.application.port.out.RefreshTokenGenerator;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogoutUseCase {

    private final RefreshSessionRepository sessions;
    private final RefreshTokenGenerator tokens;
    private final Clock clock;

    LogoutUseCase(RefreshSessionRepository sessions, RefreshTokenGenerator tokens, Clock clock) {
        this.sessions = sessions;
        this.tokens = tokens;
        this.clock = clock;
    }

    /** Idempotente: token ausente ou desconhecido não é erro. */
    @Transactional
    public void execute(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        sessions.findByTokenHash(tokens.hash(refreshToken))
            .ifPresent(session -> sessions.revokeFamily(session.familyId(), clock.instant()));
    }
}
