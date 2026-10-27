package com.dowglasmaia.financecontroller.identity.infrastructure.persistence;

import com.dowglasmaia.financecontroller.identity.domain.Email;
import com.dowglasmaia.financecontroller.identity.domain.RefreshSession;
import com.dowglasmaia.financecontroller.identity.domain.User;
import com.dowglasmaia.financecontroller.identity.domain.UserId;

final class IdentityPersistenceMapper {

    private IdentityPersistenceMapper() {
    }

    static UserEntity toEntity(User user) {
        return new UserEntity(user.id().value(), user.email().value(), user.passwordHash(), user.createdAt());
    }

    static User toDomain(UserEntity entity) {
        return new User(new UserId(entity.getId()), new Email(entity.getEmail()), entity.getPasswordHash(),
                entity.getCreatedAt());
    }

    static RefreshTokenEntity toEntity(RefreshSession session) {
        return new RefreshTokenEntity(session.id(), session.userId().value(), session.tokenHash(), session.familyId(),
                session.expiresAt(), session.revokedAt(), session.createdAt());
    }

    static RefreshSession toDomain(RefreshTokenEntity entity) {
        return new RefreshSession(entity.getId(), new UserId(entity.getUserId()), entity.getTokenHash(),
                entity.getFamilyId(), entity.getExpiresAt(), entity.getRevokedAt(), entity.getCreatedAt());
    }
}
