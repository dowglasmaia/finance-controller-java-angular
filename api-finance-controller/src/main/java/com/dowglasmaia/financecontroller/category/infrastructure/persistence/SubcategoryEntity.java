package com.dowglasmaia.financecontroller.category.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "subcategory")
class SubcategoryEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false, updatable = false)
    private CategoryEntity category;

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    /** Coluna gerada pelo banco (lower + btrim); somente leitura. */
    @Column(name = "name_key", insertable = false, updatable = false)
    private String nameKey;

    @Column(name = "is_protected", nullable = false)
    private boolean isProtected;

    protected SubcategoryEntity() {
    }

    SubcategoryEntity(UUID id, CategoryEntity category, String name, boolean isProtected) {
        this.id = id;
        this.category = category;
        this.name = name;
        this.isProtected = isProtected;
    }

    UUID getId() {
        return id;
    }

    String getName() {
        return name;
    }

    boolean isProtected() {
        return isProtected;
    }
}
