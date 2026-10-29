package com.dowglasmaia.financecontroller.budget.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/** Toda consulta recebe proprietário e moeda (P-01, RN-CUR-02). */
interface SpringDataBudgetSettingRepository extends JpaRepository<BudgetEntities.Setting, UUID> {
    @Query("select s from BudgetSetting s where s.ownerId = :ownerId and s.currency = :currency")
    Optional<BudgetEntities.Setting> find(@Param("ownerId") UUID ownerId, @Param("currency") String currency);
}
