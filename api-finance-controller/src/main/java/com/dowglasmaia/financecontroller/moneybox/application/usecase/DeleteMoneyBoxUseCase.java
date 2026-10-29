package com.dowglasmaia.financecontroller.moneybox.application.usecase;

import com.dowglasmaia.financecontroller.identity.CurrentOwner;
import com.dowglasmaia.financecontroller.moneybox.application.port.out.MoneyBoxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Remove o cofrinho e as movimentações numa só transação; se qualquer remoção falhar, nada muda (RN-BOX-06). */
@Service
public class DeleteMoneyBoxUseCase {

    private final MoneyBoxRepository boxes;
    private final CurrentOwner owner;

    DeleteMoneyBoxUseCase(MoneyBoxRepository boxes, CurrentOwner owner) {
        this.boxes = boxes;
        this.owner = owner;
    }

    @Transactional
    public void execute(UUID moneyBoxId) {
        var ownerId = owner.id();
        MoneyBoxAccess.lock(boxes, ownerId, moneyBoxId);
        boxes.delete(ownerId, moneyBoxId);
    }
}
