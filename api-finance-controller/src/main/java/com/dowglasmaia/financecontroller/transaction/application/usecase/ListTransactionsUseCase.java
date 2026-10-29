package com.dowglasmaia.financecontroller.transaction.application.usecase;

import com.dowglasmaia.financecontroller.category.CategoryDirectory;
import com.dowglasmaia.financecontroller.category.CategoryDirectory.CategoryView;
import com.dowglasmaia.financecontroller.identity.CurrentOwner;
import com.dowglasmaia.financecontroller.shared.money.Currency;
import com.dowglasmaia.financecontroller.transaction.application.port.out.TransactionRepository;
import com.dowglasmaia.financecontroller.transaction.domain.Totals;
import com.dowglasmaia.financecontroller.transaction.domain.Transaction;
import com.dowglasmaia.financecontroller.transaction.domain.TransactionKind;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ListTransactionsUseCase {

    private final TransactionRepository transactions;
    private final CategoryDirectory categories;
    private final CurrentOwner owner;

    ListTransactionsUseCase(TransactionRepository transactions, CategoryDirectory categories, CurrentOwner owner) {
        this.transactions = transactions;
        this.categories = categories;
        this.owner = owner;
    }

    /** Página do mês e da moeda; os totais cobrem o conjunto filtrado inteiro, não só a página (DESC-TXN-09). */
    @Transactional(readOnly = true)
    public Listing execute(YearMonth month, Currency currency, TransactionKind kind, int page, int size) {
        var ownerId = owner.id();
        var slice = transactions.findPage(ownerId, month, currency, kind, page, size);
        Set<UUID> categoryIds = slice.items().stream().map(Transaction::categoryId).collect(Collectors.toSet());
        Map<UUID, CategoryView> byId = categories.findAll(ownerId, categoryIds);
        List<TransactionDetails> items = slice.items().stream()
            .map(t -> {
                CategoryView category = byId.get(t.categoryId());
                return new TransactionDetails(t, category, TransactionRules.subcategoryOf(category, t.subcategoryId()));
            })
            .toList();
        long totalPages = Math.max(1, (slice.totalItems() + size - 1) / size);
        return new Listing(items, page, size, slice.totalItems(), (int) totalPages,
            transactions.totals(ownerId, month, currency).only(kind));
    }

    public record Listing(List<TransactionDetails> items, int page, int size, long totalItems, int totalPages,
                          Totals totals) {
    }
}
