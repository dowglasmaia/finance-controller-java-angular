package com.dowglasmaia.financecontroller.budget.application.usecase;

import com.dowglasmaia.financecontroller.budget.application.port.out.BudgetRepository;
import com.dowglasmaia.financecontroller.budget.domain.BudgetConfig;
import com.dowglasmaia.financecontroller.identity.CurrentOwner;
import com.dowglasmaia.financecontroller.shared.money.Currency;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

@Service
public class GetBudgetUseCase {

    private final BudgetRepository budgets;
    private final CurrentOwner owner;

    GetBudgetUseCase(BudgetRepository budgets, CurrentOwner owner) {
        this.budgets = budgets;
        this.owner = owner;
    }

    @Transactional(readOnly = true)
    public BudgetConfig execute(Currency currency, YearMonth month) {
        return budgets.load(owner.id(), currency, month);
    }
}
