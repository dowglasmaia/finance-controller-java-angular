package com.dowglasmaia.financecontroller.moneybox.application.usecase;

import com.dowglasmaia.financecontroller.identity.CurrentOwner;
import com.dowglasmaia.financecontroller.moneybox.application.port.out.MoneyBoxRepository;
import com.dowglasmaia.financecontroller.shared.money.Currency;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** Dois comandos SQL, qualquer que seja o número de cofrinhos: a lista e os saldos agrupados. */
@Service
public class ListMoneyBoxesUseCase {

    private final MoneyBoxRepository boxes;
    private final CurrentOwner owner;

    ListMoneyBoxesUseCase(MoneyBoxRepository boxes, CurrentOwner owner) {
        this.boxes = boxes;
        this.owner = owner;
    }

    @Transactional(readOnly = true)
    public List<MoneyBoxView> execute(Currency currency) {
        var ownerId = owner.id();
        var balances = boxes.balances(ownerId, currency);
        return boxes.findAll(ownerId, currency).stream()
            .map(box -> MoneyBoxView.of(box, balances.getOrDefault(box.id(), BigDecimal.ZERO.setScale(2))))
            .toList();
    }
}
