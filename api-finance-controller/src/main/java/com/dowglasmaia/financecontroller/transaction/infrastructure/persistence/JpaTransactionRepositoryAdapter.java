package com.dowglasmaia.financecontroller.transaction.infrastructure.persistence;

import com.dowglasmaia.financecontroller.shared.money.Currency;
import com.dowglasmaia.financecontroller.transaction.TransactionQuery;
import com.dowglasmaia.financecontroller.transaction.application.port.out.TransactionRepository;
import com.dowglasmaia.financecontroller.transaction.domain.Totals;
import com.dowglasmaia.financecontroller.transaction.domain.Transaction;
import com.dowglasmaia.financecontroller.transaction.TransactionKind;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaTransactionRepositoryAdapter implements TransactionRepository {

    /** Valor que nunca é id de lançamento: "não excluir nenhum" sem precisar de parâmetro nulo na consulta. */
    private static final UUID NONE = new UUID(0L, 0L);

    private final SpringDataTransactionRepository transactions;

    @PersistenceContext
    private EntityManager entityManager;

    JpaTransactionRepositoryAdapter(SpringDataTransactionRepository transactions) {
        this.transactions = transactions;
    }

    @Override
    @Transactional(readOnly = true)
    public Page findPage(UUID ownerId, YearMonth month, Currency currency, TransactionKind kind, int page, int size) {
        var pageable = PageRequest.of(page - 1, size);
        var from = month.atDay(1);
        var to = month.atEndOfMonth();
        var result = kind == null
            ? transactions.findPage(ownerId, currency.name(), from, to, pageable)
            : transactions.findPageByKind(ownerId, currency.name(), from, to, kind.name(), pageable);
        return new Page(result.getContent().stream().map(JpaTransactionRepositoryAdapter::toDomain).toList(),
            result.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Totals totals(UUID ownerId, YearMonth month, Currency currency) {
        BigDecimal income = Totals.ZERO.income();
        BigDecimal expense = Totals.ZERO.expense();
        for (Object[] row : transactions.sumByKind(ownerId, currency.name(), month.atDay(1), month.atEndOfMonth())) {
            var sum = ((BigDecimal) row[1]).setScale(2);
            if (TransactionKind.INCOME.name().equals(row[0])) {
                income = sum;
            } else {
                expense = sum;
            }
        }
        return new Totals(income, expense);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal sumExpenses(UUID ownerId, YearMonth month, Currency currency, UUID categoryId, UUID excludingId) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        UUID excluded = excludingId == null ? NONE : excludingId;
        BigDecimal sum = categoryId == null
            ? transactions.sumExpenses(ownerId, currency.name(), from, to, excluded)
            : transactions.sumExpensesOfCategory(ownerId, currency.name(), from, to, categoryId, excluded);
        return sum.setScale(2);
    }

    @Override
    @Transactional
    public void lockMonth(UUID ownerId, YearMonth month, Currency currency) {
        entityManager.createNativeQuery(
                "select 1 from (select pg_advisory_xact_lock(hashtextextended(:key, 0))) as locked")
            .setParameter("key", "expenses:" + ownerId + ":" + currency.name() + ":" + month)
            .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionQuery.DailyTotals> dailyTotals(UUID ownerId, YearMonth month, Currency currency) {
        Map<LocalDate, BigDecimal[]> byDay = new HashMap<>();
        for (Object[] row : transactions.sumByDayAndKind(ownerId, currency.name(), month.atDay(1), month.atEndOfMonth())) {
            BigDecimal[] sums = byDay.computeIfAbsent((LocalDate) row[0],
                d -> new BigDecimal[] {BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2)});
            int slot = TransactionKind.INCOME.name().equals(row[1]) ? 0 : 1;
            sums[slot] = ((BigDecimal) row[2]).setScale(2);
        }
        return byDay.entrySet().stream()
            .map(e -> new TransactionQuery.DailyTotals(e.getKey(), e.getValue()[0], e.getValue()[1]))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, BigDecimal> sumExpensesByCategory(UUID ownerId, YearMonth month, Currency currency) {
        Map<UUID, BigDecimal> totals = new HashMap<>();
        for (Object[] row : transactions.sumExpensesByCategory(ownerId, currency.name(), month.atDay(1), month.atEndOfMonth())) {
            totals.put((UUID) row[0], ((BigDecimal) row[1]).setScale(2));
        }
        return totals;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Transaction> findById(UUID ownerId, UUID transactionId) {
        return transactions.findByOwnerIdAndId(ownerId, transactionId).map(JpaTransactionRepositoryAdapter::toDomain);
    }

    /** Flush imediato: violações de integridade surgem dentro do caso de uso. */
    @Override
    @Transactional
    public Transaction save(Transaction t) {
        var entity = transactions.findByOwnerIdAndId(t.ownerId(), t.id())
            .orElseGet(() -> new TransactionEntity(t.id(), t.ownerId(), t.createdAt()));
        entity.update(t.kind().name(), t.description(), t.amount(), t.currency().name(), t.date(), t.categoryId(),
            t.subcategoryId(), t.notes(), t.updatedAt());
        return toDomain(transactions.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public boolean delete(UUID ownerId, UUID transactionId) {
        var entity = transactions.findByOwnerIdAndId(ownerId, transactionId);
        entity.ifPresent(transactions::delete);
        transactions.flush();
        return entity.isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCategory(UUID ownerId, UUID categoryId) {
        return transactions.existsByOwnerIdAndCategoryId(ownerId, categoryId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCategoryAndKind(UUID ownerId, UUID categoryId, TransactionKind kind) {
        return transactions.existsByOwnerIdAndCategoryIdAndKind(ownerId, categoryId, kind.name());
    }

    private static Transaction toDomain(TransactionEntity e) {
        return new Transaction(e.getId(), e.getOwnerId(), TransactionKind.valueOf(e.getKind()), e.getDescription(),
            e.getAmount(), Currency.valueOf(e.getCurrency()), e.getEntryDate(), e.getCategoryId(),
            e.getSubcategoryId(), e.getNotes(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
