package com.dowglasmaia.financecontroller.identity.domain;

import com.dowglasmaia.financecontroller.shared.error.DomainException;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTests {

    @Test
    void acceptsLengthLimits() {
        assertThatCode(() -> PasswordPolicy.validate("x".repeat(PasswordPolicy.MIN_LENGTH))).doesNotThrowAnyException();
        assertThatCode(() -> PasswordPolicy.validate("x".repeat(PasswordPolicy.MAX_LENGTH))).doesNotThrowAnyException();
    }

    @Test
    void rejectsOutsideLengthLimits() {
        assertRejected(null);
        assertRejected("");
        assertRejected("x".repeat(PasswordPolicy.MIN_LENGTH - 1));
        assertRejected("x".repeat(PasswordPolicy.MAX_LENGTH + 1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345678", "password", "PASSWORD", "Senha123", "qwerty123", "iloveyou"})
    void rejectsCommonPasswordsIgnoringCase(String raw) {
        assertRejected(raw);
    }

    @Test
    void hasNoCompositionRules() {
        assertThatCode(() -> PasswordPolicy.validate("only lowercase words here")).doesNotThrowAnyException();
    }

    private static void assertRejected(String raw) {
        assertThatThrownBy(() -> PasswordPolicy.validate(raw))
            .isInstanceOfSatisfying(DomainException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_FAILED));
    }
}
