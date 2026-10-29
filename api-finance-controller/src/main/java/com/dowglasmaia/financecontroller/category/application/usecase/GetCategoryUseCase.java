package com.dowglasmaia.financecontroller.category.application.usecase;

import com.dowglasmaia.financecontroller.category.application.port.out.CategoryRepository;
import com.dowglasmaia.financecontroller.category.domain.Category;
import com.dowglasmaia.financecontroller.identity.CurrentOwner;
import com.dowglasmaia.financecontroller.shared.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetCategoryUseCase {

    private final CategoryRepository categories;
    private final CurrentOwner owner;

    GetCategoryUseCase(CategoryRepository categories, CurrentOwner owner) {
        this.categories = categories;
        this.owner = owner;
    }

    @Transactional(readOnly = true)
    public Category execute(UUID categoryId) {
        return categories.findById(owner.id(), categoryId).orElseThrow(ResourceNotFoundException::new);
    }
}
