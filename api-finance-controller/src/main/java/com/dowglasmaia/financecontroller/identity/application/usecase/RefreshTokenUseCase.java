package com.dowglasmaia.financecontroller.identity.application.usecase;

import com.dowglasmaia.financecontroller.identity.application.port.out.AccessTokenIssuer;
import com.dowglasmaia.financecontroller.identity.application.port.out.RefreshSessionRepository;
import com.dowglasmaia.financecontroller.identity.application.port.out.RefreshTokenGenerator;
import com.dowglasmaia.financecontroller.identity.application.port.out.UserRepository;
import com.dowglasmaia.financecontroller.shared.error.DomainException;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenUseCase {

    private final RefreshSessionRepository sessions;
    private final RefreshTokenGenerator tokens;
    private final UserRepository users;
    private final AccessTokenIssuer accessTokens;
    private final RefreshSessionIssuer refreshSessions;
    private final Clock clock;

    RefreshTokenUseCase(RefreshSessionRepository sessions, RefreshTokenGenerator tokens, UserRepository users,
                        AccessTokenIssuer accessTokens, RefreshSessionIssuer refreshSessions, Clock clock) {
        this.sessions = sessions;
        this.tokens = tokens;
        this.users = users;
        this.accessTokens = accessTokens;
        this.refreshSessions = refreshSessions;
        this.clock = clock;
    }

    /** A revogação da família por reuso tem de ser confirmada mesmo com a exceção, por isso não há rollback. */
    @Transactional(noRollbackFor = DomainException.class)
    public TokenRefreshResult execute(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw unauthenticated();
        }
        var now = clock.instant();
        var session = sessions.findByTokenHash(tokens.hash(refreshToken)).orElseThrow(RefreshTokenUseCase::unauthenticated);
        if (session.isRevoked()) {
            sessions.revokeFamily(session.familyId(), now);
            throw unauthenticated();
        }
        if (session.isExpired(now)) {
            throw unauthenticated();
        }
        var user = users.findById(session.userId()).orElseThrow(RefreshTokenUseCase::unauthenticated);
        sessions.revoke(session.id(), now);
        var next = refreshSessions.open(session.userId(), session.familyId());
        return new TokenRefreshResult(accessTokens.issue(user), next.token(), next.expiresAt());
    }

    private static DomainException unauthenticated() {
        return new DomainException(ErrorCode.UNAUTHENTICATED);
    }
}
