package com.dowglasmaia.financecontroller.moneybox.application.usecase;

import com.dowglasmaia.financecontroller.identity.CurrentOwner;
import com.dowglasmaia.financecontroller.moneybox.application.port.out.MoneyBoxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ListMoneyBoxEntriesUseCase {

    private final MoneyBoxRepository boxes;
    private final CurrentOwner owner;

    ListMoneyBoxEntriesUseCase(MoneyBoxRepository boxes, CurrentOwner owner) {
        this.boxes = boxes;
        this.owner = owner;
    }

    @Transactional(readOnly = true)
    public List<EntryView> execute(UUID moneyBoxId) {
        var ownerId = owner.id();
        var box = MoneyBoxAccess.find(boxes, ownerId, moneyBoxId);
        return boxes.entries(ownerId, moneyBoxId).stream().map(entry -> EntryView.of(box, entry)).toList();
    }
}
