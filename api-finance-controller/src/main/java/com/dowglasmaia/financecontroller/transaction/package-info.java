/**
 * Lançamentos (entradas e saídas): criação, listagem mensal por moeda com totais, edição e exclusão, e o ponto de
 * entrada do alerta de meta. Implementa {@link com.dowglasmaia.financecontroller.category.CategoryUsageGuard} e expõe
 * {@link com.dowglasmaia.financecontroller.transaction.ExpenseQuery} aos módulos de metas, dashboard e relatórios.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Transaction")
package com.dowglasmaia.financecontroller.transaction;
