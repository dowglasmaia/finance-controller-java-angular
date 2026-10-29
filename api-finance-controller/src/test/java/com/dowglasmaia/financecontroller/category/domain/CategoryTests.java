package com.dowglasmaia.financecontroller.category.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dowglasmaia.financecontroller.category.CategoryType;
import com.dowglasmaia.financecontroller.shared.error.DomainException;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CategoryTests {
    private static final UUID OWNER = UUID.randomUUID();

    private static Category category(List<String> subs) {
        return Category.create(OWNER, " Alimentação ", CategoryType.EXPENSE, "restaurant", "#4CAF50", subs);
    }

    private static void assertCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(DomainException.class, e -> assertThat(e.code()).isEqualTo(expected));
    }

    @Test
    void createAlwaysHasExactlyOneProtectedOthers() {
        var created = category(List.of("Mercado", "outros", " OUTROS ", "Mercado"));

        assertThat(created.name()).isEqualTo("Alimentação");
        assertThat(created.subcategories()).extracting(Subcategory::name).containsExactly("Outros", "Mercado");
        assertThat(created.subcategories().stream().filter(Subcategory::isProtected)).hasSize(1);
    }

    @Test
    void createWithoutSubcategoriesStillHasOthers() {
        assertThat(category(null).subcategories()).extracting(Subcategory::name).containsExactly("Outros");
    }

    @Test
    void rejectsInvalidFields() {
        assertCode(() -> Category.create(OWNER, " ", CategoryType.EXPENSE, "i", "#4CAF50", null), ErrorCode.VALIDATION_FAILED);
        assertCode(() -> Category.create(OWNER, "x".repeat(61), CategoryType.EXPENSE, "i", "#4CAF50", null), ErrorCode.VALIDATION_FAILED);
        assertCode(() -> Category.create(OWNER, "A", CategoryType.EXPENSE, "i", "green", null), ErrorCode.VALIDATION_FAILED);
        assertCode(() -> Category.create(OWNER, "A", CategoryType.EXPENSE, " ", "#4CAF50", null), ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void addSubcategoryRejectsDuplicateIgnoringCaseAndSpaces() {
        var created = category(List.of("Mercado"));

        assertCode(() -> created.addSubcategory("  MERCADO "), ErrorCode.SUBCATEGORY_NAME_DUPLICATED);
        assertCode(() -> created.addSubcategory("outros"), ErrorCode.SUBCATEGORY_NAME_DUPLICATED);
        assertThat(created.addSubcategory("Feira").subcategories()).hasSize(3);
    }

    @Test
    void removeSubcategoryProtectsOthersAndRejectsForeignIds() {
        var created = category(List.of("Mercado"));
        var others = created.subcategories().stream().filter(Subcategory::isProtected).findFirst().orElseThrow();
        var market = created.subcategories().stream().filter(s -> !s.isProtected()).findFirst().orElseThrow();

        assertCode(() -> created.removeSubcategory(others.id()), ErrorCode.SUBCATEGORY_PROTECTED);
        assertCode(() -> created.removeSubcategory(UUID.randomUUID()), ErrorCode.RESOURCE_NOT_FOUND);
        assertThat(created.removeSubcategory(market.id()).subcategories()).containsExactly(others);
    }

    @Test
    void typeAcceptance() {
        assertThat(CategoryType.BOTH.accepts(CategoryType.EXPENSE)).isTrue();
        assertThat(CategoryType.BOTH.accepts(CategoryType.INCOME)).isTrue();
        assertThat(CategoryType.EXPENSE.accepts(CategoryType.INCOME)).isFalse();
        assertThat(CategoryType.INCOME.accepts(CategoryType.INCOME)).isTrue();
    }
}
