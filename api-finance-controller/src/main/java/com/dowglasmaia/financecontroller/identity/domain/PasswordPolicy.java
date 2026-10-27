package com.dowglasmaia.financecontroller.identity.domain;

import com.dowglasmaia.financecontroller.shared.error.DomainException;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;

import java.util.Locale;
import java.util.Set;

/** Política de senha: 8 a 128 caracteres, sem regras de composição, recusando senhas muito comuns. */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 128;

    private static final Set<String> COMMON = Set.of(
        "12345678", "123456789", "1234567890", "password", "password1", "qwertyui", "qwerty123",
        "11111111", "00000000", "abcd1234", "iloveyou", "senha123", "12341234", "123123123");

    private PasswordPolicy() {
    }

    public static void validate(String raw) {
        if (raw == null || raw.length() < MIN_LENGTH || raw.length() > MAX_LENGTH || COMMON.contains(raw.toLowerCase(Locale.ROOT))) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED);
        }
    }
}
