package com.dowglasmaia.financecontroller.identity.infrastructure.security;

import com.dowglasmaia.financecontroller.identity.application.port.out.RefreshTokenGenerator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/** 32 bytes de {@link SecureRandom} em Base64URL; só o hash SHA-256 (hex) é guardado. */
@Component
class SecureRandomRefreshTokenGenerator implements RefreshTokenGenerator {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom random = new SecureRandom();
    private final Duration lifetime;

    SecureRandomRefreshTokenGenerator(IdentityProperties properties) {
        this.lifetime = properties.refreshTokenTtl();
    }

    @Override
    public String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    @Override
    public Duration lifetime() {
        return lifetime;
    }
}
