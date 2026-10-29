package com.dowglasmaia.financecontroller.budget.api;

import com.dowglasmaia.financecontroller.budget.application.usecase.GetBudgetProgressUseCase;
import com.dowglasmaia.financecontroller.budget.application.usecase.SaveBudgetCommand;
import com.dowglasmaia.financecontroller.budget.domain.BudgetConfig;
import com.dowglasmaia.financecontroller.budget.domain.Progress;
import com.dowglasmaia.financecontroller.generated.budgets.model.Budget;
import com.dowglasmaia.financecontroller.generated.budgets.model.BudgetProgress;
import com.dowglasmaia.financecontroller.generated.budgets.model.BudgetStatus;
import com.dowglasmaia.financecontroller.generated.budgets.model.CategoryLimit;
import com.dowglasmaia.financecontroller.generated.budgets.model.Currency;
import com.dowglasmaia.financecontroller.generated.budgets.model.MonthOverride;
import com.dowglasmaia.financecontroller.generated.budgets.model.ProgressEntry;
import com.dowglasmaia.financecontroller.generated.budgets.model.SaveBudgetRequest;

import java.time.YearMonth;
import java.util.Comparator;

/** Conversão entre os modelos gerados do contrato e o domínio; nenhum tipo gerado vaza para os casos de uso. */
final class BudgetApiMapper {

    private BudgetApiMapper() {
    }

    static com.dowglasmaia.financecontroller.shared.money.Currency toDomain(Currency currency) {
        return com.dowglasmaia.financecontroller.shared.money.Currency.valueOf(currency.getValue());
    }

    static SaveBudgetCommand toCommand(Currency currency, YearMonth month, SaveBudgetRequest request) {
        var override = request.getMonthOverride();
        return new SaveBudgetCommand(toDomain(currency), month, request.getMonthlyLimit(),
            override == null ? null : override.getLimit(),
            override != null && Boolean.TRUE.equals(override.getDisabled()),
            override != null,
            request.getCategoryLimits().stream()
                .map(c -> new SaveBudgetCommand.CategoryLimit(c.getCategoryId(), c.getLimit()))
                .toList());
    }

    static Budget toResponse(BudgetConfig config, YearMonth month) {
        var response = new Budget(Currency.fromValue(config.currency().name()), month.toString(),
            config.categoryLimits().entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().toString()))
                .map(e -> new CategoryLimit(e.getKey(), e.getValue().toPlainString()))
                .toList());
        if (config.monthlyLimit() != null) {
            response.monthlyLimit(config.monthlyLimit().toPlainString());
        }
        if (config.monthOverride() != null) {
            var override = new MonthOverride();
            if (config.monthOverride().disabled()) {
                override.disabled(true);
            } else {
                override.limit(config.monthOverride().limit().toPlainString());
            }
            response.monthOverride(override);
        }
        if (config.effectiveLimit() != null) {
            response.effectiveLimit(config.effectiveLimit().toPlainString());
        }
        return response;
    }

    static BudgetProgress toResponse(com.dowglasmaia.financecontroller.shared.money.Currency currency, YearMonth month,
                                     GetBudgetProgressUseCase.Result result) {
        return new BudgetProgress(Currency.fromValue(currency.name()), month.toString(), toEntry(result.global()),
            result.categories().stream().map(BudgetApiMapper::toEntry).toList());
    }

    private static ProgressEntry toEntry(Progress progress) {
        var entry = new ProgressEntry(progress.spent().toPlainString(), BudgetStatus.fromValue(progress.status().name()));
        if (progress.categoryId() != null) {
            entry.categoryId(progress.categoryId());
        }
        if (progress.limit() != null) {
            entry.limit(progress.limit().toPlainString());
            entry.fraction(progress.fraction().toPlainString());
            entry.remaining(progress.remaining().toPlainString());
        }
        return entry;
    }
}
