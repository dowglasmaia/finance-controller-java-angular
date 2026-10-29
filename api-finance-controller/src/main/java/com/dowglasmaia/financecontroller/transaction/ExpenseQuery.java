package com.dowglasmaia.financecontroller.transaction;

import com.dowglasmaia.financecontroller.shared.money.Currency;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

/**
 * API pública de consulta de despesas, para metas, dashboard e relatórios (módulos seguintes). Só lê; nunca mistura
 * moedas e filtra sempre pelo proprietário.
 */
public interface ExpenseQuery {

    /** Total de despesas do mês na moeda; com {@code categoryId}, apenas dessa categoria (senão, todas). */
    BigDecimal expenseTotal(UUID ownerId, Currency currency, YearMonth month, UUID categoryId);
}
