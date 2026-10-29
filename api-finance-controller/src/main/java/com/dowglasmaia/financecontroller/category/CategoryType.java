package com.dowglasmaia.financecontroller.category;

/** Tipo de lançamento que a categoria aceita; {@code BOTH} aceita despesas e receitas. */
public enum CategoryType {
    INCOME, EXPENSE, BOTH;

    /** Verdadeiro se uma categoria deste tipo aceita lançamentos do tipo {@code kind} ({@code INCOME} ou {@code EXPENSE}). */
    public boolean accepts(CategoryType kind) {
        return this == BOTH || this == kind;
    }
}
