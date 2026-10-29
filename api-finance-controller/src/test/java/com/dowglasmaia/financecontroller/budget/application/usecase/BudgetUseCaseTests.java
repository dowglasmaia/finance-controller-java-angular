package com.dowglasmaia.financecontroller.budget.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dowglasmaia.financecontroller.budget.application.port.out.BudgetRepository;
import com.dowglasmaia.financecontroller.budget.domain.BudgetConfig;
import com.dowglasmaia.financecontroller.budget.domain.BudgetStatus;
import com.dowglasmaia.financecontroller.budget.domain.MonthOverride;
import com.dowglasmaia.financecontroller.category.CategoryDirectory;
import com.dowglasmaia.financecontroller.category.CategoryType;
import com.dowglasmaia.financecontroller.identity.CurrentOwner;
import com.dowglasmaia.financecontroller.shared.error.DomainException;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;
import com.dowglasmaia.financecontroller.shared.error.FieldValidationException;
import com.dowglasmaia.financecontroller.shared.money.Currency;
import com.dowglasmaia.financecontroller.transaction.ExpenseQuery;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class BudgetUseCaseTests {
    private static final YearMonth JULY = YearMonth.of(2026, 7);
    private static final YearMonth AUGUST = YearMonth.of(2026, 8);

    private final UUID ownerId = UUID.randomUUID();
    private final InMemoryBudgets repository = new InMemoryBudgets();
    private final Directory directory = new Directory();
    private final Expenses expenses = new Expenses();
    private final CurrentOwner owner = () -> ownerId;

    private final SaveBudgetUseCase save = new SaveBudgetUseCase(repository, directory, owner);
    private final GetBudgetUseCase get = new GetBudgetUseCase(repository, owner);
    private final GetBudgetProgressUseCase progress = new GetBudgetProgressUseCase(repository, directory, expenses, owner);

    private final UUID food = directory.add(ownerId, "Alimentação", CategoryType.EXPENSE);
    private final UUID leisure = directory.add(ownerId, "Lazer", CategoryType.BOTH);
    private final UUID salary = directory.add(ownerId, "Salário", CategoryType.INCOME);

    private SaveBudgetCommand command(YearMonth month, String monthly, String overrideLimit, boolean disabled,
                                      boolean overrideGiven, SaveBudgetCommand.CategoryLimit... limits) {
        return new SaveBudgetCommand(Currency.BRL, month, monthly, overrideLimit, disabled, overrideGiven, List.of(limits));
    }

    private static void assertCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(DomainException.class, e -> assertThat(e.code()).isEqualTo(expected));
    }

    @Test
    void overrideAppliesOnlyToItsMonthAndRemovingItRestoresTheRecurringLimit() {
        save.execute(command(JULY, "1000", "1500", false, true));

        assertThat(get.execute(Currency.BRL, JULY).effectiveLimit()).isEqualByComparingTo("1500.00");
        assertThat(get.execute(Currency.BRL, AUGUST).effectiveLimit()).isEqualByComparingTo("1000.00");

        save.execute(command(JULY, "1000", null, false, false));
        assertThat(get.execute(Currency.BRL, JULY).effectiveLimit()).isEqualByComparingTo("1000.00");
        assertThat(get.execute(Currency.BRL, JULY).monthOverride()).isNull();
    }

    @Test
    void disabledMonthHasNoLimitWhileOtherMonthsKeepTheRecurringOne() {
        save.execute(command(JULY, "1000", null, true, true));

        var july = get.execute(Currency.BRL, JULY);
        assertThat(july.monthOverride()).isEqualTo(MonthOverride.off());
        assertThat(july.effectiveLimit()).isNull();
        assertThat(progress.execute(Currency.BRL, JULY).global().status()).isEqualTo(BudgetStatus.NONE);
        assertThat(get.execute(Currency.BRL, AUGUST).effectiveLimit()).isEqualByComparingTo("1000.00");
    }

    @Test
    void zeroOrEmptyRemovesLimits() {
        save.execute(command(JULY, "1000", "1500", false, true, new SaveBudgetCommand.CategoryLimit(food, "200")));
        save.execute(command(JULY, "0", "0", false, true, new SaveBudgetCommand.CategoryLimit(food, "0")));

        var config = get.execute(Currency.BRL, JULY);
        assertThat(config.monthlyLimit()).isNull();
        assertThat(config.monthOverride()).isNull();
        assertThat(config.categoryLimits()).isEmpty();
    }

    @Test
    void categoryLimitsSetIsReplacedNotMerged() {
        save.execute(command(JULY, null, null, false, false,
            new SaveBudgetCommand.CategoryLimit(food, "200"), new SaveBudgetCommand.CategoryLimit(leisure, "100")));
        save.execute(command(JULY, null, null, false, false, new SaveBudgetCommand.CategoryLimit(leisure, "150")));

        assertThat(get.execute(Currency.BRL, JULY).categoryLimits()).containsOnlyKeys(leisure);
        assertThat(get.execute(Currency.BRL, JULY).categoryLimit(leisure)).isEqualByComparingTo("150.00");
    }

    @Test
    void invalidCategoryMakesTheWholeSaveFailAndNothingIsSaved() {
        save.execute(command(JULY, "1000", null, false, false, new SaveBudgetCommand.CategoryLimit(food, "200")));
        var before = repository.snapshot();

        assertCode(() -> save.execute(command(JULY, "5000", "900", false, true,
            new SaveBudgetCommand.CategoryLimit(food, "999"), new SaveBudgetCommand.CategoryLimit(salary, "50"))),
            ErrorCode.CATEGORY_TYPE_MISMATCH);
        assertCode(() -> save.execute(command(JULY, "5000", null, false, false,
            new SaveBudgetCommand.CategoryLimit(UUID.randomUUID(), "50"))), ErrorCode.RESOURCE_NOT_FOUND);
        assertCode(() -> save.execute(command(JULY, "5000", null, false, false,
            new SaveBudgetCommand.CategoryLimit(directory.add(UUID.randomUUID(), "Alheia", CategoryType.EXPENSE), "50"))),
            ErrorCode.RESOURCE_NOT_FOUND);

        assertThat(repository.snapshot()).isEqualTo(before);
    }

    @Test
    void invalidInputIsRejectedBeforeAnythingIsSaved() {
        var before = repository.snapshot();

        for (SaveBudgetCommand bad : List.of(
            command(JULY, "-1", null, false, false),
            command(JULY, "abc", null, false, false),
            command(JULY, "1000", "100", true, true),
            command(JULY, null, null, false, false, new SaveBudgetCommand.CategoryLimit(food, "1"), new SaveBudgetCommand.CategoryLimit(food, "2")),
            command(JULY, "99999999999", null, false, false))) {
            assertThatThrownBy(() -> save.execute(bad)).isInstanceOf(FieldValidationException.class);
        }
        assertThat(repository.snapshot()).isEqualTo(before);
    }

    @Test
    void progressListsGlobalAndEveryExpenseCategoryWithExactSpentAndSkipsIncomeOnes() {
        save.execute(command(JULY, "1000", null, false, false, new SaveBudgetCommand.CategoryLimit(food, "200")));
        expenses.byCategory.put(food, new BigDecimal("50.00"));
        expenses.byCategory.put(leisure, new BigDecimal("30.00"));
        expenses.total = new BigDecimal("80.00");

        var result = progress.execute(Currency.BRL, JULY);

        assertThat(result.global().limit()).isEqualByComparingTo("1000.00");
        assertThat(result.global().spent()).isEqualByComparingTo("80.00");
        assertThat(result.global().status()).isEqualTo(BudgetStatus.OK);
        assertThat(result.categories()).extracting(p -> p.categoryId()).containsExactly(food, leisure);
        var foodProgress = result.categories().get(0);
        assertThat(foodProgress.fraction()).isEqualByComparingTo("0.2500");
        assertThat(foodProgress.remaining()).isEqualByComparingTo("150.00");
        var leisureProgress = result.categories().get(1);
        assertThat(leisureProgress.limit()).isNull();
        assertThat(leisureProgress.status()).isEqualTo(BudgetStatus.NONE);
        assertThat(leisureProgress.spent()).isEqualByComparingTo("30.00");
    }

    @Test
    void anotherUsersLimitsAreInvisible() {
        var other = new GetBudgetUseCase(repository, () -> UUID.randomUUID());
        save.execute(command(JULY, "1000", null, false, false));

        assertThat(other.execute(Currency.BRL, JULY).monthlyLimit()).isNull();
    }

    @Test
    void currenciesAreIndependent() {
        save.execute(command(JULY, "1000", null, false, false));
        save.execute(new SaveBudgetCommand(Currency.EUR, JULY, "50", null, false, false, List.of()));

        assertThat(get.execute(Currency.BRL, JULY).monthlyLimit()).isEqualByComparingTo("1000.00");
        assertThat(get.execute(Currency.EUR, JULY).monthlyLimit()).isEqualByComparingTo("50.00");
    }

    // --- fakes ---

    private static final class Expenses implements ExpenseQuery {
        final Map<UUID, BigDecimal> byCategory = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO.setScale(2);

        @Override
        public BigDecimal expenseTotal(UUID ownerId, Currency currency, YearMonth month, UUID categoryId) {
            return categoryId == null ? total : byCategory.getOrDefault(categoryId, BigDecimal.ZERO.setScale(2));
        }

        @Override
        public Map<UUID, BigDecimal> expenseTotalsByCategory(UUID ownerId, Currency currency, YearMonth month) {
            return byCategory;
        }

        @Override
        public Map<UUID, BigDecimal> expenseTotalsByCategory(UUID ownerId, Currency currency, java.time.LocalDate from,
                                                             java.time.LocalDate to) {
            return byCategory;
        }
    }

    private static final class Directory implements CategoryDirectory {
        final Map<UUID, CategoryView> byId = new HashMap<>();
        final Map<UUID, UUID> ownerOf = new HashMap<>();

        UUID add(UUID owner, String name, CategoryType type) {
            var id = UUID.randomUUID();
            byId.put(id, new CategoryView(id, name, "icon", "#112233", type, List.of()));
            ownerOf.put(id, owner);
            return id;
        }

        @Override
        public Optional<CategoryView> find(UUID ownerId, UUID categoryId) {
            return ownerId.equals(ownerOf.get(categoryId)) ? Optional.of(byId.get(categoryId)) : Optional.empty();
        }

        @Override
        public Map<UUID, CategoryView> findAll(UUID ownerId, Collection<UUID> ids) {
            return ids.stream().filter(id -> ownerId.equals(ownerOf.get(id))).collect(Collectors.toMap(id -> id, byId::get));
        }

        @Override
        public List<CategoryView> listAccepting(UUID ownerId, CategoryType kind) {
            return byId.values().stream().filter(c -> ownerId.equals(ownerOf.get(c.id())) && c.type().accepts(kind)).toList();
        }
    }

    private static final class InMemoryBudgets implements BudgetRepository {
        record Key(UUID owner, Currency currency) {
        }

        record MonthKey(UUID owner, Currency currency, YearMonth month) {
        }

        final Map<Key, BigDecimal> monthly = new HashMap<>();
        final Map<MonthKey, MonthOverride> overrides = new HashMap<>();
        final Map<Key, Map<UUID, BigDecimal>> limits = new HashMap<>();

        Object snapshot() {
            return List.of(Map.copyOf(monthly), Map.copyOf(overrides), Map.copyOf(limits));
        }

        @Override
        public BudgetConfig load(UUID ownerId, Currency currency, YearMonth month) {
            var key = new Key(ownerId, currency);
            return new BudgetConfig(currency, monthly.get(key), overrides.get(new MonthKey(ownerId, currency, month)),
                limits.getOrDefault(key, Map.of()));
        }

        @Override
        public void save(UUID ownerId, Currency currency, YearMonth month, BigDecimal monthlyLimit, MonthOverride override,
                         Map<UUID, BigDecimal> categoryLimits) {
            var key = new Key(ownerId, currency);
            if (monthlyLimit == null) {
                monthly.remove(key);
            } else {
                monthly.put(key, monthlyLimit);
            }
            var monthKey = new MonthKey(ownerId, currency, month);
            if (override == null) {
                overrides.remove(monthKey);
            } else {
                overrides.put(monthKey, override);
            }
            limits.put(key, Map.copyOf(categoryLimits));
        }
    }
}
