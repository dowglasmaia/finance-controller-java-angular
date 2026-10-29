package com.dowglasmaia.financecontroller.moneybox.domain;

import com.dowglasmaia.financecontroller.shared.error.FieldValidationException;
import com.dowglasmaia.financecontroller.shared.money.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Cofrinho (RN-BOX-01). O saldo nunca é guardado: deriva das movimentações (RN-BOX-02). Meta {@code 0} ou ausente
 * significa "sem meta" e fica {@code null}.
 */
public record MoneyBox(UUID id, UUID ownerId, String name, String description, String icon, String color,
                       Currency currency, BigDecimal goal, Instant createdAt) {

    private static final Pattern COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    public MoneyBox {
        name = requireText(name, "name", 100);
        description = optionalText(description, "description", 255);
        icon = requireText(icon, "icon", 40);
        if (color == null || !COLOR.matcher(color.strip()).matches()) {
            throw new FieldValidationException("color", "validation.invalid");
        }
        color = color.strip();
        if (id == null || ownerId == null || currency == null || createdAt == null) {
            throw new FieldValidationException("body", "validation.invalid");
        }
        if (goal != null && (goal.signum() < 0 || goal.compareTo(Amounts.MAX) > 0)) {
            throw new FieldValidationException("goal", "validation.invalid");
        }
        goal = goal == null || goal.signum() == 0 ? null : goal.setScale(2);
        createdAt = createdAt.truncatedTo(ChronoUnit.MICROS);
    }

    public static MoneyBox create(UUID ownerId, String name, String description, String icon, String color,
                                  Currency currency, String goal, Instant now) {
        return new MoneyBox(UUID.randomUUID(), ownerId, name, description, icon, color, currency,
            Amounts.parse(goal, "goal"), now);
    }

    /** Edição: preserva {@code id}, proprietário e criação. */
    public MoneyBox update(String newName, String newDescription, String newIcon, String newColor,
                           Currency newCurrency, String newGoal) {
        return new MoneyBox(id, ownerId, newName, newDescription, newIcon, newColor, newCurrency,
            Amounts.parse(newGoal, "goal"), createdAt);
    }

    private static String requireText(String raw, String field, int max) {
        if (raw == null || raw.isBlank()) {
            throw new FieldValidationException(field, "validation.required");
        }
        String value = raw.strip();
        if (value.length() > max) {
            throw new FieldValidationException(field, "validation.text.length");
        }
        return value;
    }

    private static String optionalText(String raw, String field, int max) {
        return raw == null || raw.isBlank() ? null : requireText(raw, field, max);
    }
}
