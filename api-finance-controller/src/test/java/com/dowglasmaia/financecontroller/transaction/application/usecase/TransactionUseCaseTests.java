package com.dowglasmaia.financecontroller.transaction.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dowglasmaia.financecontroller.category.CategoryDirectory;
import com.dowglasmaia.financecontroller.category.CategoryDirectory.CategoryView;
import com.dowglasmaia.financecontroller.category.CategoryDirectory.SubcategoryView;
import com.dowglasmaia.financecontroller.category.CategoryType;
import com.dowglasmaia.financecontroller.identity.CurrentOwner;
import com.dowglasmaia.financecontroller.shared.error.DomainException;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;
import com.dowglasmaia.financecontroller.shared.money.Currency;
import com.dowglasmaia.financecontroller.transaction.application.port.out.BudgetImpactPort;
import com.dowglasmaia.financecontroller.transaction.application.port.out.TransactionRepository;
import com.dowglasmaia.financecontroller.transaction.domain.BudgetAlert;
import com.dowglasmaia.financecontroller.transaction.domain.Totals;
import com.dowglasmaia.financecontroller.transaction.domain.Transaction;
import com.dowglasmaia.financecontroller.transaction.domain.TransactionKind;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class TransactionUseCaseTests {
    private static final YearMonth OCT = YearMonth.of(2026, 10);

    private final UUID ownerId = UUID.randomUUID();
    private final Fakes fakes = new Fakes();
    private final CurrentOwner owner = () -> ownerId;
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-29T10:00:00Z"), ZoneOffset.UTC);

    private final CreateTransactionUseCase create = new CreateTransactionUseCase(fakes.transactions, fakes.categories, fakes.budgets, owner, clock);
    private final UpdateTransactionUseCase update = new UpdateTransactionUseCase(fakes.transactions, fakes.categories, fakes.budgets, owner, clock);
    private final GetTransactionUseCase get = new GetTransactionUseCase(fakes.transactions, fakes.categories, owner);
    private final DeleteTransactionUseCase delete = new DeleteTransactionUseCase(fakes.transactions, owner);
    private final ListTransactionsUseCase list = new ListTransactionsUseCase(fakes.transactions, fakes.categories, owner);

    private final UUID food = fakes.category(ownerId, "Alimentação", CategoryType.EXPENSE, "Mercado");
    private final UUID salary = fakes.category(ownerId, "Salário", CategoryType.INCOME);
    private final UUID both = fakes.category(ownerId, "Extras", CategoryType.BOTH);

    private TransactionCommand expense(String amount, LocalDate date, UUID category, YearMonth reference) {
        return new TransactionCommand(TransactionKind.EXPENSE, "Compra", amount, Currency.BRL, date, category, null, null, reference);
    }

    private static void assertCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(DomainException.class, e -> assertThat(e.code()).isEqualTo(expected));
    }

    @Test
    void incompatibleCategoryIsA409ButBothAcceptsEverything() {
        var income = new TransactionCommand(TransactionKind.INCOME, "Pago", "10.00", Currency.BRL, LocalDate.of(2026, 10, 1), food, null, null, null);
        assertCode(() -> create.execute(income), ErrorCode.CATEGORY_TYPE_MISMATCH);
        assertCode(() -> create.execute(expense("10.00", LocalDate.of(2026, 10, 1), salary, null)), ErrorCode.CATEGORY_TYPE_MISMATCH);

        assertThat(create.execute(expense("10.00", LocalDate.of(2026, 10, 1), both, null)).details().transaction().kind())
            .isEqualTo(TransactionKind.EXPENSE);
        assertThat(fakes.transactions.all).hasSize(1);
    }

    @Test
    void subcategoryMustBelongToTheCategory() {
        var otherCategoryScope = fakes.category(ownerId, "Lazer", CategoryType.EXPENSE, "Cinema");
        var foreignSub = fakes.subcategoryOf(otherCategoryScope, "Cinema");
        var cmd = new TransactionCommand(TransactionKind.EXPENSE, "x", "5.00", Currency.BRL, LocalDate.of(2026, 10, 1), food, foreignSub, null, null);

        assertCode(() -> create.execute(cmd), ErrorCode.SUBCATEGORY_NOT_IN_CATEGORY);

        var ownSub = fakes.subcategoryOf(food, "Mercado");
        var ok = create.execute(new TransactionCommand(TransactionKind.EXPENSE, "x", "5.00", Currency.BRL,
            LocalDate.of(2026, 10, 1), food, ownSub, null, null));
        assertThat(ok.details().subcategory().name()).isEqualTo("Mercado");
    }

    @Test
    void anotherUsersCategoryLooksLikeNotFound() {
        var foreign = fakes.category(UUID.randomUUID(), "Alheia", CategoryType.EXPENSE);

        assertCode(() -> create.execute(expense("5.00", LocalDate.of(2026, 10, 1), foreign, null)), ErrorCode.RESOURCE_NOT_FOUND);
        assertCode(() -> create.execute(expense("5.00", LocalDate.of(2026, 10, 1), UUID.randomUUID(), null)), ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void anotherUsersTransactionIsInvisibleToGetUpdateAndDelete() {
        var foreignId = fakes.transactions.seed(UUID.randomUUID(), TransactionKind.EXPENSE, "10.00", Currency.BRL,
            LocalDate.of(2026, 10, 1), food);

        assertCode(() -> get.execute(foreignId), ErrorCode.RESOURCE_NOT_FOUND);
        assertCode(() -> update.execute(foreignId, expense("1.00", LocalDate.of(2026, 10, 1), food, null)), ErrorCode.RESOURCE_NOT_FOUND);
        assertCode(() -> delete.execute(foreignId), ErrorCode.RESOURCE_NOT_FOUND);
        assertThat(fakes.transactions.all).hasSize(1);
    }

    @Test
    void alertIsOnlyEvaluatedForExpensesInTheReferenceMonth() {
        create.execute(new TransactionCommand(TransactionKind.INCOME, "Pago", "100.00", Currency.BRL,
            LocalDate.of(2026, 10, 1), salary, null, null, OCT));
        assertThat(fakes.budgets.impacts).isEmpty();

        create.execute(expense("10.00", LocalDate.of(2026, 9, 30), food, OCT));
        assertThat(fakes.budgets.impacts).isEmpty();

        create.execute(expense("10.00", LocalDate.of(2026, 10, 3), food, null));
        assertThat(fakes.budgets.impacts).isEmpty();

        create.execute(expense("10.00", LocalDate.of(2026, 10, 3), food, OCT));
        assertThat(fakes.budgets.impacts).hasSize(1);
    }

    @Test
    void createPassesSpentBeforeAndAfterAndReturnsTheAlert() {
        fakes.transactions.seed(ownerId, TransactionKind.EXPENSE, "700.00", Currency.BRL, LocalDate.of(2026, 10, 2), food);
        fakes.transactions.seed(ownerId, TransactionKind.EXPENSE, "50.00", Currency.BRL, LocalDate.of(2026, 10, 2), both);
        fakes.transactions.seed(ownerId, TransactionKind.EXPENSE, "999.00", Currency.EUR, LocalDate.of(2026, 10, 2), food);
        fakes.budgets.answer = Optional.of(new BudgetAlert(BudgetAlert.Scope.CATEGORY, BudgetAlert.Status.WARNING, food, "Alimentação"));

        var result = create.execute(expense("150.00", LocalDate.of(2026, 10, 9), food, OCT));

        var impact = fakes.budgets.impacts.get(0);
        assertThat(impact.categorySpentBefore()).isEqualByComparingTo("700.00");
        assertThat(impact.categorySpentAfter()).isEqualByComparingTo("850.00");
        assertThat(impact.monthSpentBefore()).isEqualByComparingTo("750.00");
        assertThat(impact.monthSpentAfter()).isEqualByComparingTo("900.00");
        assertThat(impact.currency()).isEqualTo(Currency.BRL);
        assertThat(result.budgetAlert()).isPresent();
    }

    @Test
    void editDoesNotCountTheOldValueTwice() {
        var id = create.execute(expense("700.00", LocalDate.of(2026, 10, 2), food, null)).details().transaction().id();
        fakes.budgets.impacts.clear();

        update.execute(id, expense("850.00", LocalDate.of(2026, 10, 2), food, OCT));

        var impact = fakes.budgets.impacts.get(0);
        assertThat(impact.categorySpentBefore()).isEqualByComparingTo("0.00");
        assertThat(impact.categorySpentAfter()).isEqualByComparingTo("850.00");
        assertThat(impact.monthSpentBefore()).isEqualByComparingTo("0.00");
    }

    @Test
    void editingToIncomeNeverEvaluatesAnAlert() {
        var id = create.execute(expense("10.00", LocalDate.of(2026, 10, 2), both, null)).details().transaction().id();
        fakes.budgets.impacts.clear();

        update.execute(id, new TransactionCommand(TransactionKind.INCOME, "Virou receita", "10.00", Currency.BRL,
            LocalDate.of(2026, 10, 2), both, null, null, OCT));

        assertThat(fakes.budgets.impacts).isEmpty();
    }

    @Test
    void updatePreservesIdAndCreatedAt() {
        var created = create.execute(expense("10.00", LocalDate.of(2026, 10, 2), food, null)).details().transaction();
        var later = new UpdateTransactionUseCase(fakes.transactions, fakes.categories, fakes.budgets, owner,
            Clock.fixed(Instant.parse("2026-10-30T09:00:00Z"), ZoneOffset.UTC));

        var edited = later.execute(created.id(), expense("20.00", LocalDate.of(2026, 10, 3), food, null)).details().transaction();

        assertThat(edited.id()).isEqualTo(created.id());
        assertThat(edited.createdAt()).isEqualTo(created.createdAt());
        assertThat(edited.updatedAt()).isEqualTo(Instant.parse("2026-10-30T09:00:00Z"));
        assertThat(edited.amount()).isEqualByComparingTo("20.00");
    }

    @Test
    void listFiltersMonthCurrencyAndKindWithTotalsOfTheWholeFilteredSetAndPaginates() {
        for (int day = 1; day <= 20; day++) {
            fakes.transactions.seed(ownerId, TransactionKind.EXPENSE, "10.00", Currency.BRL, LocalDate.of(2026, 10, day), food);
        }
        fakes.transactions.seed(ownerId, TransactionKind.INCOME, "500.00", Currency.BRL, LocalDate.of(2026, 10, 5), salary);
        fakes.transactions.seed(ownerId, TransactionKind.EXPENSE, "10.00", Currency.BRL, LocalDate.of(2026, 9, 30), food);
        fakes.transactions.seed(ownerId, TransactionKind.EXPENSE, "77.00", Currency.EUR, LocalDate.of(2026, 10, 5), food);
        fakes.transactions.seed(UUID.randomUUID(), TransactionKind.EXPENSE, "1000.00", Currency.BRL, LocalDate.of(2026, 10, 5), food);

        var firstPage = list.execute(OCT, Currency.BRL, null, 1, 15);
        var secondPage = list.execute(OCT, Currency.BRL, null, 2, 15);
        var expensesOnly = list.execute(OCT, Currency.BRL, TransactionKind.EXPENSE, 1, 15);

        assertThat(firstPage.totalItems()).isEqualTo(21);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(firstPage.items()).hasSize(15);
        assertThat(secondPage.items()).hasSize(6);
        // totais do conjunto inteiro, não da página; EUR e outro utilizador ficam de fora
        assertThat(firstPage.totals().income()).isEqualByComparingTo("500.00");
        assertThat(firstPage.totals().expense()).isEqualByComparingTo("200.00");
        assertThat(firstPage.totals().balance()).isEqualByComparingTo("300.00");
        assertThat(expensesOnly.totalItems()).isEqualTo(20);
        assertThat(expensesOnly.totals().income()).isEqualByComparingTo("0.00");
        // mais recentes primeiro
        assertThat(firstPage.items().get(0).transaction().date()).isEqualTo(LocalDate.of(2026, 10, 20));
    }

    @Test
    void emptyMonthHasOnePageAndZeroTotals() {
        var empty = list.execute(YearMonth.of(2030, 1), Currency.BRL, null, 1, 15);

        assertThat(empty.items()).isEmpty();
        assertThat(empty.totalPages()).isEqualTo(1);
        assertThat(empty.totals()).isEqualTo(Totals.ZERO);
    }

    /** Implementações em memória das portas de saída e da API pública de categorias. */
    private static final class Fakes {
        final InMemoryTransactions transactions = new InMemoryTransactions();
        final RecordingBudgets budgets = new RecordingBudgets();
        final Directory categories = new Directory();

        UUID category(UUID owner, String name, CategoryType type, String... subs) {
            return categories.add(owner, name, type, subs);
        }

        UUID subcategoryOf(UUID categoryId, String name) {
            return categories.byId.get(categoryId).subcategories().stream()
                .filter(s -> s.name().equals(name)).findFirst().orElseThrow().id();
        }
    }

    private static final class Directory implements CategoryDirectory {
        final Map<UUID, CategoryView> byId = new java.util.HashMap<>();
        final Map<UUID, UUID> ownerOf = new java.util.HashMap<>();

        UUID add(UUID owner, String name, CategoryType type, String... subs) {
            var id = UUID.randomUUID();
            var subViews = new ArrayList<SubcategoryView>();
            for (String sub : subs) {
                subViews.add(new SubcategoryView(UUID.randomUUID(), sub));
            }
            byId.put(id, new CategoryView(id, name, "icon", "#112233", type, subViews));
            ownerOf.put(id, owner);
            return id;
        }

        @Override
        public Optional<CategoryView> find(UUID ownerId, UUID categoryId) {
            return ownerId.equals(ownerOf.get(categoryId)) ? Optional.of(byId.get(categoryId)) : Optional.empty();
        }

        @Override
        public Map<UUID, CategoryView> findAll(UUID ownerId, Collection<UUID> ids) {
            return ids.stream().filter(id -> ownerId.equals(ownerOf.get(id)))
                .collect(Collectors.toMap(id -> id, byId::get));
        }
    }

    private static final class RecordingBudgets implements BudgetImpactPort {
        final List<Impact> impacts = new ArrayList<>();
        Optional<BudgetAlert> answer = Optional.empty();

        @Override
        public Optional<BudgetAlert> evaluate(Impact impact) {
            impacts.add(impact);
            return answer;
        }
    }

    private static final class InMemoryTransactions implements TransactionRepository {
        final List<Transaction> all = new ArrayList<>();

        UUID seed(UUID owner, TransactionKind kind, String amount, Currency currency, LocalDate date, UUID category) {
            var t = Transaction.create(owner, kind, "seed", amount, currency, date, category, null, null,
                Instant.parse("2026-10-01T00:00:00Z").plusMillis(all.size()));
            all.add(t);
            return t.id();
        }

        private List<Transaction> scope(UUID owner, YearMonth month, Currency currency) {
            return all.stream().filter(t -> t.ownerId().equals(owner) && t.currency() == currency
                && YearMonth.from(t.date()).equals(month)).toList();
        }

        @Override
        public Page findPage(UUID ownerId, YearMonth month, Currency currency, TransactionKind kind, int page, int size) {
            var filtered = scope(ownerId, month, currency).stream()
                .filter(t -> kind == null || t.kind() == kind)
                .sorted(Comparator.comparing(Transaction::date).thenComparing(Transaction::createdAt)
                    .thenComparing(Transaction::id).reversed())
                .toList();
            int from = Math.min((page - 1) * size, filtered.size());
            return new Page(filtered.subList(from, Math.min(from + size, filtered.size())), filtered.size());
        }

        @Override
        public Totals totals(UUID ownerId, YearMonth month, Currency currency) {
            var scoped = scope(ownerId, month, currency);
            return new Totals(sum(scoped, TransactionKind.INCOME), sum(scoped, TransactionKind.EXPENSE));
        }

        private static BigDecimal sum(List<Transaction> list, TransactionKind kind) {
            return list.stream().filter(t -> t.kind() == kind).map(Transaction::amount)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        }

        @Override
        public BigDecimal sumExpenses(UUID ownerId, YearMonth month, Currency currency, UUID categoryId, UUID excludingId) {
            return scope(ownerId, month, currency).stream()
                .filter(t -> t.kind() == TransactionKind.EXPENSE)
                .filter(t -> categoryId == null || t.categoryId().equals(categoryId))
                .filter(t -> !t.id().equals(excludingId))
                .map(Transaction::amount).reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        }

        @Override
        public Optional<Transaction> findById(UUID ownerId, UUID transactionId) {
            return all.stream().filter(t -> t.ownerId().equals(ownerId) && t.id().equals(transactionId)).findFirst();
        }

        @Override
        public Transaction save(Transaction transaction) {
            all.removeIf(t -> t.id().equals(transaction.id()));
            all.add(transaction);
            return transaction;
        }

        @Override
        public boolean delete(UUID ownerId, UUID transactionId) {
            return all.removeIf(t -> t.ownerId().equals(ownerId) && t.id().equals(transactionId));
        }

        @Override
        public boolean existsByCategory(UUID ownerId, UUID categoryId) {
            return all.stream().anyMatch(t -> t.ownerId().equals(ownerId) && t.categoryId().equals(categoryId));
        }

        @Override
        public boolean existsByCategoryAndKind(UUID ownerId, UUID categoryId, TransactionKind kind) {
            return all.stream().anyMatch(t -> t.ownerId().equals(ownerId) && t.categoryId().equals(categoryId) && t.kind() == kind);
        }
    }
}
