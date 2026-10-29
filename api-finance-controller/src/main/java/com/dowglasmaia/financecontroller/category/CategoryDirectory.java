package com.dowglasmaia.financecontroller.category;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * API pública de leitura do módulo: outros módulos (lançamentos, metas, relatórios) consultam categorias por aqui,
 * sempre dentro do proprietário. Nunca devolve categorias de outro utilizador.
 */
public interface CategoryDirectory {

    Optional<CategoryView> find(UUID ownerId, UUID categoryId);

    /** Categorias existentes entre {@code ids}, por id; ids desconhecidos ou de outro utilizador são omitidos. */
    Map<UUID, CategoryView> findAll(UUID ownerId, Collection<UUID> ids);

    /** Categorias do proprietário que aceitam o tipo (inclui {@code BOTH}), sem ordem garantida. */
    List<CategoryView> listAccepting(UUID ownerId, CategoryType kind);

    /** Visão somente leitura de uma categoria. */
    record CategoryView(UUID id, String name, String icon, String color, CategoryType type,
                        List<SubcategoryView> subcategories) {
    }

    record SubcategoryView(UUID id, String name) {
    }
}
