package com.dowglasmaia.financecontroller.identity.application.port.out;

import com.dowglasmaia.financecontroller.identity.domain.UserId;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository {

    void save(UserId userId, String tokenHash, Instant expiresAt, Instant createdAt);

    /** Marca como usados todos os tokens ainda válidos da conta (um novo pedido invalida os anteriores). */
    void invalidateAllOf(UserId userId, Instant at);

    /**
     * Consome o token de uma vez: só quem muda {@code used_at} de nulo para um valor (token existente, não usado e
     * não vencido) recebe a conta; dois pedidos simultâneos com o mesmo token têm um só vencedor.
     */
    Optional<UserId> consume(String tokenHash, Instant now);

    /** Remove os tokens vencidos antes de {@code before}. */
    int deleteExpiredBefore(Instant before);
}
