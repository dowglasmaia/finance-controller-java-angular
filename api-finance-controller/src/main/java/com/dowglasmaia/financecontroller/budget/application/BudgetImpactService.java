package com.dowglasmaia.financecontroller.budget.application;

import com.dowglasmaia.financecontroller.budget.application.port.out.BudgetRepository;
import com.dowglasmaia.financecontroller.budget.domain.BudgetStatus;
import com.dowglasmaia.financecontroller.transaction.BudgetAlert;
import com.dowglasmaia.financecontroller.transaction.BudgetImpactPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Alerta ao salvar uma despesa (RN-BUD-06/07): só a piora de estado alerta ({@code OK→WARNING}, {@code OK/WARNING→EXCEEDED});
 * nunca repete em estado já estourado nem alerta em melhora; um único alerta, com a categoria antes do global.
 */
@Service
class BudgetImpactService implements BudgetImpactPort {

    private final BudgetRepository budgets;

    BudgetImpactService(BudgetRepository budgets) {
        this.budgets = budgets;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BudgetAlert> evaluate(Impact impact) {
        var config = budgets.load(impact.ownerId(), impact.currency(), impact.month());

        var categoryStatus = worsened(impact.categorySpentBefore(), impact.categorySpentAfter(),
            config.categoryLimit(impact.categoryId()));
        if (categoryStatus.isPresent()) {
            return Optional.of(new BudgetAlert(BudgetAlert.Scope.CATEGORY, categoryStatus.get(), impact.categoryId(),
                impact.categoryName()));
        }
        return worsened(impact.monthSpentBefore(), impact.monthSpentAfter(), config.effectiveLimit())
            .map(status -> new BudgetAlert(BudgetAlert.Scope.GLOBAL, status, null, null));
    }

    private static Optional<BudgetAlert.Status> worsened(BigDecimal before, BigDecimal after, BigDecimal limit) {
        BudgetStatus then = BudgetStatus.of(before, limit);
        BudgetStatus now = BudgetStatus.of(after, limit);
        if (!now.worseThan(then)) {
            return Optional.empty();
        }
        return switch (now) {
            case WARNING -> Optional.of(BudgetAlert.Status.WARNING);
            case EXCEEDED -> Optional.of(BudgetAlert.Status.EXCEEDED);
            default -> Optional.empty();
        };
    }
}
