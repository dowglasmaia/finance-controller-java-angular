package com.dowglasmaia.financecontroller.moneybox.infrastructure.persistence;

import com.dowglasmaia.financecontroller.moneybox.application.port.out.ReserveGoalRepository;
import com.dowglasmaia.financecontroller.shared.money.Currency;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaReserveGoalRepositoryAdapter implements ReserveGoalRepository {

    private final SpringDataReserveGoalRepository goals;

    JpaReserveGoalRepositoryAdapter(SpringDataReserveGoalRepository goals) {
        this.goals = goals;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BigDecimal> find(UUID ownerId, Currency currency) {
        return goals.find(ownerId, currency.name()).map(MoneyBoxEntities.Goal::getGoalAmount);
    }

    @Override
    @Transactional
    public void save(UUID ownerId, Currency currency, BigDecimal goal) {
        goals.find(ownerId, currency.name()).ifPresentOrElse(
            existing -> existing.setGoalAmount(goal),
            () -> goals.save(new MoneyBoxEntities.Goal(ownerId, currency.name(), goal)));
        goals.flush();
    }

    @Override
    @Transactional
    public void remove(UUID ownerId, Currency currency) {
        goals.find(ownerId, currency.name()).ifPresent(goals::delete);
        goals.flush();
    }
}
