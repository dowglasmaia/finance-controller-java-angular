package com.dowglasmaia.financecontroller.moneybox.domain;

import com.dowglasmaia.financecontroller.shared.error.FieldValidationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/** Depósito ou saque de um cofrinho (RN-BOX-03); a moeda é a do cofrinho. */
public record MoneyBoxEntry(UUID id, UUID ownerId, UUID moneyBoxId, EntryKind kind, BigDecimal amount,
                            LocalDate date, String note, Instant createdAt) {

    public static final LocalDate MIN_DATE = LocalDate.of(2000, 1, 1);
    public static final LocalDate MAX_DATE = LocalDate.of(2100, 12, 31);

    public MoneyBoxEntry {
        if (kind == null) {
            throw new FieldValidationException("kind", "validation.required");
        }
        if (amount == null) {
            throw new FieldValidationException("amount", "validation.amount.required");
        }
        if (amount.signum() <= 0) {
            throw new FieldValidationException("amount", "validation.amount.positive");
        }
        if (amount.compareTo(Amounts.MAX) > 0) {
            throw new FieldValidationException("amount", "validation.amount.max");
        }
        if (amount.scale() > 2) {
            throw new FieldValidationException("amount", "validation.invalid");
        }
        amount = amount.setScale(2);
        if (date == null || date.isBefore(MIN_DATE) || date.isAfter(MAX_DATE)) {
            throw new FieldValidationException("date", "validation.date.range");
        }
        if (id == null || ownerId == null || moneyBoxId == null || createdAt == null) {
            throw new FieldValidationException("body", "validation.invalid");
        }
        note = note == null || note.isBlank() ? null : note.strip();
        if (note != null && note.length() > 255) {
            throw new FieldValidationException("note", "validation.text.length");
        }
        createdAt = createdAt.truncatedTo(ChronoUnit.MICROS);
    }

    public static MoneyBoxEntry create(UUID ownerId, UUID moneyBoxId, EntryKind kind, String amount, LocalDate date,
                                       String note, Instant now) {
        return new MoneyBoxEntry(UUID.randomUUID(), ownerId, moneyBoxId, kind, parse(amount), date, note, now);
    }

    /** Edição: não troca de cofrinho; preserva {@code id} e criação. */
    public MoneyBoxEntry update(EntryKind newKind, String newAmount, LocalDate newDate, String newNote) {
        return new MoneyBoxEntry(id, ownerId, moneyBoxId, newKind, parse(newAmount), newDate, newNote, createdAt);
    }

    /** Positivo no depósito, negativo no saque. */
    public BigDecimal signedAmount() {
        return kind == EntryKind.DEPOSIT ? amount : amount.negate();
    }

    private static BigDecimal parse(String raw) {
        BigDecimal value = Amounts.parse(raw, "amount");
        if (value == null) {
            throw new FieldValidationException("amount", "validation.amount.required");
        }
        return value;
    }
}
