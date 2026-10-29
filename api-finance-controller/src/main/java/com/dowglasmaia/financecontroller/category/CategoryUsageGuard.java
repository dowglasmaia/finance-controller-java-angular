package com.dowglasmaia.financecontroller.category;

import java.util.UUID;

/**
 * Porta de fronteira para o módulo de lançamentos (F5): o módulo {@code category} não depende dele, apenas pergunta.
 * Sem lançamentos implementados, a implementação padrão responde "sem uso".
 */
public interface CategoryUsageGuard {

    /** Há lançamentos vinculados à categoria (bloqueia a exclusão). */
    boolean isInUse(UUID ownerId, UUID categoryId);

    /** Há lançamentos incompatíveis com o novo tipo da categoria (P-13). */
    boolean hasTransactionsIncompatibleWith(UUID ownerId, UUID categoryId, CategoryType newType);
}
