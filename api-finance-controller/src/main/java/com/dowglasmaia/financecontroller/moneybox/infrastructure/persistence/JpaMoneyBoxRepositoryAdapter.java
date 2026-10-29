package com.dowglasmaia.financecontroller.moneybox.infrastructure.persistence;

import com.dowglasmaia.financecontroller.moneybox.application.port.out.MoneyBoxRepository;
import com.dowglasmaia.financecontroller.moneybox.domain.EntryKind;
import com.dowglasmaia.financecontroller.moneybox.domain.MoneyBox;
import com.dowglasmaia.financecontroller.moneybox.domain.MoneyBoxEntry;
import com.dowglasmaia.financecontroller.shared.money.Currency;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaMoneyBoxRepositoryAdapter implements MoneyBoxRepository {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final SpringDataMoneyBoxRepository boxes;
    private final SpringDataMoneyBoxEntryRepository entries;

    JpaMoneyBoxRepositoryAdapter(SpringDataMoneyBoxRepository boxes, SpringDataMoneyBoxEntryRepository entries) {
        this.boxes = boxes;
        this.entries = entries;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MoneyBox> findAll(UUID ownerId, Currency currency) {
        return boxes.findAll(ownerId, currency.name()).stream().map(JpaMoneyBoxRepositoryAdapter::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MoneyBox> find(UUID ownerId, UUID moneyBoxId) {
        return boxes.find(ownerId, moneyBoxId).map(JpaMoneyBoxRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional
    public Optional<MoneyBox> findForUpdate(UUID ownerId, UUID moneyBoxId) {
        return boxes.findForUpdate(ownerId, moneyBoxId).map(JpaMoneyBoxRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional
    public MoneyBox save(MoneyBox box) {
        var entity = boxes.find(box.ownerId(), box.id())
            .orElseGet(() -> new MoneyBoxEntities.Box(box.id(), box.ownerId(), box.createdAt()));
        entity.update(box.name(), box.description(), box.icon(), box.color(), box.currency().name(), box.goal());
        return toDomain(boxes.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public void delete(UUID ownerId, UUID moneyBoxId) {
        entries.deleteAllOf(ownerId, moneyBoxId);
        boxes.deleteBox(ownerId, moneyBoxId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasEntries(UUID ownerId, UUID moneyBoxId) {
        return entries.existsByOwnerIdAndMoneyBoxId(ownerId, moneyBoxId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, BigDecimal> balances(UUID ownerId, Currency currency) {
        Map<UUID, BigDecimal> balances = new HashMap<>();
        for (Object[] row : entries.balances(ownerId, currency.name())) {
            balances.put((UUID) row[0], ((BigDecimal) row[1]).setScale(2));
        }
        return balances;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal balance(UUID ownerId, UUID moneyBoxId) {
        return orZero(entries.balance(ownerId, moneyBoxId));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal totalBalance(UUID ownerId, Currency currency) {
        return orZero(entries.total(ownerId, currency.name()));
    }

    @Override
    @Transactional(readOnly = true)
    public long count(UUID ownerId, Currency currency) {
        return boxes.count(ownerId, currency.name());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MoneyBoxEntry> entries(UUID ownerId, UUID moneyBoxId) {
        return entries.findAllOf(ownerId, moneyBoxId).stream().map(JpaMoneyBoxRepositoryAdapter::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MoneyBoxEntry> findEntry(UUID ownerId, UUID moneyBoxId, UUID entryId) {
        return entries.find(ownerId, moneyBoxId, entryId).map(JpaMoneyBoxRepositoryAdapter::toDomain);
    }

    /** Flush imediato: violações de integridade surgem dentro do caso de uso. */
    @Override
    @Transactional
    public MoneyBoxEntry saveEntry(MoneyBoxEntry entry) {
        var entity = entries.find(entry.ownerId(), entry.moneyBoxId(), entry.id())
            .orElseGet(() -> new MoneyBoxEntities.Entry(entry.id(), entry.ownerId(), entry.moneyBoxId(),
                entry.createdAt()));
        entity.update(entry.kind().name(), entry.amount(), entry.date(), entry.note());
        return toDomain(entries.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public void deleteEntry(UUID ownerId, UUID moneyBoxId, UUID entryId) {
        entries.find(ownerId, moneyBoxId, entryId).ifPresent(entries::delete);
        entries.flush();
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2);
    }

    private static MoneyBox toDomain(MoneyBoxEntities.Box e) {
        return new MoneyBox(e.getId(), e.getOwnerId(), e.getName(), e.getDescription(), e.getIcon(), e.getColor(),
            Currency.valueOf(e.getCurrency()), e.getGoal(), e.getCreatedAt());
    }

    private static MoneyBoxEntry toDomain(MoneyBoxEntities.Entry e) {
        return new MoneyBoxEntry(e.getId(), e.getOwnerId(), e.getMoneyBoxId(), EntryKind.valueOf(e.getKind()),
            e.getAmount(), e.getEntryDate(), e.getNote(), e.getCreatedAt());
    }
}
