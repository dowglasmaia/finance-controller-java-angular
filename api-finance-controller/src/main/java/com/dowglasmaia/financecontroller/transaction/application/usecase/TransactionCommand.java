package com.dowglasmaia.financecontroller.transaction.application.usecase;

import com.dowglasmaia.financecontroller.shared.money.Currency;
import com.dowglasmaia.financecontroller.transaction.TransactionKind;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

/** Dados de criação e edição; {@code amount} chega como texto decimal (nunca ponto flutuante). */
public record TransactionCommand(TransactionKind kind, String description, String amount, Currency currency,
                                 LocalDate date, UUID categoryId, UUID subcategoryId, String notes,
                                 YearMonth referenceMonth) {
}
