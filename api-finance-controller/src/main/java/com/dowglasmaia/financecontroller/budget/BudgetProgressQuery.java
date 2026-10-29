package com.dowglasmaia.financecontroller.budget;

import com.dowglasmaia.financecontroller.shared.money.Currency;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

/**
 * API pública de leitura do módulo de metas para o dashboard: o progresso da meta global do mês (limite efetivo, gasto,
 * fração, restante e estado). Sem meta, o estado é {@code NONE} e limite, fração e restante são nulos.
 */
public interface BudgetProgressQuery {

    GlobalProgress globalProgress(UUID ownerId, Currency currency, YearMonth month);

    record GlobalProgress(BigDecimal limit, BigDecimal spent, BigDecimal fraction, BigDecimal remaining, Status status) {
    }

    enum Status { NONE, OK, WARNING, EXCEEDED }
}
