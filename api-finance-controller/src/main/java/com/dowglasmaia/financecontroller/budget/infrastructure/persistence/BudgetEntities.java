package com.dowglasmaia.financecontroller.budget.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/** Entidades JPA das três tabelas de metas; nenhuma sai deste pacote. */
final class BudgetEntities {

    private BudgetEntities() {
    }

    @Entity(name = "BudgetSetting")
    @Table(name = "budget_setting")
    static class Setting {
        @Id
        private UUID id;
        @Column(name = "owner_id", nullable = false, updatable = false)
        private UUID ownerId;
        @Column(name = "currency", nullable = false, updatable = false, length = 3)
        private String currency;
        @Column(name = "monthly_limit", nullable = false, precision = 12, scale = 2)
        private BigDecimal monthlyLimit;

        protected Setting() {
        }

        Setting(UUID ownerId, String currency, BigDecimal monthlyLimit) {
            this.id = UUID.randomUUID();
            this.ownerId = ownerId;
            this.currency = currency;
            this.monthlyLimit = monthlyLimit;
        }

        BigDecimal getMonthlyLimit() {
            return monthlyLimit;
        }

        void setMonthlyLimit(BigDecimal monthlyLimit) {
            this.monthlyLimit = monthlyLimit;
        }
    }

    @Entity(name = "BudgetMonthOverride")
    @Table(name = "budget_month_override")
    static class MonthOverride {
        @Id
        private UUID id;
        @Column(name = "owner_id", nullable = false, updatable = false)
        private UUID ownerId;
        @Column(name = "currency", nullable = false, updatable = false, length = 3)
        private String currency;
        @Column(name = "reference_month", nullable = false, updatable = false, length = 7)
        private String referenceMonth;
        @Column(name = "limit_amount", precision = 12, scale = 2)
        private BigDecimal limitAmount;
        @Column(name = "disabled", nullable = false)
        private boolean disabled;

        protected MonthOverride() {
        }

        MonthOverride(UUID ownerId, String currency, String referenceMonth) {
            this.id = UUID.randomUUID();
            this.ownerId = ownerId;
            this.currency = currency;
            this.referenceMonth = referenceMonth;
        }

        BigDecimal getLimitAmount() {
            return limitAmount;
        }

        boolean isDisabled() {
            return disabled;
        }

        void set(BigDecimal limitAmount, boolean disabled) {
            this.limitAmount = limitAmount;
            this.disabled = disabled;
        }
    }

    @Entity(name = "BudgetCategoryLimit")
    @Table(name = "budget_category_limit")
    static class CategoryLimit {
        @Id
        private UUID id;
        @Column(name = "owner_id", nullable = false, updatable = false)
        private UUID ownerId;
        @Column(name = "currency", nullable = false, updatable = false, length = 3)
        private String currency;
        @Column(name = "category_id", nullable = false, updatable = false)
        private UUID categoryId;
        @Column(name = "limit_amount", nullable = false, precision = 12, scale = 2)
        private BigDecimal limitAmount;

        protected CategoryLimit() {
        }

        CategoryLimit(UUID ownerId, String currency, UUID categoryId, BigDecimal limitAmount) {
            this.id = UUID.randomUUID();
            this.ownerId = ownerId;
            this.currency = currency;
            this.categoryId = categoryId;
            this.limitAmount = limitAmount;
        }

        UUID getCategoryId() {
            return categoryId;
        }

        BigDecimal getLimitAmount() {
            return limitAmount;
        }
    }
}
