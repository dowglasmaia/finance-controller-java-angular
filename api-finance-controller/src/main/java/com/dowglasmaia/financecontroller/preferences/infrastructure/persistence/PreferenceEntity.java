package com.dowglasmaia.financecontroller.preferences.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity(name = "Preference")
@Table(name = "preference")
class PreferenceEntity {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "theme", nullable = false, length = 10)
    private String theme;

    protected PreferenceEntity() {
    }

    UUID getOwnerId() {
        return ownerId;
    }

    String getCurrency() {
        return currency;
    }

    String getTheme() {
        return theme;
    }
}
