package com.dowglasmaia.financecontroller.budget.application.port.out;

import com.dowglasmaia.financecontroller.budget.domain.BudgetConfig;
import com.dowglasmaia.financecontroller.budget.domain.MonthOverride;
import com.dowglasmaia.financecontroller.shared.money.Currency;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;
import java.util.UUID;

/** Porta de saída; filtrada por proprietário (P-01) e moeda (RN-CUR-02). */
public interface BudgetRepository {

    /** Configuração da moeda vista a partir de {@code month} (o ajuste devolvido é o desse mês). */
    BudgetConfig load(UUID ownerId, Currency currency, YearMonth month);

    /**
     * Grava tudo de uma vez, na transação do chamador: meta recorrente ({@code null} remove), ajuste de {@code month}
     * ({@code null} remove) e o conjunto completo de limites por categoria (substitui o anterior). Duas gravações
     * simultâneas da mesma moeda são serializadas.
     */
    void save(UUID ownerId, Currency currency, YearMonth month, BigDecimal monthlyLimit, MonthOverride override,
              Map<UUID, BigDecimal> categoryLimits);
}
