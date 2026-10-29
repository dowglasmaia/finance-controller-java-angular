package com.dowglasmaia.financecontroller.report.application.port.out;

import com.dowglasmaia.financecontroller.shared.money.Currency;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Fronteira com o módulo de cofrinhos (F8): saldo acumulado das reservas do proprietário na moeda, independente do mês.
 * Até esse módulo existir, a implementação devolve zero.
 */
public interface ReservesPort {

    BigDecimal total(UUID ownerId, Currency currency);
}
