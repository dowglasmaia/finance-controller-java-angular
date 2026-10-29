package com.dowglasmaia.financecontroller.category.application.port.out;

import com.dowglasmaia.financecontroller.category.CategoryType;
import com.dowglasmaia.financecontroller.category.domain.Category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de saída; todas as consultas são filtradas pelo proprietário (P-01). */
public interface CategoryRepository {

    /** Categorias do proprietário; com {@code acceptedKind}, apenas as que aceitam o tipo (inclui {@code BOTH}). Sem ordem garantida. */
    List<Category> findAll(UUID ownerId, CategoryType acceptedKind);

    Optional<Category> findById(UUID ownerId, UUID categoryId);

    /** Categorias do proprietário entre {@code ids} (ids de outro proprietário são omitidos). */
    List<Category> findAllByIds(UUID ownerId, java.util.Collection<UUID> ids);

    boolean existsByName(UUID ownerId, String name, UUID excludingId);

    /** Grava com flush imediato: violações de unicidade surgem dentro do caso de uso. */
    Category save(Category category);

    void delete(UUID ownerId, UUID categoryId);
}
