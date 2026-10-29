package com.dowglasmaia.financecontroller.moneybox.application.usecase;

import com.dowglasmaia.financecontroller.shared.money.Currency;

import java.math.BigDecimal;

/** {@code goal} é nulo sem meta geral; {@code progress} é {@code min(total/meta, 1)} (RN-BOX-08). */
public record ReservesSummary(Currency currency, BigDecimal total, BigDecimal goal, BigDecimal progress,
                              long boxCount) {
}
