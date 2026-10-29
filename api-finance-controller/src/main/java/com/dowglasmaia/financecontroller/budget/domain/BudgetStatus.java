package com.dowglasmaia.financecontroller.budget.domain;

import java.math.BigDecimal;

/** Estado do gasto frente ao limite (RN-BUD-04). A comparação é exata, sem ponto flutuante. */
public enum BudgetStatus {
    NONE(0), OK(1), WARNING(2), EXCEEDED(3);

    private static final BigDecimal FIVE = BigDecimal.valueOf(5);
    private static final BigDecimal FOUR = BigDecimal.valueOf(4);

    private final int severity;

    BudgetStatus(int severity) {
        this.severity = severity;
    }

    /** Limite ausente ou {@code <= 0} → NONE; {@code gasto >= limite} → EXCEEDED; {@code gasto*5 >= limite*4} → WARNING. */
    public static BudgetStatus of(BigDecimal spent, BigDecimal limit) {
        if (limit == null || limit.signum() <= 0) {
            return NONE;
        }
        if (spent.compareTo(limit) >= 0) {
            return EXCEEDED;
        }
        if (spent.multiply(FIVE).compareTo(limit.multiply(FOUR)) >= 0) {
            return WARNING;
        }
        return OK;
    }

    /** Verdadeiro se este estado é mais grave que {@code other} (base do alerta: só a piora alerta, RN-BUD-06). */
    public boolean worseThan(BudgetStatus other) {
        return severity > other.severity;
    }
}
