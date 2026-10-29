package com.dowglasmaia.financecontroller.moneybox.application.usecase;

import com.dowglasmaia.financecontroller.moneybox.domain.MoneyBox;
import com.dowglasmaia.financecontroller.moneybox.domain.MoneyBoxEntry;
import com.dowglasmaia.financecontroller.shared.money.Currency;

/** Movimentação com a moeda do cofrinho, que ela herda. */
public record EntryView(MoneyBoxEntry entry, Currency currency) {

    public static EntryView of(MoneyBox box, MoneyBoxEntry entry) {
        return new EntryView(entry, box.currency());
    }
}
