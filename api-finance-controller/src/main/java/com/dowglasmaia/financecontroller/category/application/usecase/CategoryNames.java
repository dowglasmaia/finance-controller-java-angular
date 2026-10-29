package com.dowglasmaia.financecontroller.category.application.usecase;

import com.dowglasmaia.financecontroller.category.application.port.out.CategoryRepository;
import com.dowglasmaia.financecontroller.shared.error.DomainException;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;

import java.util.UUID;

/** Regra de unicidade de nome por proprietário, compartilhada por criação e atualização. */
final class CategoryNames {

    private CategoryNames() {
    }

    static void requireAvailable(CategoryRepository categories, UUID ownerId, String name, UUID excludingId) {
        if (categories.existsByName(ownerId, name, excludingId)) {
            throw new DomainException(ErrorCode.CATEGORY_NAME_DUPLICATED);
        }
    }
}
