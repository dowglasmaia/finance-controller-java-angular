package com.dowglasmaia.financecontroller.moneybox.application.usecase;

import com.dowglasmaia.financecontroller.moneybox.domain.Amounts;
import com.dowglasmaia.financecontroller.moneybox.domain.MoneyBox;

import java.math.BigDecimal;

/** Cofrinho com o saldo e o progresso calculados na leitura (nada disso é persistido). */
public record MoneyBoxView(MoneyBox box, BigDecimal balance, BigDecimal progress) {

    public static MoneyBoxView of(MoneyBox box, BigDecimal balance) {
        return new MoneyBoxView(box, balance, Amounts.ratio(balance, box.goal()));
    }
}
