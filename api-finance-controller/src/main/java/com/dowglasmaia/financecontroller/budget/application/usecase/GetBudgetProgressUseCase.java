package com.dowglasmaia.financecontroller.budget.application.usecase;

import com.dowglasmaia.financecontroller.budget.application.port.out.BudgetRepository;
import com.dowglasmaia.financecontroller.budget.domain.Progress;
import com.dowglasmaia.financecontroller.category.CategoryDirectory;
import com.dowglasmaia.financecontroller.category.CategoryDirectory.CategoryView;
import com.dowglasmaia.financecontroller.category.CategoryType;
import com.dowglasmaia.financecontroller.identity.CurrentOwner;
import com.dowglasmaia.financecontroller.shared.money.Currency;
import com.dowglasmaia.financecontroller.transaction.ExpenseQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Collator;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Progresso do mês: o global e todas as categorias que aceitam despesa (DESC-BUD-09), com gastos exatos do mês e da moeda. */
@Service
public class GetBudgetProgressUseCase {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final BudgetRepository budgets;
    private final CategoryDirectory categories;
    private final ExpenseQuery expenses;
    private final CurrentOwner owner;

    GetBudgetProgressUseCase(BudgetRepository budgets, CategoryDirectory categories, ExpenseQuery expenses,
                             CurrentOwner owner) {
        this.budgets = budgets;
        this.categories = categories;
        this.expenses = expenses;
        this.owner = owner;
    }

    @Transactional(readOnly = true)
    public Result execute(Currency currency, YearMonth month) {
        var ownerId = owner.id();
        var config = budgets.load(ownerId, currency, month);
        Map<UUID, BigDecimal> byCategory = expenses.expenseTotalsByCategory(ownerId, currency, month);
        BigDecimal monthTotal = expenses.expenseTotal(ownerId, currency, month, null);

        Collator collator = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
        collator.setStrength(Collator.PRIMARY);
        List<Progress> perCategory = categories.listAccepting(ownerId, CategoryType.EXPENSE).stream()
            .sorted(Comparator.comparing(CategoryView::name, collator))
            .map(c -> Progress.of(c.id(), config.categoryLimit(c.id()), byCategory.getOrDefault(c.id(), ZERO)))
            .toList();
        return new Result(Progress.of(null, config.effectiveLimit(), monthTotal), perCategory);
    }

    public record Result(Progress global, List<Progress> categories) {
    }
}
