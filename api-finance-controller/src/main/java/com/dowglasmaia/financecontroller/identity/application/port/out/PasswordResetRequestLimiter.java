package com.dowglasmaia.financecontroller.identity.application.port.out;

/** Limita pedidos de recuperação por e-mail e por origem; o excesso não gera e-mail (a resposta segue neutra). */
public interface PasswordResetRequestLimiter {

    /** @return {@code true} se o pedido pode seguir (e o conta); {@code false} se a janela está esgotada */
    boolean tryAcquire(String email, String origin);
}
