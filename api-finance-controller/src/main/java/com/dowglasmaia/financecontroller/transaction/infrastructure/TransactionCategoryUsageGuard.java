package com.dowglasmaia.financecontroller.transaction.infrastructure;

import com.dowglasmaia.financecontroller.category.CategoryType;
import com.dowglasmaia.financecontroller.category.CategoryUsageGuard;
import com.dowglasmaia.financecontroller.transaction.application.port.out.TransactionRepository;
import com.dowglasmaia.financecontroller.transaction.TransactionKind;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Responde ao módulo de categorias com base nos lançamentos reais (bloqueio de exclusão e de mudança de tipo). */
@Component
class TransactionCategoryUsageGuard implements CategoryUsageGuard {

    private final TransactionRepository transactions;

    TransactionCategoryUsageGuard(TransactionRepository transactions) {
        this.transactions = transactions;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInUse(UUID ownerId, UUID categoryId) {
        return transactions.existsByCategory(ownerId, categoryId);
    }

    /** Despesa → receita é incompatível se houver despesas; receita → despesa, se houver receitas; {@code BOTH} aceita tudo. */
    @Override
    @Transactional(readOnly = true)
    public boolean hasTransactionsIncompatibleWith(UUID ownerId, UUID categoryId, CategoryType newType) {
        return switch (newType) {
            case INCOME -> transactions.existsByCategoryAndKind(ownerId, categoryId, TransactionKind.EXPENSE);
            case EXPENSE -> transactions.existsByCategoryAndKind(ownerId, categoryId, TransactionKind.INCOME);
            case BOTH -> false;
        };
    }
}
