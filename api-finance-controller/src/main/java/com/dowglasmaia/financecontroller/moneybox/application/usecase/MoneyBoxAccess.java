package com.dowglasmaia.financecontroller.moneybox.application.usecase;

import com.dowglasmaia.financecontroller.moneybox.application.port.out.MoneyBoxRepository;
import com.dowglasmaia.financecontroller.moneybox.domain.MoneyBox;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;
import com.dowglasmaia.financecontroller.shared.error.ResourceNotFoundException;

import java.util.UUID;

/** Carrega o cofrinho do proprietário; o de outro utilizador é indistinguível de um inexistente (404). */
final class MoneyBoxAccess {

    private MoneyBoxAccess() {
    }

    static MoneyBox find(MoneyBoxRepository boxes, UUID ownerId, UUID moneyBoxId) {
        return boxes.find(ownerId, moneyBoxId).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.MONEY_BOX_NOT_FOUND));
    }

    /** Com bloqueio da linha: serializa saques, edições e exclusões do mesmo cofrinho. */
    static MoneyBox lock(MoneyBoxRepository boxes, UUID ownerId, UUID moneyBoxId) {
        return boxes.findForUpdate(ownerId, moneyBoxId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.MONEY_BOX_NOT_FOUND));
    }
}
