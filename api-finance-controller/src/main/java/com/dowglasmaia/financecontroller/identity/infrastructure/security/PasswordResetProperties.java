package com.dowglasmaia.financecontroller.identity.infrastructure.security;

import com.dowglasmaia.financecontroller.identity.application.port.out.PasswordResetSettings;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Recuperação de senha (prefixo {@code app.password-reset}): {@code FC_APP_BASE_URL}, {@code FC_PASSWORD_RESET_TTL}. */
@ConfigurationProperties("app.password-reset")
public record PasswordResetProperties(
        @DefaultValue("http://localhost:4200") String appBaseUrl,
        @DefaultValue("30m") Duration ttl,
        @DefaultValue("5") int maxRequests,
        @DefaultValue("1h") Duration window,
        @DefaultValue("no-reply@finance-controller.local") String mailFrom) implements PasswordResetSettings {

    public PasswordResetProperties {
        if (appBaseUrl == null || appBaseUrl.isBlank()) {
            throw new IllegalStateException("app.password-reset.app-base-url (env FC_APP_BASE_URL) must not be blank");
        }
        if (ttl == null || ttl.isZero() || ttl.isNegative() || window == null || window.isZero() || window.isNegative()) {
            throw new IllegalStateException("app.password-reset.ttl and window must be positive durations");
        }
        if (maxRequests < 1) {
            throw new IllegalStateException("app.password-reset.max-requests must be positive");
        }
    }

    @Override
    public Duration tokenTtl() {
        return ttl;
    }
}
