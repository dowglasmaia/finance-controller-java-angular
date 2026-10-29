package com.dowglasmaia.financecontroller.category.domain;

import java.util.UUID;

/** Subcategoria; a protegida ("Outros") nunca é removida isoladamente. */
public record Subcategory(UUID id, String name, boolean isProtected) {

    public Subcategory {
        name = Category.requireName(name);
    }
}
