package com.dowglasmaia.financecontroller.budget.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/** Toda consulta recebe proprietário e moeda (P-01, RN-CUR-02). */
interface SpringDataBudgetCategoryLimitRepository extends JpaRepository<BudgetEntities.CategoryLimit, UUID> {
    @Query("select c from BudgetCategoryLimit c where c.ownerId = :ownerId and c.currency = :currency")
    List<BudgetEntities.CategoryLimit> findAll(@Param("ownerId") UUID ownerId, @Param("currency") String currency);

    /** Executa na hora (antes das inserções do mesmo conjunto), sem depender da ordem de flush do Hibernate. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from BudgetCategoryLimit c where c.ownerId = :ownerId and c.currency = :currency")
    int deleteAll(@Param("ownerId") UUID ownerId, @Param("currency") String currency);
}
