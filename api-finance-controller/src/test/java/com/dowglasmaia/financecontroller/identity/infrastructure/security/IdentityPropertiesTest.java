package com.dowglasmaia.financecontroller.identity.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class IdentityPropertiesTest {

    @Test
    void rejectsMissingOrShortSecretWithoutEchoingIt() {
        assertThrows(IllegalStateException.class, () -> new IdentityProperties.Jwt(null, "https://i.test", Duration.ofMinutes(15)));
        assertThrows(IllegalStateException.class, () -> new IdentityProperties.Jwt("", "https://i.test", Duration.ofMinutes(15)));
        var ex = assertThrows(IllegalStateException.class,
            () -> new IdentityProperties.Jwt("short-secret", "https://i.test", Duration.ofMinutes(15)));
        assertFalse(ex.getMessage().contains("short-secret"));
        assertTrue(ex.getMessage().contains("APP_JWT_SECRET"));
    }

    @Test
    void acceptsSecretOfAtLeast32Bytes() {
        new IdentityProperties.Jwt("x".repeat(32), "https://i.test", Duration.ofMinutes(15));
    }

    @Test
    void secretIsNotPartOfToString() {
        var jwt = new IdentityProperties.Jwt(TestFixtures.SECRET, "https://i.test", Duration.ofMinutes(15));
        assertFalse(jwt.toString().contains(TestFixtures.SECRET));
    }

    @Test
    void rejectsWildcardOrigin() {
        assertThrows(IllegalStateException.class, () -> new IdentityProperties.Cors(List.of("*")));
    }
}
