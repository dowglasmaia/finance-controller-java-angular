package com.dowglasmaia.financecontroller.moneybox.application.usecase;

import com.dowglasmaia.financecontroller.identity.CurrentOwner;
import com.dowglasmaia.financecontroller.moneybox.application.port.out.MoneyBoxRepository;
import com.dowglasmaia.financecontroller.moneybox.application.port.out.ReserveGoalRepository;
import com.dowglasmaia.financecontroller.moneybox.domain.Amounts;
import com.dowglasmaia.financecontroller.shared.error.FieldValidationException;
import com.dowglasmaia.financecontroller.shared.money.Currency;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Valor positivo grava a meta geral da moeda; {@code 0} a remove (RN-BOX-08). */
@Service
public class SaveReserveGoalUseCase {

    private final MoneyBoxRepository boxes;
    private final ReserveGoalRepository goals;
    private final CurrentOwner owner;

    SaveReserveGoalUseCase(MoneyBoxRepository boxes, ReserveGoalRepository goals, CurrentOwner owner) {
        this.boxes = boxes;
        this.goals = goals;
        this.owner = owner;
    }

    @Transactional
    public ReservesSummary execute(Currency currency, String amount) {
        var ownerId = owner.id();
        var goal = Amounts.parse(amount, "amount");
        if (goal == null) {
            throw new FieldValidationException("amount", "validation.amount.required");
        }
        if (goal.signum() == 0) {
            goals.remove(ownerId, currency);
        } else {
            goals.save(ownerId, currency, goal);
        }
        return GetReservesSummaryUseCase.summarize(boxes, goals, ownerId, currency);
    }
}
