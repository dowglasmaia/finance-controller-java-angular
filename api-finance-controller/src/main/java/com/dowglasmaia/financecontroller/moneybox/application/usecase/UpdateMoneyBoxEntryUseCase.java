package com.dowglasmaia.financecontroller.moneybox.application.usecase;

import com.dowglasmaia.financecontroller.identity.CurrentOwner;
import com.dowglasmaia.financecontroller.moneybox.application.port.out.MoneyBoxRepository;
import com.dowglasmaia.financecontroller.moneybox.domain.Ledger;
import com.dowglasmaia.financecontroller.moneybox.domain.MoneyBoxEntry;
import com.dowglasmaia.financecontroller.shared.error.DomainException;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;
import com.dowglasmaia.financecontroller.shared.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.UUID;

/** Altera tipo, valor, data e nota; recusa se o saldo ficaria negativo em qualquer ponto do histórico (DESC-BOX-11). */
@Service
public class UpdateMoneyBoxEntryUseCase {

    private final MoneyBoxRepository boxes;
    private final CurrentOwner owner;

    UpdateMoneyBoxEntryUseCase(MoneyBoxRepository boxes, CurrentOwner owner) {
        this.boxes = boxes;
        this.owner = owner;
    }

    @Transactional
    public EntryView execute(UUID moneyBoxId, UUID entryId, SaveEntryCommand command) {
        var ownerId = owner.id();
        var box = MoneyBoxAccess.lock(boxes, ownerId, moneyBoxId);
        if (command.currency() != null && command.currency() != box.currency()) {
            throw new DomainException(ErrorCode.CURRENCY_MISMATCH, command.currency().name(), box.currency().name());
        }
        var current = boxes.findEntry(ownerId, moneyBoxId, entryId).orElseThrow(ResourceNotFoundException::new);
        var updated = current.update(command.kind(), command.amount(), command.date(), command.note());
        var history = new ArrayList<MoneyBoxEntry>();
        for (var entry : boxes.entries(ownerId, moneyBoxId)) {
            history.add(entry.id().equals(entryId) ? updated : entry);
        }
        Ledger.requireNeverNegative(history);
        return EntryView.of(box, boxes.saveEntry(updated));
    }
}
