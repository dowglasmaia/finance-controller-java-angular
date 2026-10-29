package com.dowglasmaia.financecontroller.transaction.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Toda consulta recebe o proprietário (P-01); as de listagem e soma recebem também a moeda (RN-CUR-02). A ordem é a
 * de RN-TXN-08 (data, criação e id decrescentes) e acompanha o índice {@code ix_transaction_entry_list}.
 */
interface SpringDataTransactionRepository extends JpaRepository<TransactionEntity, UUID> {

    @Query(value = "select t from TransactionEntity t where t.ownerId = :ownerId and t.currency = :currency "
        + "and t.entryDate >= :from and t.entryDate <= :to "
        + "order by t.entryDate desc, t.createdAt desc, t.id desc",
        countQuery = "select count(t) from TransactionEntity t where t.ownerId = :ownerId and t.currency = :currency "
            + "and t.entryDate >= :from and t.entryDate <= :to")
    Page<TransactionEntity> findPage(@Param("ownerId") UUID ownerId, @Param("currency") String currency,
                                     @Param("from") LocalDate from, @Param("to") LocalDate to, Pageable pageable);

    @Query(value = "select t from TransactionEntity t where t.ownerId = :ownerId and t.currency = :currency "
        + "and t.entryDate >= :from and t.entryDate <= :to and t.kind = :kind "
        + "order by t.entryDate desc, t.createdAt desc, t.id desc",
        countQuery = "select count(t) from TransactionEntity t where t.ownerId = :ownerId and t.currency = :currency "
            + "and t.entryDate >= :from and t.entryDate <= :to and t.kind = :kind")
    Page<TransactionEntity> findPageByKind(@Param("ownerId") UUID ownerId, @Param("currency") String currency,
                                           @Param("from") LocalDate from, @Param("to") LocalDate to,
                                           @Param("kind") String kind, Pageable pageable);

    /** Uma linha por direção: {@code [kind, soma]}. */
    @Query("select t.kind, sum(t.amount) from TransactionEntity t where t.ownerId = :ownerId "
        + "and t.currency = :currency and t.entryDate >= :from and t.entryDate <= :to group by t.kind")
    List<Object[]> sumByKind(@Param("ownerId") UUID ownerId, @Param("currency") String currency,
                             @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select coalesce(sum(t.amount), 0) from TransactionEntity t where t.ownerId = :ownerId "
        + "and t.currency = :currency and t.kind = 'EXPENSE' and t.entryDate >= :from and t.entryDate <= :to "
        + "and t.id <> :excludingId")
    BigDecimal sumExpenses(@Param("ownerId") UUID ownerId, @Param("currency") String currency,
                           @Param("from") LocalDate from, @Param("to") LocalDate to,
                           @Param("excludingId") UUID excludingId);

    @Query("select coalesce(sum(t.amount), 0) from TransactionEntity t where t.ownerId = :ownerId "
        + "and t.currency = :currency and t.kind = 'EXPENSE' and t.entryDate >= :from and t.entryDate <= :to "
        + "and t.categoryId = :categoryId and t.id <> :excludingId")
    BigDecimal sumExpensesOfCategory(@Param("ownerId") UUID ownerId, @Param("currency") String currency,
                                     @Param("from") LocalDate from, @Param("to") LocalDate to,
                                     @Param("categoryId") UUID categoryId, @Param("excludingId") UUID excludingId);

    /** Uma linha por dia e direção: {@code [data, kind, soma]}. */
    @Query("select t.entryDate, t.kind, sum(t.amount) from TransactionEntity t where t.ownerId = :ownerId "
        + "and t.currency = :currency and t.entryDate >= :from and t.entryDate <= :to group by t.entryDate, t.kind")
    List<Object[]> sumByDayAndKind(@Param("ownerId") UUID ownerId, @Param("currency") String currency,
                                   @Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Uma linha por categoria com despesas: {@code [categoryId, soma]}. */
    @Query("select t.categoryId, sum(t.amount) from TransactionEntity t where t.ownerId = :ownerId "
        + "and t.currency = :currency and t.kind = 'EXPENSE' and t.entryDate >= :from and t.entryDate <= :to "
        + "group by t.categoryId")
    List<Object[]> sumExpensesByCategory(@Param("ownerId") UUID ownerId, @Param("currency") String currency,
                                         @Param("from") LocalDate from, @Param("to") LocalDate to);

    Optional<TransactionEntity> findByOwnerIdAndId(UUID ownerId, UUID id);

    boolean existsByOwnerIdAndCategoryId(UUID ownerId, UUID categoryId);

    boolean existsByOwnerIdAndCategoryIdAndKind(UUID ownerId, UUID categoryId, String kind);
}
