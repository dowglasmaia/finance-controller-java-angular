package com.dowglasmaia.financecontroller.transaction;

import java.util.UUID;

/** Alerta de meta devolvido ao salvar uma despesa (ADR-005); o texto é composto no cliente. */
public record BudgetAlert(Scope scope, Status status, UUID categoryId, String categoryName) {

    public enum Scope { CATEGORY, GLOBAL }

    public enum Status { WARNING, EXCEEDED }
}
