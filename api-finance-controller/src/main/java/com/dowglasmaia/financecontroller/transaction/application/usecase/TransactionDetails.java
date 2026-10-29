package com.dowglasmaia.financecontroller.transaction.application.usecase;

import com.dowglasmaia.financecontroller.category.CategoryDirectory.CategoryView;
import com.dowglasmaia.financecontroller.category.CategoryDirectory.SubcategoryView;
import com.dowglasmaia.financecontroller.transaction.BudgetAlert;
import com.dowglasmaia.financecontroller.transaction.domain.Transaction;

import java.util.Optional;

/** Lançamento com a categoria e a subcategoria resolvidas para exibição ({@code subcategory} pode ser nula). */
public record TransactionDetails(Transaction transaction, CategoryView category, SubcategoryView subcategory) {

    /** Resposta de criação e edição: o lançamento e o alerta de meta, se houver. */
    public record Result(TransactionDetails details, Optional<BudgetAlert> budgetAlert) {
    }
}
