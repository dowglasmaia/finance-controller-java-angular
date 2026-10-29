package com.dowglasmaia.financecontroller.budget.api;

import com.dowglasmaia.financecontroller.budget.application.usecase.GetBudgetProgressUseCase;
import com.dowglasmaia.financecontroller.budget.application.usecase.GetBudgetUseCase;
import com.dowglasmaia.financecontroller.budget.application.usecase.SaveBudgetUseCase;
import com.dowglasmaia.financecontroller.generated.budgets.api.BudgetsApi;
import com.dowglasmaia.financecontroller.generated.budgets.model.Budget;
import com.dowglasmaia.financecontroller.generated.budgets.model.BudgetProgress;
import com.dowglasmaia.financecontroller.generated.budgets.model.Currency;
import com.dowglasmaia.financecontroller.generated.budgets.model.SaveBudgetRequest;
import com.dowglasmaia.financecontroller.shared.error.FieldValidationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/** Só mapeia e delega; as regras vivem nos casos de uso. */
@RestController
public class BudgetsController implements BudgetsApi {

    private final GetBudgetUseCase getBudget;
    private final SaveBudgetUseCase saveBudget;
    private final GetBudgetProgressUseCase getBudgetProgress;

    public BudgetsController(GetBudgetUseCase getBudget, SaveBudgetUseCase saveBudget,
                             GetBudgetProgressUseCase getBudgetProgress) {
        this.getBudget = getBudget;
        this.saveBudget = saveBudget;
        this.getBudgetProgress = getBudgetProgress;
    }

    @Override
    public ResponseEntity<Budget> getBudget(Currency currency, String month) {
        var ym = parseMonth(month, "month");
        return ResponseEntity.ok(BudgetApiMapper.toResponse(getBudget.execute(BudgetApiMapper.toDomain(currency), ym), ym));
    }

    @Override
    public ResponseEntity<Budget> saveBudget(Currency currency, SaveBudgetRequest request) {
        var ym = parseMonth(request.getMonth(), "month");
        var saved = saveBudget.execute(BudgetApiMapper.toCommand(currency, ym, request));
        return ResponseEntity.ok(BudgetApiMapper.toResponse(saved, ym));
    }

    @Override
    public ResponseEntity<BudgetProgress> getBudgetProgress(Currency currency, String month) {
        var ym = parseMonth(month, "month");
        var result = getBudgetProgress.execute(BudgetApiMapper.toDomain(currency), ym);
        return ResponseEntity.ok(BudgetApiMapper.toResponse(BudgetApiMapper.toDomain(currency), ym, result));
    }

    private static YearMonth parseMonth(String raw, String field) {
        try {
            return YearMonth.parse(raw);
        } catch (DateTimeParseException invalid) {
            throw new FieldValidationException(field, "validation.invalid");
        }
    }
}
