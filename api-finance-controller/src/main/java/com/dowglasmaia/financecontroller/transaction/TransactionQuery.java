package com.dowglasmaia.financecontroller.transaction;

import com.dowglasmaia.financecontroller.shared.money.Currency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * API pública de leitura de lançamentos para o dashboard e os relatórios: só lê, sempre dentro do proprietário e da
 * moeda (nunca mistura moedas). Cada método é uma consulta agregada.
 */
public interface TransactionQuery {

    /** Entradas e saídas do mês (zeros se não há lançamentos). */
    MonthTotals monthTotals(UUID ownerId, Currency currency, YearMonth month);

    /** Entradas e saídas por dia do mês; só os dias com lançamentos, sem ordem garantida. */
    List<DailyTotals> dailyTotals(UUID ownerId, Currency currency, YearMonth month);

    /** Os {@code limit} lançamentos mais recentes do mês na ordem RN-TXN-08 (data, criação e id decrescentes). */
    List<RecentTransaction> latest(UUID ownerId, Currency currency, YearMonth month, int limit);

    record MonthTotals(BigDecimal income, BigDecimal expense) {
        public BigDecimal balance() {
            return income.subtract(expense);
        }
    }

    record DailyTotals(LocalDate date, BigDecimal income, BigDecimal expense) {
    }

    record RecentTransaction(UUID id, TransactionKind kind, String description, BigDecimal amount, Currency currency,
                             LocalDate date, UUID categoryId, UUID subcategoryId) {
    }
}
