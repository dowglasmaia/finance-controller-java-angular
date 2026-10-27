package com.dowglasmaia.financecontroller.identity.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dowglasmaia.financecontroller.identity.domain.User;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

class JwtAccessTokenIssuerTest {
    private static final String ISSUER = "https://issuer.test";
    private final JwtConfiguration configuration = new JwtConfiguration();
    private final User user = TestFixtures.user();

    private SecretKey key(String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    private JwtAccessTokenIssuer issuer(String secret, Clock clock) {
        JwtEncoder encoder = configuration.jwtEncoder(key(secret));
        return new JwtAccessTokenIssuer(encoder, TestFixtures.properties(secret), clock);
    }

    private JwtDecoder decoder(String secret) {
        return JwtConfiguration.decoder(key(secret), ISSUER);
    }

    @Test
    void roundTripCarriesSubjectEmailIssuerAndShortLifetime() {
        Instant now = Instant.now();
        String token = issuer(TestFixtures.SECRET, Clock.fixed(now, ZoneOffset.UTC)).issue(user);

        Jwt jwt = decoder(TestFixtures.SECRET).decode(token);

        assertEquals(user.id().value().toString(), jwt.getSubject());
        assertEquals("ana@example.com", jwt.getClaimAsString("email"));
        assertEquals(ISSUER, jwt.getClaimAsString("iss"));
        assertEquals(Duration.ofMinutes(15), Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()));
        assertEquals("HS256", jwt.getHeaders().get("alg"));
    }

    @Test
    void eachTokenGetsUniqueId() {
        var issuer = issuer(TestFixtures.SECRET, Clock.systemUTC());
        var decoder = decoder(TestFixtures.SECRET);
        assertNotEquals(decoder.decode(issuer.issue(user)).getId(), decoder.decode(issuer.issue(user)).getId());
    }

    @Test
    void rejectsExpiredToken() {
        Clock past = Clock.fixed(Instant.now().minus(Duration.ofHours(1)), ZoneOffset.UTC);
        String token = issuer(TestFixtures.SECRET, past).issue(user);

        assertThrows(BadJwtException.class, () -> decoder(TestFixtures.SECRET).decode(token));
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        String token = issuer(TestFixtures.OTHER_SECRET, Clock.systemUTC()).issue(user);

        assertThrows(BadJwtException.class, () -> decoder(TestFixtures.SECRET).decode(token));
    }

    @Test
    void rejectsTamperedPayloadAndForeignIssuer() {
        String token = issuer(TestFixtures.SECRET, Clock.systemUTC()).issue(user);
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1].substring(0, parts[1].length() - 2) + "AA." + parts[2];
        assertThrows(BadJwtException.class, () -> decoder(TestFixtures.SECRET).decode(tampered));

        JwtDecoder otherIssuer = JwtConfiguration.decoder(key(TestFixtures.SECRET), "https://other.test");
        assertThrows(BadJwtException.class, () -> otherIssuer.decode(token));
    }
}
