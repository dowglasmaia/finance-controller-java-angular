package com.dowglasmaia.financecontroller.report.infrastructure;

import com.dowglasmaia.financecontroller.moneybox.ReservesQuery;
import com.dowglasmaia.financecontroller.report.application.port.out.ReservesPort;
import com.dowglasmaia.financecontroller.shared.money.Currency;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/** Liga o dashboard às reservas reais pela API pública do módulo de cofrinhos (substitui a porta provisória). */
@Component
class MoneyBoxReservesPort implements ReservesPort {

    private final ReservesQuery reserves;

    MoneyBoxReservesPort(ReservesQuery reserves) {
        this.reserves = reserves;
    }

    @Override
    public BigDecimal total(UUID ownerId, Currency currency) {
        return reserves.total(ownerId, currency);
    }
}
