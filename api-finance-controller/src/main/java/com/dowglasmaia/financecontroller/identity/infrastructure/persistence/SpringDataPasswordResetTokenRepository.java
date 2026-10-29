package com.dowglasmaia.financecontroller.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface SpringDataPasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, UUID> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update PasswordResetToken t set t.usedAt = :at where t.userId = :userId and t.usedAt is null")
    int invalidateAllOf(@Param("userId") UUID userId, @Param("at") Instant at);

    /** Atómico: só um dos pedidos simultâneos com o mesmo token muda {@code usedAt} de nulo. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update PasswordResetToken t set t.usedAt = :now "
        + "where t.tokenHash = :hash and t.usedAt is null and t.expiresAt > :now")
    int consume(@Param("hash") String hash, @Param("now") Instant now);

    @Query("select t.userId from PasswordResetToken t where t.tokenHash = :hash")
    Optional<UUID> userIdOf(@Param("hash") String hash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from PasswordResetToken t where t.expiresAt < :before")
    int deleteExpiredBefore(@Param("before") Instant before);
}
