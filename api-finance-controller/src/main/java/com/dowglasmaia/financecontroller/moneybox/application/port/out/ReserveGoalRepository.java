package com.dowglasmaia.financecontroller.moneybox.application.port.out;

import com.dowglasmaia.financecontroller.shared.money.Currency;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/** Meta geral de reservas por proprietário e moeda; ausência = sem meta. */
public interface ReserveGoalRepository {

    Optional<BigDecimal> find(UUID ownerId, Currency currency);

    void save(UUID ownerId, Currency currency, BigDecimal goal);

    void remove(UUID ownerId, Currency currency);
}
