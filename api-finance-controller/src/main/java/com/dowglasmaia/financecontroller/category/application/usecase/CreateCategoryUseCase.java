package com.dowglasmaia.financecontroller.category.application.usecase;

import com.dowglasmaia.financecontroller.category.application.port.out.CategoryRepository;
import com.dowglasmaia.financecontroller.category.domain.Category;
import com.dowglasmaia.financecontroller.identity.CurrentOwner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CreateCategoryUseCase {

    private final CategoryRepository categories;
    private final CurrentOwner owner;

    CreateCategoryUseCase(CategoryRepository categories, CurrentOwner owner) {
        this.categories = categories;
        this.owner = owner;
    }

    @Transactional
    public Category execute(CategoryCommand command, List<String> subcategoryNames) {
        var ownerId = owner.id();
        var category = Category.create(ownerId, command.name(), command.type(), command.icon(), command.color(),
            subcategoryNames);
        CategoryNames.requireAvailable(categories, ownerId, category.name(), null);
        return categories.save(category);
    }
}
