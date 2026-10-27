package com.dowglasmaia.financecontroller.identity.infrastructure.security;

import com.dowglasmaia.financecontroller.identity.application.port.out.PasswordHasher;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * BCrypt sobre o resumo SHA-256 da senha: a política aceita até 128 caracteres e o BCrypt só considera 72 bytes
 * (o Spring Security recente recusa mais que isso). A senha nunca é registada nem devolvida.
 */
@Component
class BcryptPasswordHasher implements PasswordHasher {

    private final PasswordEncoder encoder;

    BcryptPasswordHasher(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(prehash(rawPassword));
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        if (rawPassword == null || passwordHash == null || passwordHash.isBlank()) {
            return false;
        }
        return encoder.matches(prehash(rawPassword), passwordHash);
    }

    private static String prehash(String rawPassword) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
