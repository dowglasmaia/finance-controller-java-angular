package com.dowglasmaia.financecontroller.category.application.usecase;

import com.dowglasmaia.financecontroller.category.CategoryUsageGuard;
import com.dowglasmaia.financecontroller.category.application.port.out.CategoryRepository;
import com.dowglasmaia.financecontroller.identity.CurrentOwner;
import com.dowglasmaia.financecontroller.shared.error.DomainException;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;
import com.dowglasmaia.financecontroller.shared.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DeleteCategoryUseCase {

    private final CategoryRepository categories;
    private final CategoryUsageGuard usage;
    private final CurrentOwner owner;

    DeleteCategoryUseCase(CategoryRepository categories, CategoryUsageGuard usage, CurrentOwner owner) {
        this.categories = categories;
        this.usage = usage;
        this.owner = owner;
    }

    @Transactional
    public void execute(UUID categoryId) {
        var ownerId = owner.id();
        categories.findById(ownerId, categoryId).orElseThrow(ResourceNotFoundException::new);
        if (usage.isInUse(ownerId, categoryId)) {
            throw new DomainException(ErrorCode.CATEGORY_IN_USE);
        }
        categories.delete(ownerId, categoryId);
    }
}
