package com.dowglasmaia.financecontroller.identity.domain;

import com.dowglasmaia.financecontroller.shared.error.DomainException;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTests {

    @Test
    void normalizesByTrimmingAndLowerCasing() {
        assertThat(Email.of("  Ana.Teste@Example.COM ").value()).isEqualTo("ana.teste@example.com");
        assertThat(Email.of("A@B.co")).isEqualTo(Email.of("a@b.CO"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "ana", "ana@", "@example.com", "ana@example", "ana@@example.com", "a na@example.com"})
    void rejectsInvalidFormat(String raw) {
        assertValidationFailed(() -> Email.of(raw));
    }

    @Test
    void rejectsNullAndTooLong() {
        assertValidationFailed(() -> Email.of(null));
        assertValidationFailed(() -> Email.of("a".repeat(250) + "@b.co"));
    }

    @Test
    void acceptsMaximumLength() {
        var local = "a".repeat(254 - "@b.co".length());
        assertThat(Email.of(local + "@b.co").value()).hasSize(254);
    }

    @Test
    void canonicalConstructorRejectsNonNormalizedValue() {
        assertValidationFailed(() -> new Email("Ana@Example.com"));
    }

    private static void assertValidationFailed(Runnable action) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(DomainException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_FAILED));
    }
}
