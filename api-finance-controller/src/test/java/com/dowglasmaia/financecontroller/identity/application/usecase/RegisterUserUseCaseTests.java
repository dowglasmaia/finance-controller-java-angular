package com.dowglasmaia.financecontroller.identity.application.usecase;

import com.dowglasmaia.financecontroller.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterUserUseCaseTests {

    private final java.util.List<Object> published = new java.util.ArrayList<>();
    private final IdentityFakes fakes = new IdentityFakes();
    private final RegisterUserUseCase useCase = new RegisterUserUseCase(
        fakes.users, fakes.hasher, fakes.accessTokens, fakes.issuer, fakes.clock, event -> published.add(event));

    @Test
    void registersUserWithNormalizedEmailHashedPasswordAndNewRefreshFamily() {
        var result = useCase.execute(new RegisterUserCommand("  Ana.Teste@Example.COM ", "correct horse battery"));

        assertThat(result.user().email().value()).isEqualTo("ana.teste@example.com");
        assertThat(result.user().passwordHash()).isEqualTo("hashed:correct horse battery").isNotEqualTo("correct horse battery");
        assertThat(result.user().createdAt()).isEqualTo(IdentityFakes.NOW);
        assertThat(result.accessToken()).isEqualTo("access-for-" + result.user().id().value());
        assertThat(result.refreshTokenExpiresAt()).isEqualTo(IdentityFakes.NOW.plus(IdentityFakes.REFRESH_LIFETIME));
        assertThat(fakes.users.byId).containsKey(result.user().id());
        assertThat(published).containsExactly(new com.dowglasmaia.financecontroller.identity.UserRegistered(result.user().id().value()));

        var session = fakes.sessions.findByTokenHash(fakes.tokens.hash(result.refreshToken())).orElseThrow();
        assertThat(session.userId()).isEqualTo(result.user().id());
        assertThat(session.isRevoked()).isFalse();
        assertThat(session.expiresAt()).isEqualTo(result.refreshTokenExpiresAt());
        assertThat(session.tokenHash()).isNotEqualTo(result.refreshToken());
    }

    @Test
    void rejectsDuplicatedEmailInAnyCase() {
        fakes.existingUser("ana@example.com", "correct horse battery");

        IdentityFakes.assertCode(
            () -> useCase.execute(new RegisterUserCommand("ANA@Example.com", "another good password")),
            ErrorCode.EMAIL_ALREADY_REGISTERED);
        assertThat(fakes.users.byId).hasSize(1);
        assertThat(fakes.sessions.byId).isEmpty();
    }

    @Test
    void rejectsInvalidEmail() {
        IdentityFakes.assertCode(
            () -> useCase.execute(new RegisterUserCommand("not-an-email", "correct horse battery")), ErrorCode.VALIDATION_FAILED);
        IdentityFakes.assertCode(
            () -> useCase.execute(new RegisterUserCommand(null, "correct horse battery")), ErrorCode.VALIDATION_FAILED);
        assertThat(fakes.users.byId).isEmpty();
    }

    @Test
    void rejectsWeakPassword() {
        IdentityFakes.assertCode(
            () -> useCase.execute(new RegisterUserCommand("ana@example.com", "short")), ErrorCode.VALIDATION_FAILED);
        IdentityFakes.assertCode(
            () -> useCase.execute(new RegisterUserCommand("ana@example.com", "password")), ErrorCode.VALIDATION_FAILED);
        assertThat(fakes.users.byId).isEmpty();
        assertThat(fakes.sessions.byId).isEmpty();
    }
}
