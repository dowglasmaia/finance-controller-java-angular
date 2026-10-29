package com.dowglasmaia.financecontroller.budget.application.usecase;

import com.dowglasmaia.financecontroller.budget.application.port.out.BudgetRepository;
import com.dowglasmaia.financecontroller.budget.domain.BudgetConfig;
import com.dowglasmaia.financecontroller.budget.domain.LimitAmount;
import com.dowglasmaia.financecontroller.budget.domain.MonthOverride;
import com.dowglasmaia.financecontroller.category.CategoryDirectory;
import com.dowglasmaia.financecontroller.category.CategoryType;
import com.dowglasmaia.financecontroller.identity.CurrentOwner;
import com.dowglasmaia.financecontroller.shared.error.DomainException;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;
import com.dowglasmaia.financecontroller.shared.error.FieldValidationException;
import com.dowglasmaia.financecontroller.shared.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Grava a meta da moeda de uma vez (DESC-BUD-09). Tudo é validado antes de gravar e a gravação é uma única transação:
 * qualquer categoria inválida desfaz tudo. Limite {@code 0} ou ausente remove (RN-BUD-02/03); sem ajuste, o ajuste do
 * mês é removido (P-06).
 */
@Service
public class SaveBudgetUseCase {

    private final BudgetRepository budgets;
    private final CategoryDirectory categories;
    private final CurrentOwner owner;

    SaveBudgetUseCase(BudgetRepository budgets, CategoryDirectory categories, CurrentOwner owner) {
        this.budgets = budgets;
        this.categories = categories;
        this.owner = owner;
    }

    @Transactional
    public BudgetConfig execute(SaveBudgetCommand command) {
        var ownerId = owner.id();
        BigDecimal monthly = LimitAmount.positiveOrNull(LimitAmount.parse(command.monthlyLimit(), "monthlyLimit"));
        MonthOverride override = override(command);
        Map<UUID, BigDecimal> limits = categoryLimits(ownerId, command);

        budgets.save(ownerId, command.currency(), command.month(), monthly, override, limits);
        return budgets.load(ownerId, command.currency(), command.month());
    }

    private static MonthOverride override(SaveBudgetCommand command) {
        if (!command.overrideGiven()) {
            return null;
        }
        BigDecimal limit = LimitAmount.parse(command.overrideLimit(), "monthOverride");
        if (command.overrideDisabled()) {
            if (limit != null) {
                throw new FieldValidationException("monthOverride", "validation.invalid");
            }
            return MonthOverride.off();
        }
        BigDecimal positive = LimitAmount.positiveOrNull(limit);
        return positive == null ? null : MonthOverride.ofLimit(positive);
    }

    /** Categorias existentes do proprietário (senão 404) que aceitam despesa (senão 409); repetidas são recusadas. */
    private Map<UUID, BigDecimal> categoryLimits(UUID ownerId, SaveBudgetCommand command) {
        Set<UUID> seen = new HashSet<>();
        Map<UUID, BigDecimal> wanted = new LinkedHashMap<>();
        for (var item : command.categoryLimits()) {
            if (!seen.add(item.categoryId())) {
                throw new FieldValidationException("categoryLimits", "validation.invalid");
            }
            BigDecimal limit = LimitAmount.positiveOrNull(LimitAmount.parse(item.limit(), "categoryLimits"));
            if (limit != null) {
                wanted.put(item.categoryId(), limit);
            }
        }
        var found = categories.findAll(ownerId, wanted.keySet());
        for (UUID id : wanted.keySet()) {
            var category = found.get(id);
            if (category == null) {
                throw new ResourceNotFoundException();
            }
            if (!category.type().accepts(CategoryType.EXPENSE)) {
                throw new DomainException(ErrorCode.CATEGORY_TYPE_MISMATCH);
            }
        }
        return wanted;
    }
}
