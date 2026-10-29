package com.dowglasmaia.financecontroller.moneybox.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Saldo = depósitos - saques, sempre calculado aqui; a moeda vem do cofrinho dono da movimentação. */
interface SpringDataMoneyBoxEntryRepository extends JpaRepository<MoneyBoxEntities.Entry, UUID> {

    String SIGNED = "case when e.kind = 'DEPOSIT' then e.amount else (0 - e.amount) end";

    boolean existsByOwnerIdAndMoneyBoxId(UUID ownerId, UUID moneyBoxId);

    @Query("select e from MoneyBoxEntry e where e.ownerId = :ownerId and e.moneyBoxId = :boxId "
        + "order by e.entryDate desc, e.createdAt desc, e.id desc")
    List<MoneyBoxEntities.Entry> findAllOf(@Param("ownerId") UUID ownerId, @Param("boxId") UUID boxId);

    @Query("select e from MoneyBoxEntry e where e.ownerId = :ownerId and e.moneyBoxId = :boxId and e.id = :id")
    Optional<MoneyBoxEntities.Entry> find(@Param("ownerId") UUID ownerId, @Param("boxId") UUID boxId,
                                          @Param("id") UUID id);

    @Query("select e.moneyBoxId, sum(" + SIGNED + ") from MoneyBoxEntry e where e.ownerId = :ownerId "
        + "and e.moneyBoxId in (select b.id from MoneyBox b where b.ownerId = :ownerId and b.currency = :currency) "
        + "group by e.moneyBoxId")
    List<Object[]> balances(@Param("ownerId") UUID ownerId, @Param("currency") String currency);

    @Query("select sum(" + SIGNED + ") from MoneyBoxEntry e where e.ownerId = :ownerId and e.moneyBoxId = :boxId")
    BigDecimal balance(@Param("ownerId") UUID ownerId, @Param("boxId") UUID boxId);

    @Query("select sum(" + SIGNED + ") from MoneyBoxEntry e where e.ownerId = :ownerId "
        + "and e.moneyBoxId in (select b.id from MoneyBox b where b.ownerId = :ownerId and b.currency = :currency)")
    BigDecimal total(@Param("ownerId") UUID ownerId, @Param("currency") String currency);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from MoneyBoxEntry e where e.ownerId = :ownerId and e.moneyBoxId = :boxId")
    int deleteAllOf(@Param("ownerId") UUID ownerId, @Param("boxId") UUID boxId);
}
