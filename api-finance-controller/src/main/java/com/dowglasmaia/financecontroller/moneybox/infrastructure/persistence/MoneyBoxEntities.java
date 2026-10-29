package com.dowglasmaia.financecontroller.moneybox.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Entidades JPA das três tabelas de cofrinhos; nenhuma sai deste pacote. */
final class MoneyBoxEntities {

    private MoneyBoxEntities() {
    }

    @Entity(name = "MoneyBox")
    @Table(name = "money_box")
    static class Box {
        @Id
        private UUID id;
        @Column(name = "owner_id", nullable = false, updatable = false)
        private UUID ownerId;
        @Column(name = "name", nullable = false, length = 100)
        private String name;
        @Column(name = "description", length = 255)
        private String description;
        @Column(name = "icon", nullable = false, length = 40)
        private String icon;
        @Column(name = "color", nullable = false, length = 7)
        private String color;
        @Column(name = "currency", nullable = false, length = 3)
        private String currency;
        @Column(name = "goal", precision = 12, scale = 2)
        private BigDecimal goal;
        @Column(name = "created_at", nullable = false, updatable = false)
        private Instant createdAt;

        protected Box() {
        }

        Box(UUID id, UUID ownerId, Instant createdAt) {
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

        String getName() {
            return name;
        }

        String getDescription() {
            return description;
        }

        String getIcon() {
            return icon;
        }

        String getColor() {
            return color;
        }

        String getCurrency() {
            return currency;
        }

        BigDecimal getGoal() {
            return goal;
        }

        Instant getCreatedAt() {
            return createdAt;
        }

        void update(String name, String description, String icon, String color, String currency, BigDecimal goal) {
            this.name = name;
            this.description = description;
            this.icon = icon;
            this.color = color;
            this.currency = currency;
            this.goal = goal;
        }
    }

    @Entity(name = "MoneyBoxEntry")
    @Table(name = "money_box_entry")
    static class Entry {
        @Id
        private UUID id;
        @Column(name = "owner_id", nullable = false, updatable = false)
        private UUID ownerId;
        @Column(name = "money_box_id", nullable = false, updatable = false)
        private UUID moneyBoxId;
        @Column(name = "kind", nullable = false, length = 10)
        private String kind;
        @Column(name = "amount", nullable = false, precision = 12, scale = 2)
        private BigDecimal amount;
        @Column(name = "entry_date", nullable = false)
        private LocalDate entryDate;
        @Column(name = "note", length = 255)
        private String note;
        @Column(name = "created_at", nullable = false, updatable = false)
        private Instant createdAt;

        protected Entry() {
        }

        Entry(UUID id, UUID ownerId, UUID moneyBoxId, Instant createdAt) {
            this.id = id;
            this.ownerId = ownerId;
            this.moneyBoxId = moneyBoxId;
            this.createdAt = createdAt;
        }

        UUID getId() {
            return id;
        }

        UUID getOwnerId() {
            return ownerId;
        }

        UUID getMoneyBoxId() {
            return moneyBoxId;
        }

        String getKind() {
            return kind;
        }

        BigDecimal getAmount() {
            return amount;
        }

        LocalDate getEntryDate() {
            return entryDate;
        }

        String getNote() {
            return note;
        }

        Instant getCreatedAt() {
            return createdAt;
        }

        void update(String kind, BigDecimal amount, LocalDate entryDate, String note) {
            this.kind = kind;
            this.amount = amount;
            this.entryDate = entryDate;
            this.note = note;
        }
    }

    @Entity(name = "ReserveGoal")
    @Table(name = "reserve_goal")
    static class Goal {
        @Id
        private UUID id;
        @Column(name = "owner_id", nullable = false, updatable = false)
        private UUID ownerId;
        @Column(name = "currency", nullable = false, updatable = false, length = 3)
        private String currency;
        @Column(name = "goal_amount", nullable = false, precision = 12, scale = 2)
        private BigDecimal goalAmount;

        protected Goal() {
        }

        Goal(UUID ownerId, String currency, BigDecimal goalAmount) {
            this.id = UUID.randomUUID();
            this.ownerId = ownerId;
            this.currency = currency;
            this.goalAmount = goalAmount;
        }

        BigDecimal getGoalAmount() {
            return goalAmount;
        }

        void setGoalAmount(BigDecimal goalAmount) {
            this.goalAmount = goalAmount;
        }
    }
}
