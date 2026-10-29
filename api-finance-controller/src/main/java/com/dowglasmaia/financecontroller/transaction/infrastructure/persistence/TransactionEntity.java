package com.dowglasmaia.financecontroller.transaction.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "transaction_entry")
class TransactionEntity {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(name = "kind", nullable = false, length = 10)
    private String kind;

    @Column(name = "description", nullable = false, length = 120)
    private String description;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "subcategory_id")
    private UUID subcategoryId;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TransactionEntity() {
    }

    TransactionEntity(UUID id, UUID ownerId, Instant createdAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
    }

    UUID getId() {
        return id;
    }

    UUID getOwnerId() {
        return ownerId;
    }

    String getKind() {
        return kind;
    }

    String getDescription() {
        return description;
    }

    BigDecimal getAmount() {
        return amount;
    }

    String getCurrency() {
        return currency;
    }

    LocalDate getEntryDate() {
        return entryDate;
    }

    UUID getCategoryId() {
        return categoryId;
    }

    UUID getSubcategoryId() {
        return subcategoryId;
    }

    String getNotes() {
        return notes;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }

    void update(String kind, String description, BigDecimal amount, String currency, LocalDate entryDate,
                UUID categoryId, UUID subcategoryId, String notes, Instant updatedAt) {
        this.kind = kind;
        this.description = description;
        this.amount = amount;
        this.currency = currency;
        this.entryDate = entryDate;
        this.categoryId = categoryId;
        this.subcategoryId = subcategoryId;
        this.notes = notes;
        this.updatedAt = updatedAt;
    }
}
