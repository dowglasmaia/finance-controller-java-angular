package com.dowglasmaia.financecontroller.report.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/** Percentual de uma parte sobre o total, em unidades de percentual com 4 casas ({@code HALF_UP}, RN-VAL-02). */
public final class Percentage {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int SCALE = 4;

    private Percentage() {
    }

    /** {@code part / total × 100}; total zero ou negativo → {@code 0.0000}. */
    public static BigDecimal of(BigDecimal part, BigDecimal total) {
        if (total.signum() <= 0) {
            return BigDecimal.ZERO.setScale(SCALE);
        }
        return part.multiply(HUNDRED).divide(total, SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Variação do saldo sobre o anterior, {@code (atual - anterior) / |anterior| × 100} com 1 casa ({@code HALF_UP});
     * ausente se o anterior é zero (RN-REP-05).
     */
    public static Optional<String> change(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.signum() == 0) {
            return Optional.empty();
        }
        return Optional.of(current.subtract(previous).multiply(HUNDRED)
            .divide(previous.abs(), 1, RoundingMode.HALF_UP).toPlainString());
    }
}
