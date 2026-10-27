package com.dowglasmaia.financecontroller.identity.application.port.out;

/** Limita tentativas falhas de entrada por conta (e-mail normalizado) e por origem, de forma independente. */
public interface LoginAttemptLimiter {

    boolean isBlocked(String email, String origin);

    void recordFailure(String email, String origin);

    void reset(String email, String origin);
}
