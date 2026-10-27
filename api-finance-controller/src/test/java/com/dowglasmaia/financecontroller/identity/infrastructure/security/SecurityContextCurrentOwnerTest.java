package com.dowglasmaia.financecontroller.identity.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dowglasmaia.financecontroller.shared.error.DomainException;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class SecurityContextCurrentOwnerTest {
    private final SecurityContextCurrentOwner owner = new SecurityContextCurrentOwner();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static JwtAuthenticationToken tokenFor(String subject) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "HS256").subject(subject)
            .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        return new JwtAuthenticationToken(jwt, List.of());
    }

    @Test
    void returnsSubjectOfAuthenticatedJwt() {
        UUID id = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(tokenFor(id.toString()));

        assertEquals(id, owner.id());
    }

    @Test
    void withoutAuthenticationIsUnauthenticated() {
        assertEquals(ErrorCode.UNAUTHENTICATED, assertThrows(DomainException.class, owner::id).code());
    }

    @Test
    void anonymousOrNonJwtAuthenticationIsUnauthenticated() {
        SecurityContextHolder.getContext().setAuthentication(
            new AnonymousAuthenticationToken("k", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        assertEquals(ErrorCode.UNAUTHENTICATED, assertThrows(DomainException.class, owner::id).code());

        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(UUID.randomUUID().toString(), "x", List.of()));
        assertEquals(ErrorCode.UNAUTHENTICATED, assertThrows(DomainException.class, owner::id).code());
    }

    @Test
    void malformedSubjectIsUnauthenticated() {
        SecurityContextHolder.getContext().setAuthentication(tokenFor("not-a-uuid"));

        assertEquals(ErrorCode.UNAUTHENTICATED, assertThrows(DomainException.class, owner::id).code());
    }
}
