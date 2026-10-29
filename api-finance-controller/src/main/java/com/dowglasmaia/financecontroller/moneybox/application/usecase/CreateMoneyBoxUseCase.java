package com.dowglasmaia.financecontroller.moneybox.application.usecase;

import com.dowglasmaia.financecontroller.identity.CurrentOwner;
import com.dowglasmaia.financecontroller.moneybox.application.port.out.MoneyBoxRepository;
import com.dowglasmaia.financecontroller.moneybox.domain.MoneyBox;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;

@Service
public class CreateMoneyBoxUseCase {

    private final MoneyBoxRepository boxes;
    private final CurrentOwner owner;
    private final Clock clock;

    CreateMoneyBoxUseCase(MoneyBoxRepository boxes, CurrentOwner owner, Clock clock) {
        this.boxes = boxes;
        this.owner = owner;
        this.clock = clock;
    }

    @Transactional
    public MoneyBoxView execute(SaveMoneyBoxCommand command) {
        var box = MoneyBox.create(owner.id(), command.name(), command.description(), command.icon(), command.color(),
            command.currency(), command.goal(), clock.instant());
        return MoneyBoxView.of(boxes.save(box), BigDecimal.ZERO.setScale(2));
    }
}
