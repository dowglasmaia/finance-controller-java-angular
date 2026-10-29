package com.dowglasmaia.financecontroller.report.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
}
