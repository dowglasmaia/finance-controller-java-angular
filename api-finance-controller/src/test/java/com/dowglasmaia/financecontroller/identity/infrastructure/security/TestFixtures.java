package com.dowglasmaia.financecontroller.identity.infrastructure.security;

import com.dowglasmaia.financecontroller.identity.domain.Email;
import com.dowglasmaia.financecontroller.identity.domain.User;
import com.dowglasmaia.financecontroller.identity.domain.UserId;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.util.unit.DataSize;

/** Dados fictícios compartilhados pelos testes de segurança. */
final class TestFixtures {
    static final String SECRET = "test-only-fictitious-secret-0123456789abcdef";
    static final String OTHER_SECRET = "another-fictitious-secret-0123456789abcdef!";

    private TestFixtures() {
    }

    static IdentityProperties properties(String secret) {
        return new IdentityProperties(
            new IdentityProperties.Jwt(secret, "https://issuer.test", Duration.ofMinutes(15)),
            Duration.ofDays(30),
            new IdentityProperties.Limiter(5, Duration.ofMinutes(15), 100),
            new IdentityProperties.Cors(List.of("http://localhost:4200")),
            DataSize.ofMegabytes(1));
    }

    static User user() {
        return new User(UserId.generate(), Email.of("ana@example.com"), "hash", Instant.parse("2026-01-01T00:00:00Z"));
    }
}
