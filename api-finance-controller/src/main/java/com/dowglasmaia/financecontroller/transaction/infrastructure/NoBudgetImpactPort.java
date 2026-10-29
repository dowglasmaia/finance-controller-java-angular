package com.dowglasmaia.financecontroller.transaction.infrastructure;

import com.dowglasmaia.financecontroller.transaction.application.port.out.BudgetImpactPort;
import com.dowglasmaia.financecontroller.transaction.domain.BudgetAlert;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Implementação provisória enquanto não há metas: nunca alerta. O módulo de metas (F6) a substitui. */
@Component
class NoBudgetImpactPort implements BudgetImpactPort {

    @Override
    public Optional<BudgetAlert> evaluate(Impact impact) {
        return Optional.empty();
    }
}
