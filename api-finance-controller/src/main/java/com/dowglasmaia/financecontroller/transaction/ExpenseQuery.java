package com.dowglasmaia.financecontroller.transaction;

import com.dowglasmaia.financecontroller.shared.money.Currency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.UUID;

/**
 * API pública de consulta de despesas, para metas, dashboard e relatórios (módulos seguintes). Só lê; nunca mistura
 * moedas e filtra sempre pelo proprietário.
 */
public interface ExpenseQuery {

    /** Total de despesas do mês na moeda; com {@code categoryId}, apenas dessa categoria (senão, todas). */
    BigDecimal expenseTotal(UUID ownerId, Currency currency, YearMonth month, UUID categoryId);

    /** Total de despesas do mês e da moeda por categoria (só categorias com gasto); uma consulta, sem N+1. */
    Map<UUID, BigDecimal> expenseTotalsByCategory(UUID ownerId, Currency currency, YearMonth month);

    /** Despesas do intervalo {@code from..to} (inclusivo por dia) e da moeda por categoria; só categorias com gasto. */
    Map<UUID, BigDecimal> expenseTotalsByCategory(UUID ownerId, Currency currency, LocalDate from, LocalDate to);
}
