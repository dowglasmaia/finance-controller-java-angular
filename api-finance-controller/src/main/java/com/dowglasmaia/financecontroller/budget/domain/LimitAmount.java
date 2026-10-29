package com.dowglasmaia.financecontroller.budget.domain;

import com.dowglasmaia.financecontroller.shared.error.FieldValidationException;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/** Valor de limite recebido como texto decimal da API (RN-VAL-01); {@code 0} é válido e significa "remover". */
public final class LimitAmount {

    public static final BigDecimal MAX = new BigDecimal("9999999999.99");
    private static final Pattern FORMAT = Pattern.compile("^\\d{1,10}(\\.\\d{1,2})?$");

    private LimitAmount() {
    }

    /** @return o valor com escala 2, ou {@code null} se {@code raw} é nulo ou vazio */
    public static BigDecimal parse(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String text = raw.strip();
        if (!FORMAT.matcher(text).matches()) {
            throw new FieldValidationException(field, "validation.invalid");
        }
        BigDecimal value = new BigDecimal(text);
        if (value.compareTo(MAX) > 0) {
            throw new FieldValidationException(field, "validation.amount.max");
        }
        return value.setScale(2);
    }

    /** O valor se for positivo; zero ou ausente vira {@code null} (remoção). */
    public static BigDecimal positiveOrNull(BigDecimal value) {
        return value == null || value.signum() <= 0 ? null : value;
    }
}
