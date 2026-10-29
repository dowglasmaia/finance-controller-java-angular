package com.dowglasmaia.financecontroller.budget.application;

import com.dowglasmaia.financecontroller.budget.BudgetProgressQuery;
import com.dowglasmaia.financecontroller.budget.application.port.out.BudgetRepository;
import com.dowglasmaia.financecontroller.budget.domain.Progress;
import com.dowglasmaia.financecontroller.shared.money.Currency;
import com.dowglasmaia.financecontroller.transaction.ExpenseQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.UUID;

/** Implementa a API pública do progresso global com a mesma regra de estado do módulo; sem lógica própria. */
@Service
class BudgetProgressQueryService implements BudgetProgressQuery {

    private final BudgetRepository budgets;
    private final ExpenseQuery expenses;

    BudgetProgressQueryService(BudgetRepository budgets, ExpenseQuery expenses) {
        this.budgets = budgets;
        this.expenses = expenses;
    }

    @Override
    @Transactional(readOnly = true)
    public GlobalProgress globalProgress(UUID ownerId, Currency currency, YearMonth month) {
        var config = budgets.load(ownerId, currency, month);
        Progress progress = Progress.of(null, config.effectiveLimit(), expenses.expenseTotal(ownerId, currency, month, null));
        return new GlobalProgress(progress.limit(), progress.spent(), progress.fraction(), progress.remaining(),
            Status.valueOf(progress.status().name()));
    }
}
