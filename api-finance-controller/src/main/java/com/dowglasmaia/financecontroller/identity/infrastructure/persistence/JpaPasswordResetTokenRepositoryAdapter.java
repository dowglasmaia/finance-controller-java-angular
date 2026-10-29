package com.dowglasmaia.financecontroller.identity.infrastructure.persistence;

import com.dowglasmaia.financecontroller.identity.application.port.out.PasswordResetTokenRepository;
import com.dowglasmaia.financecontroller.identity.domain.UserId;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaPasswordResetTokenRepositoryAdapter implements PasswordResetTokenRepository {

    private final SpringDataPasswordResetTokenRepository tokens;

    JpaPasswordResetTokenRepositoryAdapter(SpringDataPasswordResetTokenRepository tokens) {
        this.tokens = tokens;
    }

    @Override
    @Transactional
    public void save(UserId userId, String tokenHash, Instant expiresAt, Instant createdAt) {
        tokens.saveAndFlush(new PasswordResetTokenEntity(UUID.randomUUID(), userId.value(), tokenHash, expiresAt, createdAt));
    }

    @Override
    @Transactional
    public void invalidateAllOf(UserId userId, Instant at) {
        tokens.invalidateAllOf(userId.value(), at);
    }

    @Override
    @Transactional
    public Optional<UserId> consume(String tokenHash, Instant now) {
        if (tokens.consume(tokenHash, now) != 1) {
            return Optional.empty();
        }
        return tokens.userIdOf(tokenHash).map(UserId::new);
    }

    @Override
    @Transactional
    public int deleteExpiredBefore(Instant before) {
        return tokens.deleteExpiredBefore(before);
    }
}
