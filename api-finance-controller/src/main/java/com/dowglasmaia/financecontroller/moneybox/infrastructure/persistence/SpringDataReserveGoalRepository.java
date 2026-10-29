package com.dowglasmaia.financecontroller.moneybox.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/** Toda consulta recebe proprietário e moeda (P-01, RN-CUR-02). */
interface SpringDataReserveGoalRepository extends JpaRepository<MoneyBoxEntities.Goal, UUID> {

    @Query("select g from ReserveGoal g where g.ownerId = :ownerId and g.currency = :currency")
    Optional<MoneyBoxEntities.Goal> find(@Param("ownerId") UUID ownerId, @Param("currency") String currency);
}
