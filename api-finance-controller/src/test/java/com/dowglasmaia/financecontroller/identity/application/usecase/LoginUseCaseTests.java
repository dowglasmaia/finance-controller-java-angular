package com.dowglasmaia.financecontroller.identity.application.usecase;

import com.dowglasmaia.financecontroller.shared.error.DomainException;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class LoginUseCaseTests {

    private static final String PASSWORD = "correct horse battery";

    private final IdentityFakes fakes = new IdentityFakes();
    private final LoginUseCase useCase = new LoginUseCase(
        fakes.users, fakes.hasher, fakes.accessTokens, fakes.issuer, fakes.limiter);

    @Test
    void logsInAndOpensNewRefreshFamilyResettingTheLimiter() {
        var user = fakes.existingUser("ana@example.com", PASSWORD);
        fakes.limiter.failures.put("ana@example.com", 2);

        var result = useCase.execute(new LoginCommand(" ANA@example.com ", PASSWORD, IdentityFakes.ORIGIN));

        assertThat(result.user()).isEqualTo(user);
        assertThat(result.accessToken()).isEqualTo("access-for-" + user.id().value());
        assertThat(result.refreshTokenExpiresAt()).isEqualTo(IdentityFakes.NOW.plus(IdentityFakes.REFRESH_LIFETIME));
        assertThat(fakes.sessions.findByTokenHash(fakes.tokens.hash(result.refreshToken()))).isPresent();
        assertThat(fakes.limiter.resets).containsExactly("ana@example.com");
        assertThat(fakes.limiter.failures).isEmpty();
    }

    @Test
    void eachLoginStartsADistinctFamily() {
        fakes.existingUser("ana@example.com", PASSWORD);
        var first = useCase.execute(new LoginCommand("ana@example.com", PASSWORD, IdentityFakes.ORIGIN));
        var second = useCase.execute(new LoginCommand("ana@example.com", PASSWORD, IdentityFakes.ORIGIN));

        var familyA = fakes.sessions.findByTokenHash(fakes.tokens.hash(first.refreshToken())).orElseThrow().familyId();
        var familyB = fakes.sessions.findByTokenHash(fakes.tokens.hash(second.refreshToken())).orElseThrow().familyId();
        assertThat(familyA).isNotEqualTo(familyB);
    }

    @Test
    void unknownEmailAndWrongPasswordGiveTheSameResponseAndRecordFailure() {
        fakes.existingUser("ana@example.com", PASSWORD);

        var unknown = catchThrowableOfType(DomainException.class,
            () -> useCase.execute(new LoginCommand("nobody@example.com", PASSWORD, IdentityFakes.ORIGIN)));
        var wrong = catchThrowableOfType(DomainException.class,
            () -> useCase.execute(new LoginCommand("ana@example.com", "wrong password!", IdentityFakes.ORIGIN)));

        assertThat(unknown.code()).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        assertThat(wrong.code()).isEqualTo(unknown.code());
        assertThat(wrong.getMessage()).isEqualTo(unknown.getMessage());
        assertThat(wrong.args()).isEqualTo(unknown.args());
        assertThat(fakes.limiter.failures).containsEntry("nobody@example.com", 1).containsEntry("ana@example.com", 1);
        assertThat(fakes.sessions.byId).isEmpty();
    }

    @Test
    void invalidOrMissingEmailAndPasswordAreInvalidCredentials() {
        IdentityFakes.assertCode(
            () -> useCase.execute(new LoginCommand("not-an-email", PASSWORD, IdentityFakes.ORIGIN)), ErrorCode.INVALID_CREDENTIALS);
        IdentityFakes.assertCode(
            () -> useCase.execute(new LoginCommand(null, PASSWORD, IdentityFakes.ORIGIN)), ErrorCode.INVALID_CREDENTIALS);
        fakes.existingUser("ana@example.com", PASSWORD);
        IdentityFakes.assertCode(
            () -> useCase.execute(new LoginCommand("ana@example.com", null, IdentityFakes.ORIGIN)), ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void blockedAccountIsRejectedBeforeCheckingCredentials() {
        fakes.existingUser("ana@example.com", PASSWORD);
        fakes.limiter.blocked.add("ana@example.com");

        IdentityFakes.assertCode(
            () -> useCase.execute(new LoginCommand("Ana@example.com", PASSWORD, IdentityFakes.ORIGIN)), ErrorCode.TOO_MANY_ATTEMPTS);
        assertThat(fakes.sessions.byId).isEmpty();
        assertThat(fakes.limiter.failures).isEmpty();
    }
}
