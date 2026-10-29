package com.dowglasmaia.financecontroller.identity.infrastructure.persistence;

import com.dowglasmaia.financecontroller.identity.application.port.out.RefreshSessionRepository;
import com.dowglasmaia.financecontroller.identity.domain.RefreshSession;
import com.dowglasmaia.financecontroller.identity.domain.UserId;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaRefreshSessionRepositoryAdapter implements RefreshSessionRepository {

    private final SpringDataRefreshTokenRepository tokens;

    JpaRefreshSessionRepositoryAdapter(SpringDataRefreshTokenRepository tokens) {
        this.tokens = tokens;
    }

    @Override
    @Transactional
    public RefreshSession save(RefreshSession session) {
        return IdentityPersistenceMapper.toDomain(tokens.saveAndFlush(IdentityPersistenceMapper.toEntity(session)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshSession> findByTokenHash(String tokenHash) {
        return tokens.findByTokenHash(tokenHash).map(IdentityPersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public void revoke(UUID sessionId, Instant at) {
        tokens.revoke(sessionId, at);
    }

    @Override
    @Transactional
    public void revokeFamily(UUID familyId, Instant at) {
        tokens.revokeFamily(familyId, at);
    }

    @Override
    @Transactional
    public void revokeAllOfUser(UserId userId, Instant at) {
        tokens.revokeAllOfUser(userId.value(), at);
    }
}
