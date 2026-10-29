package com.dowglasmaia.financecontroller.moneybox.application.usecase;

import com.dowglasmaia.financecontroller.identity.CurrentOwner;
import com.dowglasmaia.financecontroller.moneybox.application.port.out.MoneyBoxRepository;
import com.dowglasmaia.financecontroller.moneybox.application.port.out.ReserveGoalRepository;
import com.dowglasmaia.financecontroller.moneybox.domain.Amounts;
import com.dowglasmaia.financecontroller.shared.money.Currency;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Total, meta geral e progresso da moeda; não depende de mês (RN-BOX-07/08). */
@Service
public class GetReservesSummaryUseCase {

    private final MoneyBoxRepository boxes;
    private final ReserveGoalRepository goals;
    private final CurrentOwner owner;

    GetReservesSummaryUseCase(MoneyBoxRepository boxes, ReserveGoalRepository goals, CurrentOwner owner) {
        this.boxes = boxes;
        this.goals = goals;
        this.owner = owner;
    }

    @Transactional(readOnly = true)
    public ReservesSummary execute(Currency currency) {
        return summarize(boxes, goals, owner.id(), currency);
    }

    static ReservesSummary summarize(MoneyBoxRepository boxes, ReserveGoalRepository goals, java.util.UUID ownerId,
                                     Currency currency) {
        var total = boxes.totalBalance(ownerId, currency);
        var goal = goals.find(ownerId, currency).orElse(null);
        return new ReservesSummary(currency, total, goal, Amounts.ratio(total, goal), boxes.count(ownerId, currency));
    }
}
