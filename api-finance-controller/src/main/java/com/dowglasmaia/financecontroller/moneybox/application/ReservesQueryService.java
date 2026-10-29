package com.dowglasmaia.financecontroller.moneybox.application;

import com.dowglasmaia.financecontroller.moneybox.ReservesQuery;
import com.dowglasmaia.financecontroller.moneybox.application.port.out.MoneyBoxRepository;
import com.dowglasmaia.financecontroller.shared.money.Currency;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/** Implementa a API pública de reservas sem lógica própria: a soma dos saldos vem do repositório. */
@Service
class ReservesQueryService implements ReservesQuery {

    private final MoneyBoxRepository boxes;

    ReservesQueryService(MoneyBoxRepository boxes) {
        this.boxes = boxes;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal total(UUID ownerId, Currency currency) {
        return boxes.totalBalance(ownerId, currency);
    }
}
