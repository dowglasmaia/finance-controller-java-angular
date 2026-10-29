package com.dowglasmaia.financecontroller.category.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dowglasmaia.financecontroller.category.CategoryType;
import com.dowglasmaia.financecontroller.category.CategoryUsageGuard;
import com.dowglasmaia.financecontroller.category.application.port.out.CategoryRepository;
import com.dowglasmaia.financecontroller.category.domain.Category;
import com.dowglasmaia.financecontroller.identity.CurrentOwner;
import com.dowglasmaia.financecontroller.shared.error.DomainException;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CategoryUseCaseTests {

    private final UUID ownerId = UUID.randomUUID();
    private final InMemoryCategories repository = new InMemoryCategories();
    private final CurrentOwner owner = () -> ownerId;
    private final StubGuard guard = new StubGuard();

    private final CreateCategoryUseCase create = new CreateCategoryUseCase(repository, owner);
    private final ListCategoriesUseCase list = new ListCategoriesUseCase(repository, owner);
    private final GetCategoryUseCase get = new GetCategoryUseCase(repository, owner);
    private final UpdateCategoryUseCase update = new UpdateCategoryUseCase(repository, guard, owner);
    private final DeleteCategoryUseCase delete = new DeleteCategoryUseCase(repository, guard, owner);
    private final AddSubcategoryUseCase addSubcategory = new AddSubcategoryUseCase(repository, owner);
    private final DeleteSubcategoryUseCase deleteSubcategory = new DeleteSubcategoryUseCase(repository, owner);

    private static CategoryCommand command(String name, CategoryType type) {
        return new CategoryCommand(name, type, "icon", "#112233");
    }

    private static void assertCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(DomainException.class, e -> assertThat(e.code()).isEqualTo(expected));
    }

    @Test
    void createRejectsDuplicateNameInAnyCase() {
        create.execute(command("Alimentação", CategoryType.EXPENSE), List.of());

        assertCode(() -> create.execute(command("  alimentação ", CategoryType.EXPENSE), List.of()), ErrorCode.CATEGORY_NAME_DUPLICATED);
        assertThat(repository.all).hasSize(1);
    }

    @Test
    void sameNameIsAllowedForAnotherOwner() {
        create.execute(command("Lazer", CategoryType.EXPENSE), List.of());
        var other = new CreateCategoryUseCase(repository, () -> UUID.randomUUID());

        assertThat(other.execute(command("Lazer", CategoryType.EXPENSE), List.of()).name()).isEqualTo("Lazer");
        assertThat(repository.all).hasSize(2);
    }

    @Test
    void allowsMoreThanFiftyCategories() {
        for (int i = 1; i <= 51; i++) {
            create.execute(command("Categoria " + i, CategoryType.EXPENSE), List.of());
        }
        assertThat(list.execute(null)).hasSize(51);
    }

    @Test
    void listFiltersByAcceptedTypeIncludingBothAndSortsIgnoringCaseAndAccents() {
        create.execute(command("zebra", CategoryType.EXPENSE), List.of());
        create.execute(command("Água", CategoryType.BOTH), List.of());
        create.execute(command("Bônus", CategoryType.INCOME), List.of());
        create.execute(command("abacaxi", CategoryType.EXPENSE), List.of());

        assertThat(list.execute(CategoryType.EXPENSE)).extracting(Category::name).containsExactly("abacaxi", "Água", "zebra");
        assertThat(list.execute(CategoryType.INCOME)).extracting(Category::name).containsExactly("Água", "Bônus");
        assertThat(list.execute(null)).hasSize(4);
    }

    @Test
    void getAndMutationsOfAnotherOwnersCategoryLookLikeNotFound() {
        var foreign = new CreateCategoryUseCase(repository, () -> UUID.randomUUID())
            .execute(command("Alheia", CategoryType.EXPENSE), List.of());

        assertCode(() -> get.execute(foreign.id()), ErrorCode.RESOURCE_NOT_FOUND);
        assertCode(() -> update.execute(foreign.id(), command("X", CategoryType.EXPENSE)), ErrorCode.RESOURCE_NOT_FOUND);
        assertCode(() -> delete.execute(foreign.id()), ErrorCode.RESOURCE_NOT_FOUND);
        assertCode(() -> addSubcategory.execute(foreign.id(), "Nova"), ErrorCode.RESOURCE_NOT_FOUND);
        assertThat(repository.all).hasSize(1);
    }

    @Test
    void updateKeepsOwnNameButRejectsAnotherCategorysName() {
        var first = create.execute(command("Casa", CategoryType.EXPENSE), List.of());
        create.execute(command("Carro", CategoryType.EXPENSE), List.of());

        assertThat(update.execute(first.id(), command("CASA", CategoryType.EXPENSE)).name()).isEqualTo("CASA");
        assertCode(() -> update.execute(first.id(), command(" carro", CategoryType.EXPENSE)), ErrorCode.CATEGORY_NAME_DUPLICATED);
    }

    @Test
    void typeChangeIsRefusedWhenTransactionsAreIncompatible() {
        var created = create.execute(command("Casa", CategoryType.EXPENSE), List.of());
        guard.incompatible = true;

        assertCode(() -> update.execute(created.id(), command("Casa", CategoryType.INCOME)), ErrorCode.CATEGORY_TYPE_CONFLICT);
        assertThat(get.execute(created.id()).type()).isEqualTo(CategoryType.EXPENSE);
        // sem mudança de tipo, a porta nem é consultada
        assertThat(update.execute(created.id(), command("Casa 2", CategoryType.EXPENSE)).name()).isEqualTo("Casa 2");
    }

    @Test
    void deleteIsBlockedWhenInUseAndRemovesEverythingOtherwise() {
        var created = create.execute(command("Casa", CategoryType.EXPENSE), List.of("Aluguel"));
        guard.inUse = true;

        assertCode(() -> delete.execute(created.id()), ErrorCode.CATEGORY_IN_USE);
        assertThat(repository.all).hasSize(1);

        guard.inUse = false;
        delete.execute(created.id());
        assertThat(repository.all).isEmpty();
    }

    @Test
    void subcategoryLifecycle() {
        var created = create.execute(command("Casa", CategoryType.EXPENSE), List.of());
        var rent = addSubcategory.execute(created.id(), " Aluguel ");

        assertThat(rent.name()).isEqualTo("Aluguel");
        assertCode(() -> addSubcategory.execute(created.id(), "aluguel"), ErrorCode.SUBCATEGORY_NAME_DUPLICATED);

        var others = get.execute(created.id()).subcategories().stream().filter(s -> s.isProtected()).findFirst().orElseThrow();
        assertCode(() -> deleteSubcategory.execute(created.id(), others.id()), ErrorCode.SUBCATEGORY_PROTECTED);
        assertCode(() -> deleteSubcategory.execute(created.id(), UUID.randomUUID()), ErrorCode.RESOURCE_NOT_FOUND);

        deleteSubcategory.execute(created.id(), rent.id());
        assertThat(get.execute(created.id()).subcategories()).extracting(s -> s.name()).containsExactly("Outros");
    }

    @Test
    void seedCreatesTheElevenDefaultsAndIsIdempotent() {
        var seed = new SeedDefaultCategoriesUseCase(repository);
        seed.execute(ownerId);
        seed.execute(ownerId);

        var all = list.execute(null);
        assertThat(all).hasSize(11);
        assertThat(all.stream().filter(c -> c.type() == CategoryType.EXPENSE)).hasSize(7);
        assertThat(all.stream().filter(c -> c.type() == CategoryType.INCOME)).hasSize(4);
        assertThat(all).allSatisfy(c -> assertThat(c.subcategories()).extracting(s -> s.name()).contains("Outros"));
        var home = all.stream().filter(c -> c.name().equals("Moradia")).findFirst().orElseThrow();
        assertThat(home.subcategories()).extracting(s -> s.name())
            .containsExactlyInAnyOrder("Outros", "Aluguel", "Condomínio", "Energia", "Água", "Internet");
    }

    private static final class StubGuard implements CategoryUsageGuard {
        boolean inUse;
        boolean incompatible;

        @Override
        public boolean isInUse(UUID ownerId, UUID categoryId) {
            return inUse;
        }

        @Override
        public boolean hasTransactionsIncompatibleWith(UUID ownerId, UUID categoryId, CategoryType newType) {
            return incompatible;
        }
    }

    private static final class InMemoryCategories implements CategoryRepository {
        final List<Category> all = new ArrayList<>();

        @Override
        public List<Category> findAll(UUID ownerId, CategoryType acceptedKind) {
            return all.stream()
                .filter(c -> c.ownerId().equals(ownerId))
                .filter(c -> acceptedKind == null || c.type().accepts(acceptedKind))
                .toList();
        }

        @Override
        public Optional<Category> findById(UUID ownerId, UUID categoryId) {
            return all.stream().filter(c -> c.ownerId().equals(ownerId) && c.id().equals(categoryId)).findFirst();
        }

        @Override
        public List<Category> findAllByIds(UUID ownerId, java.util.Collection<UUID> ids) {
            return all.stream().filter(c -> c.ownerId().equals(ownerId) && ids.contains(c.id())).toList();
        }

        @Override
        public boolean existsByName(UUID ownerId, String name, UUID excludingId) {
            return all.stream().anyMatch(c -> c.ownerId().equals(ownerId)
                && Category.key(c.name()).equals(Category.key(name)) && !c.id().equals(excludingId));
        }

        @Override
        public Category save(Category category) {
            all.removeIf(c -> c.id().equals(category.id()));
            all.add(category);
            return category;
        }

        @Override
        public void delete(UUID ownerId, UUID categoryId) {
            all.removeIf(c -> c.ownerId().equals(ownerId) && c.id().equals(categoryId));
        }
    }
}
