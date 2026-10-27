package com.dowglasmaia.financecontroller.identity.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class SecureRandomRefreshTokenGeneratorTest {
    private final SecureRandomRefreshTokenGenerator generator =
        new SecureRandomRefreshTokenGenerator(TestFixtures.properties(TestFixtures.SECRET));

    @Test
    void tokenHas32BytesOfBase64UrlWithoutPadding() {
        String token = generator.newToken();

        assertTrue(Pattern.matches("[A-Za-z0-9_-]{43}", token));
        assertEquals(32, Base64.getUrlDecoder().decode(token).length);
    }

    @Test
    void tokensAreUnique() {
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            tokens.add(generator.newToken());
        }
        assertEquals(1000, tokens.size());
    }

    @Test
    void hashIsStableLowercaseSha256HexAndNotTheToken() {
        String token = generator.newToken();

        assertEquals(generator.hash(token), generator.hash(token));
        assertTrue(Pattern.matches("[0-9a-f]{64}", generator.hash(token)));
        assertNotEquals(token, generator.hash(token));
        assertNotEquals(generator.hash(token), generator.hash(generator.newToken()));
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", generator.hash("abc"));
    }

    @Test
    void lifetimeComesFromConfiguration() {
        assertEquals(Duration.ofDays(30), generator.lifetime());
    }
}
