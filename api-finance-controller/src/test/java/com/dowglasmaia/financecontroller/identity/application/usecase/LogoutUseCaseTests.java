package com.dowglasmaia.financecontroller.identity.application.usecase;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class LogoutUseCaseTests {

    private final IdentityFakes fakes = new IdentityFakes();
    private final LogoutUseCase useCase = new LogoutUseCase(fakes.sessions, fakes.tokens, fakes.clock);

    @Test
    void revokesTheWholeFamilyOfTheToken() {
        var user = fakes.existingUser("ana@example.com", "correct horse battery");
        var token = fakes.issuer.openNewFamily(user.id()).token();
        var familyId = fakes.sessions.findByTokenHash(fakes.tokens.hash(token)).orElseThrow().familyId();
        fakes.issuer.open(user.id(), familyId);
        var otherFamilyToken = fakes.issuer.openNewFamily(user.id()).token();

        useCase.execute(token);

        assertThat(fakes.sessions.family(familyId)).hasSize(2).allMatch(s -> s.isRevoked());
        assertThat(fakes.sessions.findByTokenHash(fakes.tokens.hash(otherFamilyToken)).orElseThrow().isRevoked()).isFalse();
    }

    @Test
    void isIdempotent() {
        var user = fakes.existingUser("ana@example.com", "correct horse battery");
        var token = fakes.issuer.openNewFamily(user.id()).token();

        useCase.execute(token);

        assertThatCode(() -> useCase.execute(token)).doesNotThrowAnyException();
    }

    @Test
    void ignoresNullBlankAndUnknownTokens() {
        assertThatCode(() -> useCase.execute(null)).doesNotThrowAnyException();
        assertThatCode(() -> useCase.execute(" ")).doesNotThrowAnyException();
        assertThatCode(() -> useCase.execute("never-issued")).doesNotThrowAnyException();
        assertThat(fakes.sessions.byId).isEmpty();
    }
}
