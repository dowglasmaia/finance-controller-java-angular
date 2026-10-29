package com.dowglasmaia.financecontroller.transaction.application.port.out;

import com.dowglasmaia.financecontroller.shared.money.Currency;
import com.dowglasmaia.financecontroller.transaction.domain.BudgetAlert;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

/**
 * Fronteira com o módulo de metas (F6): dado o gasto do mês antes e depois da despesa, diz se o limiar de alerta foi
 * cruzado. Até existir o módulo de metas, a implementação não devolve alerta.
 */
public interface BudgetImpactPort {

    Optional<BudgetAlert> evaluate(Impact impact);

    /** Gasto do mês (da categoria e global) antes e depois da despesa; o "antes" já exclui o valor antigo na edição. */
    record Impact(UUID ownerId, Currency currency, YearMonth month, UUID categoryId, String categoryName,
                  BigDecimal categorySpentBefore, BigDecimal categorySpentAfter,
                  BigDecimal monthSpentBefore, BigDecimal monthSpentAfter) {
    }
}
