package com.dowglasmaia.financecontroller.identity.application.usecase;

import com.dowglasmaia.financecontroller.identity.domain.UserId;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GetCurrentUserUseCaseTests {

    private final IdentityFakes fakes = new IdentityFakes();
    private final GetCurrentUserUseCase useCase = new GetCurrentUserUseCase(fakes.users);

    @Test
    void returnsTheUser() {
        var user = fakes.existingUser("ana@example.com", "correct horse battery");

        assertThat(useCase.execute(user.id())).isEqualTo(user);
    }

    @Test
    void missingUserIsUnauthenticated() {
        IdentityFakes.assertCode(() -> useCase.execute(UserId.generate()), ErrorCode.UNAUTHENTICATED);
    }
}
