package com.dowglasmaia.financecontroller.transaction.application;

import com.dowglasmaia.financecontroller.shared.money.Currency;
import com.dowglasmaia.financecontroller.transaction.ExpenseQuery;
import com.dowglasmaia.financecontroller.transaction.application.port.out.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;
import java.util.UUID;

/** Implementa a API pública de despesas sobre a porta de saída; sem regra própria. */
@Service
class ExpenseQueryService implements ExpenseQuery {

    private final TransactionRepository transactions;

    ExpenseQueryService(TransactionRepository transactions) {
        this.transactions = transactions;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal expenseTotal(UUID ownerId, Currency currency, YearMonth month, UUID categoryId) {
        return transactions.sumExpenses(ownerId, month, currency, categoryId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, BigDecimal> expenseTotalsByCategory(UUID ownerId, Currency currency, YearMonth month) {
        return transactions.sumExpensesByCategory(ownerId, month, currency);
    }
}
