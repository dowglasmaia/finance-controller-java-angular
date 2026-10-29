package com.dowglasmaia.financecontroller.budget.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/** Toda consulta recebe proprietário e moeda (P-01, RN-CUR-02). */
interface SpringDataBudgetOverrideRepository extends JpaRepository<BudgetEntities.MonthOverride, UUID> {
    @Query("select o from BudgetMonthOverride o where o.ownerId = :ownerId and o.currency = :currency "
        + "and o.referenceMonth = :month")
    Optional<BudgetEntities.MonthOverride> find(@Param("ownerId") UUID ownerId, @Param("currency") String currency,
                                                @Param("month") String month);
}
