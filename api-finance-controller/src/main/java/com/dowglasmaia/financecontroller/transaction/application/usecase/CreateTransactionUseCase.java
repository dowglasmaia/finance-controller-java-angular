package com.dowglasmaia.financecontroller.transaction.application.usecase;

import com.dowglasmaia.financecontroller.category.CategoryDirectory;
import com.dowglasmaia.financecontroller.identity.CurrentOwner;
import com.dowglasmaia.financecontroller.transaction.BudgetImpactPort;
import com.dowglasmaia.financecontroller.transaction.application.port.out.TransactionRepository;
import com.dowglasmaia.financecontroller.transaction.domain.Transaction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class CreateTransactionUseCase {

    private final TransactionRepository transactions;
    private final CategoryDirectory categories;
    private final BudgetImpactPort budgets;
    private final CurrentOwner owner;
    private final Clock clock;

    CreateTransactionUseCase(TransactionRepository transactions, CategoryDirectory categories, BudgetImpactPort budgets,
                             CurrentOwner owner, Clock clock) {
        this.transactions = transactions;
        this.categories = categories;
        this.budgets = budgets;
        this.owner = owner;
        this.clock = clock;
    }

    @Transactional
    public TransactionDetails.Result execute(TransactionCommand command) {
        var ownerId = owner.id();
        var transaction = Transaction.create(ownerId, command.kind(), command.description(), command.amount(),
            command.currency(), command.date(), command.categoryId(), command.subcategoryId(), command.notes(),
            clock.instant());
        var category = TransactionRules.requireCompatibleCategory(categories, ownerId, command.kind(),
            command.categoryId(), command.subcategoryId());
        var alert = TransactionRules.budgetAlert(transactions, budgets, transaction, category,
            command.referenceMonth(), null);
        var saved = transactions.save(transaction);
        var details = new TransactionDetails(saved, category, TransactionRules.subcategoryOf(category, saved.subcategoryId()));
        return new TransactionDetails.Result(details, alert);
    }
}
