package com.dowglasmaia.financecontroller.category.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "category")
class CategoryEntity {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    /** Coluna gerada pelo banco (lower + btrim); somente leitura. */
    @Column(name = "name_key", insertable = false, updatable = false)
    private String nameKey;

    @Column(name = "type", nullable = false, length = 10)
    private String type;

    @Column(name = "icon", nullable = false, length = 40)
    private String icon;

    @Column(name = "color", nullable = false, length = 7)
    private String color;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubcategoryEntity> subcategories = new ArrayList<>();

    protected CategoryEntity() {
    }

    CategoryEntity(UUID id, UUID ownerId) {
        this.id = id;
        this.ownerId = ownerId;
    }

    UUID getId() {
        return id;
    }

    UUID getOwnerId() {
        return ownerId;
    }

    String getName() {
        return name;
    }

    String getType() {
        return type;
    }

    String getIcon() {
        return icon;
    }

    String getColor() {
        return color;
    }

    List<SubcategoryEntity> getSubcategories() {
        return subcategories;
    }

    void update(String name, String type, String icon, String color) {
        this.name = name;
        this.type = type;
        this.icon = icon;
        this.color = color;
    }
}
