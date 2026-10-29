package com.dowglasmaia.financecontroller.identity.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Token de recuperação de senha: 256 bits aleatórios, URL-safe. Só o hash SHA-256 (hexadecimal) é guardado; o valor
 * em claro existe apenas no e-mail enviado e na memória.
 */
public record ResetToken(String value, String hash) {

    private static final SecureRandom RANDOM = new SecureRandom();
    public static final int MAX_LENGTH = 200;

    public static ResetToken generate() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new ResetToken(value, hash(value));
    }

    public static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    @Override
    public String toString() {
        return "ResetToken[redacted]";
    }
}
