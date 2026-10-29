package com.dowglasmaia.financecontroller.category.api;

import com.dowglasmaia.financecontroller.category.CategoryType;
import com.dowglasmaia.financecontroller.category.application.usecase.CategoryCommand;
import com.dowglasmaia.financecontroller.category.domain.Category;
import com.dowglasmaia.financecontroller.category.domain.Subcategory;

/** Conversão entre os modelos gerados do contrato e o domínio; nenhum tipo gerado vaza para os casos de uso. */
final class CategoryApiMapper {

    private CategoryApiMapper() {
    }

    static CategoryCommand toCommand(String name, com.dowglasmaia.financecontroller.generated.categories.model.CategoryType type,
                                     String icon, String color) {
        return new CategoryCommand(name, CategoryType.valueOf(type.getValue()), icon, color);
    }

    static com.dowglasmaia.financecontroller.generated.categories.model.Category toResponse(Category category) {
        return new com.dowglasmaia.financecontroller.generated.categories.model.Category(
            category.id(),
            category.name(),
            com.dowglasmaia.financecontroller.generated.categories.model.CategoryType.fromValue(category.type().name()),
            category.icon(),
            category.color(),
            category.subcategories().stream().map(CategoryApiMapper::toResponse).toList());
    }

    static com.dowglasmaia.financecontroller.generated.categories.model.Subcategory toResponse(Subcategory sub) {
        return new com.dowglasmaia.financecontroller.generated.categories.model.Subcategory(sub.id(), sub.name(),
            sub.isProtected());
    }
}
