/**
 * Metas de gastos por moeda: meta recorrente, ajuste por mês, limites por categoria, progresso e estado. Implementa
 * {@link com.dowglasmaia.financecontroller.transaction.BudgetImpactPort} (alerta ao salvar despesas) e lê as despesas
 * por {@link com.dowglasmaia.financecontroller.transaction.ExpenseQuery}.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Budget")
package com.dowglasmaia.financecontroller.budget;
