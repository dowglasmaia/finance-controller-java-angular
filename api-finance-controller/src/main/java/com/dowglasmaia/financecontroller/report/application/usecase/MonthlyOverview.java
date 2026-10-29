package com.dowglasmaia.financecontroller.report.application.usecase;

import com.dowglasmaia.financecontroller.budget.BudgetProgressQuery.GlobalProgress;
import com.dowglasmaia.financecontroller.category.CategoryDirectory.CategoryView;
import com.dowglasmaia.financecontroller.category.CategoryDirectory.SubcategoryView;
import com.dowglasmaia.financecontroller.report.domain.MonthBands;
import com.dowglasmaia.financecontroller.shared.money.Currency;
import com.dowglasmaia.financecontroller.transaction.TransactionKind;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Resultado da visão geral mensal, já calculado; o controller só mapeia para o contrato. */
public record MonthlyOverview(YearMonth month, Currency currency, Totals summary, Totals previous,
                              Optional<String> incomeDeltaPercent, Optional<String> expenseDeltaPercent,
                              BigDecimal reservesTotal, List<MonthBands.Band> bars,
                              List<CategoryExpense> expenseByCategory, List<LatestTransaction> latestTransactions,
                              GlobalProgress budgetProgress) {

    public record Totals(BigDecimal income, BigDecimal expense, BigDecimal balance) {
    }

    public record CategoryExpense(CategoryView category, BigDecimal amount, BigDecimal percentage) {
    }

    public record LatestTransaction(UUID id, TransactionKind kind, String description, BigDecimal amount,
                                    Currency currency, LocalDate date, CategoryView category,
                                    SubcategoryView subcategory) {
    }
}
