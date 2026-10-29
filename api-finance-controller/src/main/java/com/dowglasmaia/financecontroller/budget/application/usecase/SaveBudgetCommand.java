package com.dowglasmaia.financecontroller.budget.application.usecase;

import com.dowglasmaia.financecontroller.shared.money.Currency;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Pedido de gravação; os valores chegam como texto decimal. {@code overrideLimit} e {@code overrideDisabled} descrevem
 * o ajuste de {@code month}: ambos vazios removem o ajuste.
 */
public record SaveBudgetCommand(Currency currency, YearMonth month, String monthlyLimit, String overrideLimit,
                                boolean overrideDisabled, boolean overrideGiven, List<CategoryLimit> categoryLimits) {

    public record CategoryLimit(UUID categoryId, String limit) {
    }
}
