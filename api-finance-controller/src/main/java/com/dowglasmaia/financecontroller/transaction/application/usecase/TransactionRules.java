package com.dowglasmaia.financecontroller.transaction.application.usecase;

import com.dowglasmaia.financecontroller.category.CategoryDirectory;
import com.dowglasmaia.financecontroller.category.CategoryDirectory.CategoryView;
import com.dowglasmaia.financecontroller.category.CategoryDirectory.SubcategoryView;
import com.dowglasmaia.financecontroller.category.CategoryType;
import com.dowglasmaia.financecontroller.shared.error.DomainException;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;
import com.dowglasmaia.financecontroller.shared.error.ResourceNotFoundException;
import com.dowglasmaia.financecontroller.transaction.BudgetImpactPort;
import com.dowglasmaia.financecontroller.transaction.application.port.out.TransactionRepository;
import com.dowglasmaia.financecontroller.transaction.BudgetAlert;
import com.dowglasmaia.financecontroller.transaction.domain.Transaction;
import com.dowglasmaia.financecontroller.transaction.TransactionKind;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

/** Regras compartilhadas por criação e edição: compatibilidade com a categoria (RN-TXN-03/04) e alerta de meta. */
final class TransactionRules {

    private TransactionRules() {
    }

    /**
     * A categoria deve existir para o proprietário (senão 404), aceitar a direção ({@code CATEGORY_TYPE_MISMATCH}) e,
     * se houver subcategoria, conter-na ({@code SUBCATEGORY_NOT_IN_CATEGORY}).
     */
    static CategoryView requireCompatibleCategory(CategoryDirectory categories, UUID ownerId, TransactionKind kind,
                                                  UUID categoryId, UUID subcategoryId) {
        CategoryView category = categories.find(ownerId, categoryId).orElseThrow(ResourceNotFoundException::new);
        CategoryType needed = CategoryType.valueOf(kind.name());
        if (!category.type().accepts(needed)) {
            throw new DomainException(ErrorCode.CATEGORY_TYPE_MISMATCH);
        }
        if (subcategoryId != null && subcategoryOf(category, subcategoryId) == null) {
            throw new DomainException(ErrorCode.SUBCATEGORY_NOT_IN_CATEGORY);
        }
        return category;
    }

    static SubcategoryView subcategoryOf(CategoryView category, UUID subcategoryId) {
        if (subcategoryId == null) {
            return null;
        }
        return category.subcategories().stream().filter(s -> s.id().equals(subcategoryId)).findFirst().orElse(null);
    }

    /**
     * Só despesas cuja data pertence ao mês que o cliente exibe (P-20). O "antes" exclui o próprio lançamento, para a
     * edição não contar o valor antigo duas vezes; deve ser chamado antes de gravar.
     */
    static Optional<BudgetAlert> budgetAlert(TransactionRepository transactions, BudgetImpactPort budgets,
                                             Transaction transaction, CategoryView category,
                                             YearMonth referenceMonth, UUID excludingId) {
        if (transaction.kind() != TransactionKind.EXPENSE || referenceMonth == null
                || !YearMonth.from(transaction.date()).equals(referenceMonth)) {
            return Optional.empty();
        }
        UUID owner = transaction.ownerId();
        transactions.lockMonth(owner, referenceMonth, transaction.currency());
        BigDecimal monthBefore = transactions.sumExpenses(owner, referenceMonth, transaction.currency(), null, excludingId);
        BigDecimal categoryBefore = transactions.sumExpenses(owner, referenceMonth, transaction.currency(),
            category.id(), excludingId);
        return budgets.evaluate(new BudgetImpactPort.Impact(owner, transaction.currency(), referenceMonth,
            category.id(), category.name(), categoryBefore, categoryBefore.add(transaction.amount()), monthBefore,
            monthBefore.add(transaction.amount())));
    }
}
