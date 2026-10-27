package com.dowglasmaia.financecontroller.identity.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

/** Configuração do módulo {@code identity} (prefixo {@code app.identity}); o prefixo {@code cookie} é lido pelo controller. */
@ConfigurationProperties("app.identity")
public record IdentityProperties(
        @DefaultValue Jwt jwt,
        @DefaultValue("30d") Duration refreshTokenTtl,
        @DefaultValue Limiter limiter,
        @DefaultValue Cors cors,
        @DefaultValue("1MB") DataSize maxRequestBody) {

    public IdentityProperties {
        requirePositive(refreshTokenTtl, "app.identity.refresh-token-ttl");
        if (maxRequestBody.toBytes() <= 0) {
            throw new IllegalStateException("app.identity.max-request-body must be positive");
        }
    }

    /** Segredo HS256 vindo só do ambiente ({@code APP_JWT_SECRET}); o valor nunca entra em mensagens de erro. */
    public record Jwt(
            String secret,
            @DefaultValue("https://api.finance-controller.local") String issuer,
            @DefaultValue("15m") Duration accessTokenTtl) {

        public static final int MIN_SECRET_BYTES = 32;

        public Jwt {
            if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
                throw new IllegalStateException(
                    "app.identity.jwt.secret (env APP_JWT_SECRET) is required and must have at least "
                        + MIN_SECRET_BYTES + " bytes");
            }
            if (issuer == null || issuer.isBlank()) {
                throw new IllegalStateException("app.identity.jwt.issuer must not be blank");
            }
            requirePositive(accessTokenTtl, "app.identity.jwt.access-token-ttl");
        }

        @Override
        public String toString() {
            return "Jwt[issuer=" + issuer + ", accessTokenTtl=" + accessTokenTtl + "]";
        }
    }

    public record Limiter(
            @DefaultValue("5") int maxFailures,
            @DefaultValue("15m") Duration window,
            @DefaultValue("10000") int maxTrackedKeys) {

        public Limiter {
            if (maxFailures < 1 || maxTrackedKeys < 1) {
                throw new IllegalStateException("app.identity.limiter.max-failures and max-tracked-keys must be positive");
            }
            requirePositive(window, "app.identity.limiter.window");
        }
    }

    /** Origens explícitas do frontend; vazio nega todas. O curinga é recusado porque /auth usa credenciais. */
    public record Cors(@DefaultValue List<String> allowedOrigins) {

        public Cors {
            allowedOrigins = allowedOrigins.stream().map(String::strip).filter(o -> !o.isEmpty()).toList();
            if (allowedOrigins.stream().anyMatch(o -> o.contains("*"))) {
                throw new IllegalStateException("app.identity.cors.allowed-origins must list explicit origins, not wildcards");
            }
        }
    }

    private static void requirePositive(Duration value, String key) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalStateException(key + " must be a positive duration");
        }
    }
}
