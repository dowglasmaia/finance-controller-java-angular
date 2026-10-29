package com.dowglasmaia.financecontroller.report.application.usecase;

import com.dowglasmaia.financecontroller.budget.BudgetProgressQuery;
import com.dowglasmaia.financecontroller.category.CategoryDirectory;
import com.dowglasmaia.financecontroller.category.CategoryDirectory.CategoryView;
import com.dowglasmaia.financecontroller.identity.CurrentOwner;
import com.dowglasmaia.financecontroller.report.application.port.out.ReservesPort;
import com.dowglasmaia.financecontroller.report.domain.MonthBands;
import com.dowglasmaia.financecontroller.report.domain.Percentage;
import com.dowglasmaia.financecontroller.report.domain.Variation;
import com.dowglasmaia.financecontroller.shared.money.Currency;
import com.dowglasmaia.financecontroller.transaction.ExpenseQuery;
import com.dowglasmaia.financecontroller.transaction.TransactionQuery;
import com.dowglasmaia.financecontroller.transaction.TransactionQuery.MonthTotals;
import com.dowglasmaia.financecontroller.transaction.TransactionQuery.RecentTransaction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Collator;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Visão geral do mês e da moeda para o dashboard. Toda conta é feita aqui (variação, faixas, percentuais, ordem); o
 * cliente só formata. Consultas agregadas, sem N+1; nada é lido de tabelas de outros módulos.
 */
@Service
public class GetMonthlyOverviewUseCase {

    static final int LATEST_LIMIT = 8;

    private final TransactionQuery transactions;
    private final ExpenseQuery expenses;
    private final CategoryDirectory categories;
    private final BudgetProgressQuery budgets;
    private final ReservesPort reserves;
    private final CurrentOwner owner;

    GetMonthlyOverviewUseCase(TransactionQuery transactions, ExpenseQuery expenses, CategoryDirectory categories,
                              BudgetProgressQuery budgets, ReservesPort reserves, CurrentOwner owner) {
        this.transactions = transactions;
        this.expenses = expenses;
        this.categories = categories;
        this.budgets = budgets;
        this.reserves = reserves;
        this.owner = owner;
    }

    @Transactional(readOnly = true)
    public MonthlyOverview execute(YearMonth month, Currency currency) {
        UUID ownerId = owner.id();
        MonthTotals current = transactions.monthTotals(ownerId, currency, month);
        MonthTotals previous = transactions.monthTotals(ownerId, currency, month.minusMonths(1));

        var days = transactions.dailyTotals(ownerId, currency, month).stream()
            .map(d -> new MonthBands.Day(d.date(), d.income(), d.expense()))
            .toList();

        Map<UUID, BigDecimal> expenseByCategory = expenses.expenseTotalsByCategory(ownerId, currency, month);
        List<RecentTransaction> latest = transactions.latest(ownerId, currency, month, LATEST_LIMIT);

        Set<UUID> categoryIds = new HashSet<>(expenseByCategory.keySet());
        latest.forEach(t -> categoryIds.add(t.categoryId()));
        Map<UUID, CategoryView> views = categories.findAll(ownerId, categoryIds);

        return new MonthlyOverview(month, currency, totals(current), totals(previous),
            Variation.percent(current.income(), previous.income()),
            Variation.percent(current.expense(), previous.expense()),
            reserves.total(ownerId, currency),
            MonthBands.of(month, days),
            expenseShares(expenseByCategory, views, current.expense()),
            latest.stream().map(t -> latestOf(t, views)).toList(),
            budgets.globalProgress(ownerId, currency, month));
    }

    private static MonthlyOverview.Totals totals(MonthTotals totals) {
        return new MonthlyOverview.Totals(totals.income(), totals.expense(), totals.balance());
    }

    /** Despesas por categoria, da maior para a menor (empate por nome), com a parte de cada uma no total de despesas. */
    private static List<MonthlyOverview.CategoryExpense> expenseShares(Map<UUID, BigDecimal> byCategory,
                                                                       Map<UUID, CategoryView> views, BigDecimal total) {
        Collator collator = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
        collator.setStrength(Collator.PRIMARY);
        return byCategory.entrySet().stream()
            .filter(e -> views.containsKey(e.getKey()))
            .map(e -> new MonthlyOverview.CategoryExpense(views.get(e.getKey()), e.getValue(),
                Percentage.of(e.getValue(), total)))
            .sorted(Comparator.comparing(MonthlyOverview.CategoryExpense::amount).reversed()
                .thenComparing(c -> c.category().name(), collator))
            .toList();
    }

    private static MonthlyOverview.LatestTransaction latestOf(RecentTransaction t, Map<UUID, CategoryView> views) {
        CategoryView category = views.get(t.categoryId());
        var subcategory = t.subcategoryId() == null || category == null ? null
            : category.subcategories().stream().filter(s -> s.id().equals(t.subcategoryId())).findFirst().orElse(null);
        return new MonthlyOverview.LatestTransaction(t.id(), t.kind(), t.description(), t.amount(), t.currency(),
            t.date(), category, subcategory);
    }
}
