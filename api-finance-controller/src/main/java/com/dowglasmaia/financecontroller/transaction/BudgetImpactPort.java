package com.dowglasmaia.financecontroller.transaction;

import com.dowglasmaia.financecontroller.shared.money.Currency;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de fronteira com o módulo de metas: dado o gasto do mês antes e depois da despesa, diz se o estado piorou e
 * qual alerta mostrar (categoria primeiro, depois o global). Implementada pelo módulo {@code budget}; o módulo
 * {@code transaction} só conhece esta interface.
 */
public interface BudgetImpactPort {

    Optional<BudgetAlert> evaluate(Impact impact);

    /** Gasto do mês (da categoria e global) antes e depois da despesa; o "antes" já exclui o valor antigo na edição. */
    record Impact(UUID ownerId, Currency currency, YearMonth month, UUID categoryId, String categoryName,
                  BigDecimal categorySpentBefore, BigDecimal categorySpentAfter,
                  BigDecimal monthSpentBefore, BigDecimal monthSpentAfter) {
    }
}
