package com.dowglasmaia.financecontroller.preferences.domain;

import com.dowglasmaia.financecontroller.shared.error.FieldValidationException;
import com.dowglasmaia.financecontroller.shared.money.Currency;

import java.util.UUID;

/** Moeda e tema de um utilizador. Os padrões (BRL e DARK) valem enquanto ele nunca gravou. */
public record Preference(UUID ownerId, Currency currency, ThemeMode theme) {

    public static final Currency DEFAULT_CURRENCY = Currency.BRL;
    public static final ThemeMode DEFAULT_THEME = ThemeMode.DARK;

    public Preference {
        if (ownerId == null) {
            throw new FieldValidationException("body", "validation.invalid");
        }
        if (currency == null) {
            throw new FieldValidationException("currency", "validation.required");
        }
        if (theme == null) {
            throw new FieldValidationException("theme", "validation.required");
        }
    }

    public static Preference defaults(UUID ownerId) {
        return new Preference(ownerId, DEFAULT_CURRENCY, DEFAULT_THEME);
    }
}
