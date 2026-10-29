package com.dowglasmaia.financecontroller.category.application.usecase;

import com.dowglasmaia.financecontroller.category.CategoryUsageGuard;
import com.dowglasmaia.financecontroller.category.application.port.out.CategoryRepository;
import com.dowglasmaia.financecontroller.category.domain.Category;
import com.dowglasmaia.financecontroller.identity.CurrentOwner;
import com.dowglasmaia.financecontroller.shared.error.DomainException;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;
import com.dowglasmaia.financecontroller.shared.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UpdateCategoryUseCase {

    private final CategoryRepository categories;
    private final CategoryUsageGuard usage;
    private final CurrentOwner owner;

    UpdateCategoryUseCase(CategoryRepository categories, CategoryUsageGuard usage, CurrentOwner owner) {
        this.categories = categories;
        this.usage = usage;
        this.owner = owner;
    }

    @Transactional
    public Category execute(UUID categoryId, CategoryCommand command) {
        var ownerId = owner.id();
        var current = categories.findById(ownerId, categoryId).orElseThrow(ResourceNotFoundException::new);
        Category updated = current.withDetails(command.name(), command.type(), command.icon(), command.color());
        CategoryNames.requireAvailable(categories, ownerId, updated.name(), categoryId);
        if (updated.type() != current.type()
                && usage.hasTransactionsIncompatibleWith(ownerId, categoryId, updated.type())) {
            throw new DomainException(ErrorCode.CATEGORY_TYPE_CONFLICT);
        }
        return categories.save(updated);
    }
}
