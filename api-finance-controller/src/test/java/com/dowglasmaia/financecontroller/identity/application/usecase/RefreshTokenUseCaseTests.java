package com.dowglasmaia.financecontroller.identity.application.usecase;

import com.dowglasmaia.financecontroller.identity.domain.User;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenUseCaseTests {

    private final IdentityFakes fakes = new IdentityFakes();
    private final RefreshTokenUseCase useCase = new RefreshTokenUseCase(
        fakes.sessions, fakes.tokens, fakes.users, fakes.accessTokens, fakes.issuer, fakes.clock);

    private User user;
    private String firstToken;

    @BeforeEach
    void setUp() {
        user = fakes.existingUser("ana@example.com", "correct horse battery");
        firstToken = fakes.issuer.openNewFamily(user.id()).token();
    }

    @Test
    void rotatesTheTokenInTheSameFamilyAndRevokesTheOldSession() {
        var oldSession = fakes.sessions.findByTokenHash(fakes.tokens.hash(firstToken)).orElseThrow();
        fakes.clock.advance(Duration.ofMinutes(5));

        var result = useCase.execute(firstToken);

        assertThat(result.refreshToken()).isNotEqualTo(firstToken);
        assertThat(result.accessToken()).isEqualTo("access-for-" + user.id().value());
        assertThat(result.refreshTokenExpiresAt()).isEqualTo(fakes.clock.instant().plus(IdentityFakes.REFRESH_LIFETIME));

        var old = fakes.sessions.byId.get(oldSession.id());
        assertThat(old.isRevoked()).isTrue();
        assertThat(old.revokedAt()).isEqualTo(fakes.clock.instant());
        var next = fakes.sessions.findByTokenHash(fakes.tokens.hash(result.refreshToken())).orElseThrow();
        assertThat(next.familyId()).isEqualTo(oldSession.familyId());
        assertThat(next.isRevoked()).isFalse();
    }

    @Test
    void reusingARotatedTokenRevokesTheWholeFamilyIncludingSiblingSessions() {
        var familyId = fakes.sessions.findByTokenHash(fakes.tokens.hash(firstToken)).orElseThrow().familyId();
        var rotated = useCase.execute(firstToken);
        var sibling = fakes.issuer.open(user.id(), familyId);

        IdentityFakes.assertCode(() -> useCase.execute(firstToken), ErrorCode.UNAUTHENTICATED);

        assertThat(fakes.sessions.family(familyId)).hasSize(3).allMatch(s -> s.isRevoked());
        IdentityFakes.assertCode(() -> useCase.execute(rotated.refreshToken()), ErrorCode.UNAUTHENTICATED);
        IdentityFakes.assertCode(() -> useCase.execute(sibling.token()), ErrorCode.UNAUTHENTICATED);
    }

    @Test
    void reuseDoesNotAffectOtherFamilies() {
        var otherToken = fakes.issuer.openNewFamily(user.id()).token();
        useCase.execute(firstToken);

        IdentityFakes.assertCode(() -> useCase.execute(firstToken), ErrorCode.UNAUTHENTICATED);

        assertThat(useCase.execute(otherToken).refreshToken()).isNotBlank();
    }

    @Test
    void rejectsNullBlankAndUnknownTokens() {
        IdentityFakes.assertCode(() -> useCase.execute(null), ErrorCode.UNAUTHENTICATED);
        IdentityFakes.assertCode(() -> useCase.execute("  "), ErrorCode.UNAUTHENTICATED);
        IdentityFakes.assertCode(() -> useCase.execute("never-issued"), ErrorCode.UNAUTHENTICATED);
    }

    @Test
    void rejectsExpiredTokenWithoutRotating() {
        fakes.clock.advance(IdentityFakes.REFRESH_LIFETIME.plusSeconds(1));
        var sessionCount = fakes.sessions.byId.size();

        IdentityFakes.assertCode(() -> useCase.execute(firstToken), ErrorCode.UNAUTHENTICATED);

        assertThat(fakes.sessions.byId).hasSize(sessionCount);
    }

    @Test
    void rejectsTokenWhoseUserNoLongerExistsWithoutRotating() {
        fakes.users.byId.clear();

        IdentityFakes.assertCode(() -> useCase.execute(firstToken), ErrorCode.UNAUTHENTICATED);

        assertThat(fakes.sessions.byId).hasSize(1);
        assertThat(fakes.sessions.byId.values()).noneMatch(s -> s.isRevoked());
    }
}
