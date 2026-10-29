package com.dowglasmaia.financecontroller.transaction.application.port.out;

import com.dowglasmaia.financecontroller.shared.money.Currency;
import com.dowglasmaia.financecontroller.transaction.domain.Totals;
import com.dowglasmaia.financecontroller.transaction.domain.Transaction;
import com.dowglasmaia.financecontroller.transaction.domain.TransactionKind;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de saída; todas as consultas são filtradas pelo proprietário (P-01) e pela moeda (RN-CUR-02). */
public interface TransactionRepository {

    /** Uma página (a partir de 1) na ordem RN-TXN-08; {@code kind} nulo traz entradas e saídas. */
    Page findPage(UUID ownerId, YearMonth month, Currency currency, TransactionKind kind, int page, int size);

    /** Entradas e saídas do mês e da moeda (conjunto inteiro, sem paginação). */
    Totals totals(UUID ownerId, YearMonth month, Currency currency);

    /**
     * Soma das despesas do mês e da moeda, opcionalmente só de uma categoria e sem contar o lançamento
     * {@code excludingId} (para calcular o "antes" de uma edição).
     */
    BigDecimal sumExpenses(UUID ownerId, YearMonth month, Currency currency, UUID categoryId, UUID excludingId);

    Optional<Transaction> findById(UUID ownerId, UUID transactionId);

    /** Grava com flush imediato. */
    Transaction save(Transaction transaction);

    /** @return se algo foi removido */
    boolean delete(UUID ownerId, UUID transactionId);

    boolean existsByCategory(UUID ownerId, UUID categoryId);

    boolean existsByCategoryAndKind(UUID ownerId, UUID categoryId, TransactionKind kind);

    record Page(List<Transaction> items, long totalItems) {
    }
}
