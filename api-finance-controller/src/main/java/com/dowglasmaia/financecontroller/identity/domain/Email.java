package com.dowglasmaia.financecontroller.identity.domain;

import com.dowglasmaia.financecontroller.shared.error.DomainException;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;

import java.util.Locale;
import java.util.regex.Pattern;

/** E-mail normalizado (aparado e em minúsculas); a unicidade é sobre este valor. */
public record Email(String value) {

    private static final int MAX_LENGTH = 254;
    private static final Pattern FORMAT = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public Email {
        if (value == null || value.length() > MAX_LENGTH || !FORMAT.matcher(value).matches() || !value.equals(value.toLowerCase(Locale.ROOT))) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED);
        }
    }

    public static Email of(String raw) {
        if (raw == null) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED);
        }
        return new Email(raw.strip().toLowerCase(Locale.ROOT));
    }
}
