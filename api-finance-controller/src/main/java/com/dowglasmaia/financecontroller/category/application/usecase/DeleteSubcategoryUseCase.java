package com.dowglasmaia.financecontroller.category.application.usecase;

import com.dowglasmaia.financecontroller.category.application.port.out.CategoryRepository;
import com.dowglasmaia.financecontroller.identity.CurrentOwner;
import com.dowglasmaia.financecontroller.shared.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Lançamentos vinculados ficam sem subcategoria (P-11); essa desassociação entra com o módulo de lançamentos (F5). */
@Service
public class DeleteSubcategoryUseCase {

    private final CategoryRepository categories;
    private final CurrentOwner owner;

    DeleteSubcategoryUseCase(CategoryRepository categories, CurrentOwner owner) {
        this.categories = categories;
        this.owner = owner;
    }

    @Transactional
    public void execute(UUID categoryId, UUID subcategoryId) {
        var current = categories.findById(owner.id(), categoryId).orElseThrow(ResourceNotFoundException::new);
        categories.save(current.removeSubcategory(subcategoryId));
    }
}
