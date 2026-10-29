package com.dowglasmaia.financecontroller.budget.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Progresso de um limite no mês (global quando {@code categoryId} é nulo). Sem limite, {@code limit}, {@code fraction} e
 * {@code remaining} são nulos e o estado é NONE. {@code fraction} tem 4 casas ({@code HALF_UP}); {@code remaining} pode ser
 * negativo.
 */
public record Progress(UUID categoryId, BigDecimal limit, BigDecimal spent, BigDecimal fraction, BigDecimal remaining,
                       BudgetStatus status) {

    private static final int FRACTION_SCALE = 4;

    public static Progress of(UUID categoryId, BigDecimal limit, BigDecimal spent) {
        BudgetStatus status = BudgetStatus.of(spent, limit);
        if (status == BudgetStatus.NONE) {
            return new Progress(categoryId, null, spent, null, null, status);
        }
        return new Progress(categoryId, limit, spent, spent.divide(limit, FRACTION_SCALE, RoundingMode.HALF_UP),
            limit.subtract(spent), status);
    }
}
