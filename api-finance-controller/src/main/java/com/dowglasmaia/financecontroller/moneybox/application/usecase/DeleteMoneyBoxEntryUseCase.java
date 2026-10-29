package com.dowglasmaia.financecontroller.moneybox.application.usecase;

import com.dowglasmaia.financecontroller.identity.CurrentOwner;
import com.dowglasmaia.financecontroller.moneybox.application.port.out.MoneyBoxRepository;
import com.dowglasmaia.financecontroller.moneybox.domain.Ledger;
import com.dowglasmaia.financecontroller.shared.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Recusa a exclusão que deixaria o saldo negativo em algum ponto, por exemplo apagar um depósito já "gasto". */
@Service
public class DeleteMoneyBoxEntryUseCase {

    private final MoneyBoxRepository boxes;
    private final CurrentOwner owner;

    DeleteMoneyBoxEntryUseCase(MoneyBoxRepository boxes, CurrentOwner owner) {
        this.boxes = boxes;
        this.owner = owner;
    }

    @Transactional
    public void execute(UUID moneyBoxId, UUID entryId) {
        var ownerId = owner.id();
        MoneyBoxAccess.lock(boxes, ownerId, moneyBoxId);
        boxes.findEntry(ownerId, moneyBoxId, entryId).orElseThrow(ResourceNotFoundException::new);
        var remaining = boxes.entries(ownerId, moneyBoxId).stream().filter(entry -> !entry.id().equals(entryId)).toList();
        Ledger.requireNeverNegative(remaining);
        boxes.deleteEntry(ownerId, moneyBoxId, entryId);
    }
}
