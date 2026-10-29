package com.dowglasmaia.financecontroller.transaction.application.usecase;

import com.dowglasmaia.financecontroller.category.CategoryDirectory;
import com.dowglasmaia.financecontroller.identity.CurrentOwner;
import com.dowglasmaia.financecontroller.shared.error.ResourceNotFoundException;
import com.dowglasmaia.financecontroller.transaction.application.port.out.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetTransactionUseCase {

    private final TransactionRepository transactions;
    private final CategoryDirectory categories;
    private final CurrentOwner owner;

    GetTransactionUseCase(TransactionRepository transactions, CategoryDirectory categories, CurrentOwner owner) {
        this.transactions = transactions;
        this.categories = categories;
        this.owner = owner;
    }

    @Transactional(readOnly = true)
    public TransactionDetails execute(UUID transactionId) {
        var ownerId = owner.id();
        var transaction = transactions.findById(ownerId, transactionId).orElseThrow(ResourceNotFoundException::new);
        var category = categories.find(ownerId, transaction.categoryId()).orElseThrow(ResourceNotFoundException::new);
        return new TransactionDetails(transaction, category,
            TransactionRules.subcategoryOf(category, transaction.subcategoryId()));
    }
}
