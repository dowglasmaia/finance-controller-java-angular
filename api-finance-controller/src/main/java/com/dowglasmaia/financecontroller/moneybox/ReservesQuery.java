package com.dowglasmaia.financecontroller.moneybox;

import com.dowglasmaia.financecontroller.shared.money.Currency;

import java.math.BigDecimal;
import java.util.UUID;

/** API pública de leitura do módulo: total guardado nas reservas do proprietário na moeda, independente do mês. */
public interface ReservesQuery {

    BigDecimal total(UUID ownerId, Currency currency);
}
