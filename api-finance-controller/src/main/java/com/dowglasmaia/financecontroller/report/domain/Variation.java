package com.dowglasmaia.financecontroller.report.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Variação percentual sobre o mês anterior (RN-SAL-02): {@code (atual - anterior) / anterior × 100}, só se
 * {@code anterior > 0}. Variação com módulo menor que 0,5 vale "0"; as demais têm 1 casa decimal ({@code HALF_UP}).
 * Cálculo exato com {@link BigDecimal}.
 */
public final class Variation {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal HALF = new BigDecimal("0.5");
    private static final int PRECISION = 20;

    private Variation() {
    }

    /** @return o texto da variação, ou vazio se não há base de comparação */
    public static Optional<String> percent(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.signum() <= 0) {
            return Optional.empty();
        }
        BigDecimal exact = current.subtract(previous).multiply(HUNDRED)
            .divide(previous, PRECISION, RoundingMode.HALF_UP);
        if (exact.abs().compareTo(HALF) < 0) {
            return Optional.of("0");
        }
        return Optional.of(exact.setScale(1, RoundingMode.HALF_UP).toPlainString());
    }
}
