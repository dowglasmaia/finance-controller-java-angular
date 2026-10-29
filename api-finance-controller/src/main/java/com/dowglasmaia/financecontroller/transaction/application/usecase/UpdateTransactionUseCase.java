package com.dowglasmaia.financecontroller.transaction.application.usecase;

import com.dowglasmaia.financecontroller.category.CategoryDirectory;
import com.dowglasmaia.financecontroller.identity.CurrentOwner;
import com.dowglasmaia.financecontroller.shared.error.ResourceNotFoundException;
import com.dowglasmaia.financecontroller.transaction.application.port.out.BudgetImpactPort;
import com.dowglasmaia.financecontroller.transaction.application.port.out.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class UpdateTransactionUseCase {

    private final TransactionRepository transactions;
    private final CategoryDirectory categories;
    private final BudgetImpactPort budgets;
    private final CurrentOwner owner;
    private final Clock clock;

    UpdateTransactionUseCase(TransactionRepository transactions, CategoryDirectory categories, BudgetImpactPort budgets,
                             CurrentOwner owner, Clock clock) {
        this.transactions = transactions;
        this.categories = categories;
        this.budgets = budgets;
        this.owner = owner;
        this.clock = clock;
    }

    @Transactional
    public TransactionDetails.Result execute(UUID transactionId, TransactionCommand command) {
        var ownerId = owner.id();
        var current = transactions.findById(ownerId, transactionId).orElseThrow(ResourceNotFoundException::new);
        var updated = current.update(command.kind(), command.description(), command.amount(), command.currency(),
            command.date(), command.categoryId(), command.subcategoryId(), command.notes(), clock.instant());
        var category = TransactionRules.requireCompatibleCategory(categories, ownerId, command.kind(),
            command.categoryId(), command.subcategoryId());
        // o "antes" exclui o valor antigo deste lançamento
        var alert = TransactionRules.budgetAlert(transactions, budgets, updated, category, command.referenceMonth(),
            transactionId);
        var saved = transactions.save(updated);
        var details = new TransactionDetails(saved, category, TransactionRules.subcategoryOf(category, saved.subcategoryId()));
        return new TransactionDetails.Result(details, alert);
    }
}
