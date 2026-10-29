package com.dowglasmaia.financecontroller.category.infrastructure.persistence;

import com.dowglasmaia.financecontroller.category.CategoryType;
import com.dowglasmaia.financecontroller.category.application.port.out.CategoryRepository;
import com.dowglasmaia.financecontroller.category.domain.Category;
import com.dowglasmaia.financecontroller.category.domain.Subcategory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
class JpaCategoryRepositoryAdapter implements CategoryRepository {

    private final SpringDataCategoryRepository categories;

    JpaCategoryRepositoryAdapter(SpringDataCategoryRepository categories) {
        this.categories = categories;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> findAll(UUID ownerId, CategoryType acceptedKind) {
        var types = acceptedKind == null || acceptedKind == CategoryType.BOTH
            ? List.of("INCOME", "EXPENSE", "BOTH")
            : List.of(acceptedKind.name(), CategoryType.BOTH.name());
        return categories.findAllByOwnerAndTypes(ownerId, types).stream().map(JpaCategoryRepositoryAdapter::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Category> findById(UUID ownerId, UUID categoryId) {
        return categories.findByOwnerAndId(ownerId, categoryId).map(JpaCategoryRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> findAllByIds(UUID ownerId, java.util.Collection<UUID> ids) {
        return categories.findAllByOwnerAndIds(ownerId, ids).stream().map(JpaCategoryRepositoryAdapter::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(UUID ownerId, String name, UUID excludingId) {
        var key = Category.key(name);
        return excludingId == null
            ? categories.existsByOwnerAndNameKey(ownerId, key)
            : categories.existsByOwnerAndNameKeyExcluding(ownerId, key, excludingId);
    }

    /** Flush imediato: a violação de unicidade surge dentro do caso de uso, não no commit. */
    @Override
    @Transactional
    public Category save(Category category) {
        var entity = categories.findByOwnerAndId(category.ownerId(), category.id())
            .orElseGet(() -> new CategoryEntity(category.id(), category.ownerId()));
        entity.update(category.name(), category.type().name(), category.icon(), category.color());
        Set<UUID> keep = category.subcategories().stream().map(Subcategory::id).collect(Collectors.toSet());
        entity.getSubcategories().removeIf(existing -> !keep.contains(existing.getId()));
        Set<UUID> present = entity.getSubcategories().stream().map(SubcategoryEntity::getId).collect(Collectors.toSet());
        for (Subcategory sub : category.subcategories()) {
            if (!present.contains(sub.id())) {
                entity.getSubcategories().add(new SubcategoryEntity(sub.id(), entity, sub.name(), sub.isProtected()));
            }
        }
        return toDomain(categories.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public void delete(UUID ownerId, UUID categoryId) {
        categories.findByOwnerAndId(ownerId, categoryId).ifPresent(categories::delete);
        categories.flush();
    }

    private static Category toDomain(CategoryEntity e) {
        var subs = e.getSubcategories().stream()
            .map(s -> new Subcategory(s.getId(), s.getName(), s.isProtected()))
            .toList();
        return new Category(e.getId(), e.getOwnerId(), e.getName(), CategoryType.valueOf(e.getType()), e.getIcon(),
            e.getColor().strip(), subs);
    }
}
