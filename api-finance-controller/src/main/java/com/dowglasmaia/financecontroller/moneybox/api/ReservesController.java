package com.dowglasmaia.financecontroller.moneybox.api;

import com.dowglasmaia.financecontroller.generated.reserves.api.ReservesApi;
import com.dowglasmaia.financecontroller.generated.reserves.model.Currency;
import com.dowglasmaia.financecontroller.generated.reserves.model.ReservesSummary;
import com.dowglasmaia.financecontroller.generated.reserves.model.SaveReserveGoalRequest;
import com.dowglasmaia.financecontroller.moneybox.application.usecase.GetReservesSummaryUseCase;
import com.dowglasmaia.financecontroller.moneybox.application.usecase.SaveReserveGoalUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Só mapeia e delega; as regras vivem nos casos de uso. */
@RestController
public class ReservesController implements ReservesApi {

    private final GetReservesSummaryUseCase getSummary;
    private final SaveReserveGoalUseCase saveGoal;

    public ReservesController(GetReservesSummaryUseCase getSummary, SaveReserveGoalUseCase saveGoal) {
        this.getSummary = getSummary;
        this.saveGoal = saveGoal;
    }

    @Override
    public ResponseEntity<ReservesSummary> getReservesSummary(Currency currency) {
        return ResponseEntity.ok(toResponse(getSummary.execute(toDomain(currency))));
    }

    @Override
    public ResponseEntity<ReservesSummary> saveReserveGoal(Currency currency, SaveReserveGoalRequest request) {
        return ResponseEntity.ok(toResponse(saveGoal.execute(toDomain(currency), request.getAmount())));
    }

    private static com.dowglasmaia.financecontroller.shared.money.Currency toDomain(Currency currency) {
        return com.dowglasmaia.financecontroller.shared.money.Currency.valueOf(currency.getValue());
    }

    private static ReservesSummary toResponse(
        com.dowglasmaia.financecontroller.moneybox.application.usecase.ReservesSummary summary) {
        var response = new ReservesSummary(Currency.fromValue(summary.currency().name()),
            summary.total().toPlainString(), summary.progress().toPlainString(), (int) summary.boxCount());
        if (summary.goal() != null) {
            response.goal(summary.goal().toPlainString());
        }
        return response;
    }
}
