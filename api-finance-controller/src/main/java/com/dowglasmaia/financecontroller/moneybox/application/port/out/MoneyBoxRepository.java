package com.dowglasmaia.financecontroller.moneybox.application.port.out;

import com.dowglasmaia.financecontroller.moneybox.domain.MoneyBox;
import com.dowglasmaia.financecontroller.moneybox.domain.MoneyBoxEntry;
import com.dowglasmaia.financecontroller.shared.money.Currency;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Porta de saída. Toda operação recebe o proprietário (P-01); as de lista e soma recebem a moeda (RN-CUR-02). */
public interface MoneyBoxRepository {

    List<MoneyBox> findAll(UUID ownerId, Currency currency);

    Optional<MoneyBox> find(UUID ownerId, UUID moneyBoxId);

    /** Bloqueia a linha do cofrinho até o fim da transação: gravações no mesmo cofrinho passam uma de cada vez. */
    Optional<MoneyBox> findForUpdate(UUID ownerId, UUID moneyBoxId);

    MoneyBox save(MoneyBox moneyBox);

    /** Remove o cofrinho e as suas movimentações. */
    void delete(UUID ownerId, UUID moneyBoxId);

    boolean hasEntries(UUID ownerId, UUID moneyBoxId);

    /** Saldo de cada cofrinho da moeda que tem movimentações; os demais têm saldo zero. */
    Map<UUID, BigDecimal> balances(UUID ownerId, Currency currency);

    BigDecimal balance(UUID ownerId, UUID moneyBoxId);

    BigDecimal totalBalance(UUID ownerId, Currency currency);

    long count(UUID ownerId, Currency currency);

    /** Por data decrescente, depois criação e id decrescentes. */
    List<MoneyBoxEntry> entries(UUID ownerId, UUID moneyBoxId);

    Optional<MoneyBoxEntry> findEntry(UUID ownerId, UUID moneyBoxId, UUID entryId);

    MoneyBoxEntry saveEntry(MoneyBoxEntry entry);

    void deleteEntry(UUID ownerId, UUID moneyBoxId, UUID entryId);
}
