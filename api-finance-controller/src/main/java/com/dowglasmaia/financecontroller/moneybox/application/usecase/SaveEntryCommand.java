package com.dowglasmaia.financecontroller.moneybox.application.usecase;

import com.dowglasmaia.financecontroller.moneybox.domain.EntryKind;
import com.dowglasmaia.financecontroller.shared.money.Currency;

import java.time.LocalDate;

/** {@code currency} é opcional: se vier e divergir da do cofrinho, a gravação é recusada (CURRENCY_MISMATCH). */
public record SaveEntryCommand(EntryKind kind, String amount, LocalDate date, String note, Currency currency) {
}
