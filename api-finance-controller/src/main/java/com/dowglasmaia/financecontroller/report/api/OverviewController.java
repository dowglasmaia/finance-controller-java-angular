package com.dowglasmaia.financecontroller.report.api;

import com.dowglasmaia.financecontroller.generated.overview.api.OverviewApi;
import com.dowglasmaia.financecontroller.generated.overview.model.Currency;
import com.dowglasmaia.financecontroller.report.application.usecase.GetMonthlyOverviewUseCase;
import com.dowglasmaia.financecontroller.shared.error.FieldValidationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/** Só mapeia e delega; as contas vivem no caso de uso. */
@RestController
public class OverviewController implements OverviewApi {

    private final GetMonthlyOverviewUseCase getMonthlyOverview;

    public OverviewController(GetMonthlyOverviewUseCase getMonthlyOverview) {
        this.getMonthlyOverview = getMonthlyOverview;
    }

    @Override
    public ResponseEntity<com.dowglasmaia.financecontroller.generated.overview.model.MonthlyOverview> getMonthlyOverview(
            String month, Currency currency) {
        var result = getMonthlyOverview.execute(parseMonth(month),
            com.dowglasmaia.financecontroller.shared.money.Currency.valueOf(currency.getValue()));
        return ResponseEntity.ok(OverviewApiMapper.toResponse(result));
    }

    private static YearMonth parseMonth(String raw) {
        try {
            return YearMonth.parse(raw);
        } catch (DateTimeParseException invalid) {
            throw new FieldValidationException("month", "validation.invalid");
        }
    }
}
