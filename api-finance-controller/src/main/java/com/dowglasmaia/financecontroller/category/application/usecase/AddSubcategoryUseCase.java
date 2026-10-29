package com.dowglasmaia.financecontroller.category.application.usecase;

import com.dowglasmaia.financecontroller.category.application.port.out.CategoryRepository;
import com.dowglasmaia.financecontroller.category.domain.Category;
import com.dowglasmaia.financecontroller.category.domain.Subcategory;
import com.dowglasmaia.financecontroller.identity.CurrentOwner;
import com.dowglasmaia.financecontroller.shared.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AddSubcategoryUseCase {

    private final CategoryRepository categories;
    private final CurrentOwner owner;

    AddSubcategoryUseCase(CategoryRepository categories, CurrentOwner owner) {
        this.categories = categories;
        this.owner = owner;
    }

    @Transactional
    public Subcategory execute(UUID categoryId, String name) {
        var current = categories.findById(owner.id(), categoryId).orElseThrow(ResourceNotFoundException::new);
        Category saved = categories.save(current.addSubcategory(name));
        var key = Category.key(name);
        return saved.subcategories().stream().filter(s -> Category.key(s.name()).equals(key)).findFirst().orElseThrow();
    }
}
