package com.dowglasmaia.financecontroller.report.application.usecase;

import com.dowglasmaia.financecontroller.category.CategoryDirectory;
import com.dowglasmaia.financecontroller.category.CategoryDirectory.CategoryView;
import com.dowglasmaia.financecontroller.identity.CurrentOwner;
import com.dowglasmaia.financecontroller.report.domain.MonthBands;
import com.dowglasmaia.financecontroller.report.domain.Percentage;
import com.dowglasmaia.financecontroller.report.domain.PeriodBars;
import com.dowglasmaia.financecontroller.report.domain.ReportPeriod;
import com.dowglasmaia.financecontroller.report.domain.ReportRange;
import com.dowglasmaia.financecontroller.shared.error.FieldValidationException;
import com.dowglasmaia.financecontroller.shared.money.Currency;
import com.dowglasmaia.financecontroller.transaction.ExpenseQuery;
import com.dowglasmaia.financecontroller.transaction.TransactionQuery;
import com.dowglasmaia.financecontroller.transaction.TransactionQuery.MonthTotals;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Collator;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Relatório do período e da moeda. O resumo, as barras, a distribuição e o resumo mensal derivam do mesmo intervalo
 * (corrige DESC-REP-03). Quatro consultas agregadas, qualquer que seja o volume; nada é lido de tabelas de outros módulos.
 */
@Service
public class GetReportUseCase {

    public static final int DEFAULT_RANKING_SIZE = 7;
    public static final int MAX_RANKING_SIZE = 50;

    private final TransactionQuery transactions;
    private final ExpenseQuery expenses;
    private final CategoryDirectory categories;
    private final CurrentOwner owner;

    GetReportUseCase(TransactionQuery transactions, ExpenseQuery expenses, CategoryDirectory categories,
                     CurrentOwner owner) {
        this.transactions = transactions;
        this.expenses = expenses;
        this.categories = categories;
        this.owner = owner;
    }

    @Transactional(readOnly = true)
    public Report execute(ReportPeriod period, LocalDate anchorDate, Currency currency, Integer rankingSize) {
        int size = rankingSize == null ? DEFAULT_RANKING_SIZE : rankingSize;
        if (size < 1 || size > MAX_RANKING_SIZE) {
            throw new FieldValidationException("rankingSize", "validation.invalid");
        }
        ReportRange range = ReportRange.of(period, anchorDate);
        UUID ownerId = owner.id();

        MonthTotals totals = transactions.rangeTotals(ownerId, currency, range.start(), range.end());
        List<MonthBands.Day> days = transactions.dailyTotals(ownerId, currency, range.start(), range.end()).stream()
            .map(d -> new MonthBands.Day(d.date(), d.income(), d.expense()))
            .toList();
        Map<UUID, BigDecimal> byCategory = expenses.expenseTotalsByCategory(ownerId, currency, range.start(), range.end());
        Map<UUID, CategoryView> views = categories.findAll(ownerId, byCategory.keySet());

        List<MonthlyOverview.CategoryExpense> distribution = shares(byCategory, views, totals.expense());
        return new Report(period, currency, range,
            new MonthlyOverview.Totals(totals.income(), totals.expense(), totals.balance()),
            PeriodBars.of(period, range, days), distribution,
            distribution.stream().limit(size).toList(),
            period == ReportPeriod.YEAR ? monthly(range.start().getYear(), days) : null);
    }

    /** Da maior para a menor despesa (empate por nome), com a parte de cada categoria no total de despesas. */
    private static List<MonthlyOverview.CategoryExpense> shares(Map<UUID, BigDecimal> byCategory,
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

    /** Doze meses; a variação do saldo é ausente em janeiro e quando o saldo anterior é zero (RN-REP-05). */
    private static List<Report.MonthlyItem> monthly(int year, List<MonthBands.Day> days) {
        BigDecimal[][] totals = PeriodBars.monthTotals(year, days);
        List<Report.MonthlyItem> items = new ArrayList<>(12);
        BigDecimal previousBalance = null;
        for (int i = 0; i < 12; i++) {
            BigDecimal income = totals[i][0];
            BigDecimal expense = totals[i][1];
            BigDecimal balance = income.subtract(expense);
            Optional<String> change = previousBalance == null ? Optional.empty() : Percentage.change(balance, previousBalance);
            items.add(new Report.MonthlyItem(YearMonth.of(year, i + 1), income, expense, balance, change));
            previousBalance = balance;
        }
        return items;
    }
}
