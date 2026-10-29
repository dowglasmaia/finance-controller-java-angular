package com.dowglasmaia.financecontroller.report.infrastructure;

import com.dowglasmaia.financecontroller.report.application.port.out.ReservesPort;
import com.dowglasmaia.financecontroller.shared.money.Currency;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/** Implementação provisória enquanto não há cofrinhos: nenhuma reserva (zero). O módulo de cofrinhos (F8) a substitui. */
@Component
class NoReservesPort implements ReservesPort {

    @Override
    public BigDecimal total(UUID ownerId, Currency currency) {
        return BigDecimal.ZERO.setScale(2);
    }
}
