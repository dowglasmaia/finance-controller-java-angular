package com.dowglasmaia.financecontroller.moneybox.application.usecase;

import com.dowglasmaia.financecontroller.identity.CurrentOwner;
import com.dowglasmaia.financecontroller.moneybox.application.port.out.MoneyBoxRepository;
import com.dowglasmaia.financecontroller.moneybox.domain.Ledger;
import com.dowglasmaia.financecontroller.moneybox.domain.MoneyBoxEntry;
import com.dowglasmaia.financecontroller.shared.error.DomainException;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Registra depósito ou saque. A verificação do saldo e a gravação acontecem na mesma transação, com a linha do
 * cofrinho bloqueada: dois saques simultâneos não excedem o saldo (RN-BOX-05).
 */
@Service
public class CreateMoneyBoxEntryUseCase {

    private final MoneyBoxRepository boxes;
    private final CurrentOwner owner;
    private final Clock clock;

    CreateMoneyBoxEntryUseCase(MoneyBoxRepository boxes, CurrentOwner owner, Clock clock) {
        this.boxes = boxes;
        this.owner = owner;
        this.clock = clock;
    }

    @Transactional
    public EntryView execute(UUID moneyBoxId, SaveEntryCommand command) {
        var ownerId = owner.id();
        var box = MoneyBoxAccess.lock(boxes, ownerId, moneyBoxId);
        if (command.currency() != null && command.currency() != box.currency()) {
            throw new DomainException(ErrorCode.CURRENCY_MISMATCH, command.currency().name(), box.currency().name());
        }
        var entry = MoneyBoxEntry.create(ownerId, moneyBoxId, command.kind(), command.amount(), command.date(),
            command.note(), clock.instant());
        var history = new ArrayList<>(boxes.entries(ownerId, moneyBoxId));
        history.add(entry);
        Ledger.requireNeverNegative(history);
        return EntryView.of(box, boxes.saveEntry(entry));
    }
}
