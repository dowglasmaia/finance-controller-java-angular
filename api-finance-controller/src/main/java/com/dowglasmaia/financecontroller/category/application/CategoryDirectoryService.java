package com.dowglasmaia.financecontroller.category.application;

import com.dowglasmaia.financecontroller.category.CategoryDirectory;
import com.dowglasmaia.financecontroller.category.application.port.out.CategoryRepository;
import com.dowglasmaia.financecontroller.category.domain.Category;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Implementa a API pública de leitura sobre a porta de saída; sem regra própria. */
@Service
class CategoryDirectoryService implements CategoryDirectory {

    private final CategoryRepository categories;

    CategoryDirectoryService(CategoryRepository categories) {
        this.categories = categories;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CategoryView> find(UUID ownerId, UUID categoryId) {
        return categories.findById(ownerId, categoryId).map(CategoryDirectoryService::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, CategoryView> findAll(UUID ownerId, Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return categories.findAllByIds(ownerId, ids).stream()
            .map(CategoryDirectoryService::toView)
            .collect(Collectors.toMap(CategoryView::id, Function.identity()));
    }

    private static CategoryView toView(Category c) {
        return new CategoryView(c.id(), c.name(), c.icon(), c.color(), c.type(),
            c.subcategories().stream().map(s -> new SubcategoryView(s.id(), s.name())).toList());
    }
}
