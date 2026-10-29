package com.dowglasmaia.financecontroller.transaction.domain;

import java.math.BigDecimal;

/** Entradas e saídas de um conjunto de lançamentos de uma única moeda; o saldo é derivado (RN-SAL-01). */
public record Totals(BigDecimal income, BigDecimal expense) {

    public static final Totals ZERO = new Totals(BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2));

    public BigDecimal balance() {
        return income.subtract(expense);
    }

    /** Mantém só a direção filtrada ({@code null} mantém ambas). */
    public Totals only(TransactionKind kind) {
        if (kind == null) {
            return this;
        }
        return kind == TransactionKind.INCOME ? new Totals(income, ZERO.expense()) : new Totals(ZERO.income(), expense);
    }
}
