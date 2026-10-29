package com.dowglasmaia.financecontroller.budget.domain;

import java.math.BigDecimal;

/**
 * Ajuste de um mês (RN-BUD-09): um valor {@code > 0} que sobrepõe a meta recorrente, ou "sem meta neste mês"
 * ({@code disabled}). Um ajuste {@code <= 0} não existe: é removido.
 */
public record MonthOverride(BigDecimal limit, boolean disabled) {

    public static MonthOverride ofLimit(BigDecimal limit) {
        return new MonthOverride(limit, false);
    }

    public static MonthOverride off() {
        return new MonthOverride(null, true);
    }
}
