package com.dowglasmaia.financecontroller.moneybox.application.usecase;

import com.dowglasmaia.financecontroller.identity.CurrentOwner;
import com.dowglasmaia.financecontroller.moneybox.application.port.out.MoneyBoxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetMoneyBoxUseCase {

    private final MoneyBoxRepository boxes;
    private final CurrentOwner owner;

    GetMoneyBoxUseCase(MoneyBoxRepository boxes, CurrentOwner owner) {
        this.boxes = boxes;
        this.owner = owner;
    }

    @Transactional(readOnly = true)
    public MoneyBoxView execute(UUID moneyBoxId) {
        var ownerId = owner.id();
        var box = MoneyBoxAccess.find(boxes, ownerId, moneyBoxId);
        return MoneyBoxView.of(box, boxes.balance(ownerId, moneyBoxId));
    }
}
