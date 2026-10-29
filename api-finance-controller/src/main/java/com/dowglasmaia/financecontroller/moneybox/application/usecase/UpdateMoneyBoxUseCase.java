package com.dowglasmaia.financecontroller.moneybox.application.usecase;

import com.dowglasmaia.financecontroller.identity.CurrentOwner;
import com.dowglasmaia.financecontroller.moneybox.application.port.out.MoneyBoxRepository;
import com.dowglasmaia.financecontroller.shared.error.DomainException;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** A moeda só muda enquanto não há movimentações (RN-BOX-04); o cofrinho fica bloqueado durante a verificação. */
@Service
public class UpdateMoneyBoxUseCase {

    private final MoneyBoxRepository boxes;
    private final CurrentOwner owner;

    UpdateMoneyBoxUseCase(MoneyBoxRepository boxes, CurrentOwner owner) {
        this.boxes = boxes;
        this.owner = owner;
    }

    @Transactional
    public MoneyBoxView execute(UUID moneyBoxId, SaveMoneyBoxCommand command) {
        var ownerId = owner.id();
        var current = MoneyBoxAccess.lock(boxes, ownerId, moneyBoxId);
        var updated = current.update(command.name(), command.description(), command.icon(), command.color(),
            command.currency(), command.goal());
        if (updated.currency() != current.currency() && boxes.hasEntries(ownerId, moneyBoxId)) {
            throw new DomainException(ErrorCode.CURRENCY_LOCKED);
        }
        var saved = boxes.save(updated);
        return MoneyBoxView.of(saved, boxes.balance(ownerId, moneyBoxId));
    }
}
