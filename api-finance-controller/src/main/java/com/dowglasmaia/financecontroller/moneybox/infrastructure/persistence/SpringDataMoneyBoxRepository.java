package com.dowglasmaia.financecontroller.moneybox.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Toda consulta recebe o proprietário (P-01); as de lista recebem também a moeda (RN-CUR-02). */
interface SpringDataMoneyBoxRepository extends JpaRepository<MoneyBoxEntities.Box, UUID> {

    @Query("select b from MoneyBox b where b.ownerId = :ownerId and b.currency = :currency "
        + "order by b.createdAt asc, b.id asc")
    List<MoneyBoxEntities.Box> findAll(@Param("ownerId") UUID ownerId, @Param("currency") String currency);

    @Query("select b from MoneyBox b where b.ownerId = :ownerId and b.id = :id")
    Optional<MoneyBoxEntities.Box> find(@Param("ownerId") UUID ownerId, @Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from MoneyBox b where b.ownerId = :ownerId and b.id = :id")
    Optional<MoneyBoxEntities.Box> findForUpdate(@Param("ownerId") UUID ownerId, @Param("id") UUID id);

    @Query("select count(b) from MoneyBox b where b.ownerId = :ownerId and b.currency = :currency")
    long count(@Param("ownerId") UUID ownerId, @Param("currency") String currency);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from MoneyBox b where b.ownerId = :ownerId and b.id = :id")
    int deleteBox(@Param("ownerId") UUID ownerId, @Param("id") UUID id);
}
