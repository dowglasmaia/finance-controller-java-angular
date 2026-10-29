package com.dowglasmaia.financecontroller.moneybox.domain;

import com.dowglasmaia.financecontroller.shared.error.FieldValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

/** Valores monetários recebidos como texto decimal da API (RN-VAL-01) e razões com 4 casas. */
public final class Amounts {

    public static final BigDecimal MAX = new BigDecimal("9999999999.99");
    private static final Pattern FORMAT = Pattern.compile("^\\d{1,10}(\\.\\d{1,2})?$");

    private Amounts() {
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

    /** {@code clamp(numerator / denominator, 0, 1)} com 4 casas; {@code 0.0000} sem denominador positivo. */
    public static BigDecimal ratio(BigDecimal numerator, BigDecimal denominator) {
        BigDecimal zero = BigDecimal.ZERO.setScale(4);
        if (denominator == null || denominator.signum() <= 0 || numerator.signum() <= 0) {
            return zero;
        }
        BigDecimal ratio = numerator.divide(denominator, 4, RoundingMode.HALF_UP);
        return ratio.compareTo(BigDecimal.ONE) > 0 ? BigDecimal.ONE.setScale(4) : ratio;
    }
}
