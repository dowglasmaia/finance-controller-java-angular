package com.dowglasmaia.financecontroller.budget.domain;

import com.dowglasmaia.financecontroller.shared.money.Currency;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Configuração de metas de uma moeda vista a partir de um mês: meta recorrente, ajuste desse mês (se houver) e limites
 * por categoria. Metas de moedas diferentes nunca se misturam (RN-CUR-02).
 */
public record BudgetConfig(Currency currency, BigDecimal monthlyLimit, MonthOverride monthOverride,
                           Map<UUID, BigDecimal> categoryLimits) {

    public BudgetConfig {
        categoryLimits = Map.copyOf(categoryLimits);
    }

    /**
     * Limite do mês (RN-BUD-02): "sem meta neste mês" → nenhum; ajuste {@code > 0} → o ajuste; senão a meta recorrente
     * (que pode não existir).
     */
    public BigDecimal effectiveLimit() {
        if (monthOverride != null) {
            return monthOverride.disabled() ? null : monthOverride.limit();
        }
        return monthlyLimit;
    }

    /** Limite recorrente da categoria, ou {@code null}. */
    public BigDecimal categoryLimit(UUID categoryId) {
        return categoryLimits.get(categoryId);
    }
}
