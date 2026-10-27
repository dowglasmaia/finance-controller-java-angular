package com.dowglasmaia.financecontroller.identity.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class BcryptPasswordHasherTest {
    private final BcryptPasswordHasher hasher = new BcryptPasswordHasher(new BCryptPasswordEncoder(4));

    @Test
    void hashesAndVerifiesWithoutKeepingThePassword() {
        String hash = hasher.hash("correct horse battery");

        assertTrue(hash.startsWith("$2"));
        assertFalse(hash.contains("correct horse"));
        assertTrue(hasher.matches("correct horse battery", hash));
        assertFalse(hasher.matches("wrong horse battery", hash));
    }

    @Test
    void hashesAreSalted() {
        assertNotEquals(hasher.hash("same password 1"), hasher.hash("same password 1"));
    }

    @Test
    void acceptsLongPasswordsUpToThePolicyLimitAndDistinguishesTheTail() {
        String longPassword = "a".repeat(127) + "b";
        String hash = hasher.hash(longPassword);

        assertTrue(hasher.matches(longPassword, hash));
        assertFalse(hasher.matches("a".repeat(127) + "c", hash));
    }

    @Test
    void invalidInputNeverMatches() {
        assertFalse(hasher.matches(null, "$2a$04$abc"));
        assertFalse(hasher.matches("x", null));
        assertFalse(hasher.matches("x", ""));
    }
}
