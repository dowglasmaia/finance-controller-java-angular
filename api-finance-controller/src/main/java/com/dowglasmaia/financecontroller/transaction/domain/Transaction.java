package com.dowglasmaia.financecontroller.transaction.domain;

import com.dowglasmaia.financecontroller.shared.error.FieldValidationException;
import com.dowglasmaia.financecontroller.shared.money.Currency;
import com.dowglasmaia.financecontroller.transaction.TransactionKind;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Lançamento financeiro (RN-TXN-01…09, RN-VAL-01). O valor é um decimal exato com escala 2, maior que zero e até
 * {@code 9 999 999 999,99}; o saldo nunca é guardado (RN-SAL-01). A compatibilidade com a categoria é verificada nos
 * casos de uso, que conhecem as categorias.
 */
public record Transaction(UUID id, UUID ownerId, TransactionKind kind, String description, BigDecimal amount,
                          Currency currency, LocalDate date, UUID categoryId, UUID subcategoryId, String notes,
                          Instant createdAt, Instant updatedAt) {

    public static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999.99");
    public static final LocalDate MIN_DATE = LocalDate.of(2000, 1, 1);
    public static final LocalDate MAX_DATE = LocalDate.of(2100, 12, 31);
    private static final int MAX_DESCRIPTION = 120;
    private static final int MAX_NOTES = 500;
    private static final Pattern AMOUNT_FORMAT = Pattern.compile("^\\d{1,10}(\\.\\d{1,2})?$");

    public Transaction {
        description = requireDescription(description);
        amount = requireAmount(amount);
        notes = normalizeNotes(notes);
        if (date == null || date.isBefore(MIN_DATE) || date.isAfter(MAX_DATE)) {
            throw new FieldValidationException("date", "validation.date.range");
        }
        if (id == null || ownerId == null || kind == null || currency == null || categoryId == null
                || createdAt == null || updatedAt == null) {
            throw new FieldValidationException("body", "validation.invalid");
        }
        // o PostgreSQL guarda microssegundos: truncar aqui evita que o valor devolvido ao gravar difira do lido depois
        createdAt = createdAt.truncatedTo(ChronoUnit.MICROS);
        updatedAt = updatedAt.truncatedTo(ChronoUnit.MICROS);
    }

    /** Novo lançamento; {@code createdAt} e {@code updatedAt} recebem {@code now}. */
    public static Transaction create(UUID ownerId, TransactionKind kind, String description, String amount,
                                     Currency currency, LocalDate date, UUID categoryId, UUID subcategoryId,
                                     String notes, Instant now) {
        return new Transaction(UUID.randomUUID(), ownerId, kind, description, parseAmount(amount), currency, date,
            categoryId, subcategoryId, notes, now, now);
    }

    /** Edição: preserva {@code id}, proprietário e {@code createdAt}; atualiza {@code updatedAt}. */
    public Transaction update(TransactionKind newKind, String newDescription, String newAmount, Currency newCurrency,
                              LocalDate newDate, UUID newCategoryId, UUID newSubcategoryId, String newNotes,
                              Instant now) {
        return new Transaction(id, ownerId, newKind, newDescription, parseAmount(newAmount), newCurrency, newDate,
            newCategoryId, newSubcategoryId, newNotes, createdAt, now);
    }

    /** Aceita só o formato da API ({@code "1234.56"}); vírgula, sinal e notação científica são recusados. */
    public static BigDecimal parseAmount(String raw) {
        if (raw == null || !AMOUNT_FORMAT.matcher(raw.strip()).matches()) {
            throw new FieldValidationException("amount", "validation.invalid");
        }
        return new BigDecimal(raw.strip());
    }

    private static BigDecimal requireAmount(BigDecimal value) {
        if (value == null) {
            throw new FieldValidationException("amount", "validation.amount.required");
        }
        if (value.signum() <= 0) {
            throw new FieldValidationException("amount", "validation.amount.positive");
        }
        if (value.compareTo(MAX_AMOUNT) > 0) {
            throw new FieldValidationException("amount", "validation.amount.max");
        }
        if (value.scale() > 2) {
            throw new FieldValidationException("amount", "validation.invalid");
        }
        return value.setScale(2);
    }

    private static String requireDescription(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new FieldValidationException("description", "validation.required");
        }
        String value = raw.strip();
        if (value.length() > MAX_DESCRIPTION) {
            throw new FieldValidationException("description", "validation.description.length");
        }
        return value;
    }

    private static String normalizeNotes(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.strip();
        if (value.length() > MAX_NOTES) {
            throw new FieldValidationException("notes", "validation.notes.length");
        }
        return value;
    }
}
