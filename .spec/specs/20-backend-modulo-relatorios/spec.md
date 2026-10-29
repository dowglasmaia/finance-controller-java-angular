# 20-backend-modulo-relatorios

> **ID:** BACKEND-SPEC-15 · **Categoria:** Backend (módulo `report`, relatórios) · **Estado:** executada na F9 (2026-10-29); comparação literal com `report_service_test` pendente — ver evidências.

## Objetivo

Implementar o relatório por semana, mês ou ano — resumo, barras, distribuição, ranking e resumo mensal — com totais e gráfico calculados sobre o mesmo intervalo.

## Escopo

- Inclui: `getReport`.
- Não inclui: visão geral do dashboard (BACKEND-SPEC-12), interface (FRONTEND-SPEC-14).

## Contexto técnico local

### Regras aplicadas

RN-REP-01…05. Evidências: DESC-REP-01…08.

### Cálculo do intervalo

`range = rangeFor(period, anchorDate)`: semana = domingo a sábado que contém `anchorDate`; mês = mês de `anchorDate`; ano = ano de `anchorDate`. Na origem, a âncora é o dia 15 do mês de referência (DESC-REP-02). **Regra mantida (P-05)**: o cliente envia `anchorDate` = `AAAA-MM-15` do mês selecionado.

### Blocos da resposta

| Bloco | Regra |
|---|---|
| `summary` | entradas, saídas, saldo no intervalo e moeda |
| `bars` | semana: 7 (Dom…Sáb); mês: `ceil(últimoDia/7)` faixas S1…S5; ano: 12 (Jan…Dez) |
| `expenseByCategory` | só despesas, com percentual do total das despesas |
| `ranking` | `expenseByCategory` ordenado desc, limitado a `rankingSize` (padrão 7, comportamento atual — P-10); categoria sem registo → `Sem categoria` (inalcançável com integridade referencial) |
| `monthlySummary` | só `YEAR`: por mês entradas, saídas, saldo e `changePercent` |

### Correção proposta (DESC-REP-03)

A origem calcula `bars` com a data atual quando o relatório é aberto com um mês de referência diferente do atual, enquanto os totais usam o período ancorado. No destino, `bars` e `summary` derivam do **mesmo** `range`. Isto altera o comportamento observado e foi **aprovado** (P-09: os módulos novos nascem com a lógica correta).

### Riscos

Fuso e fronteira de dia: com `LocalDate`, os limites inclusivos por dia eliminam a dependência de `dayEnd` 23:59:59.999 da origem (DESC-DAT-04).

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [BACKEND-SPEC-01](../07-backend-dominio-e-regras-de-negocio/spec.md)
- [FRONTEND-SPEC-14](../37-frontend-pagina-relatorios/spec.md)

## Regras e critérios de aceitação

- [x] `period=WEEK` com `anchorDate=2026-06-24` → `range` 2026-06-21…2026-06-27 e 7 barras.
  > ✅ 2026-10-29 13:50 +00:00 — `ReportsFlowIntegrationTest.weekIsSundayToSaturdayWithSevenBarsAndInclusiveEdges` (PostgreSQL: `range` 2026-06-21…2026-06-27, 7 barras `Dom`…`Sáb`) e `ReportCalculationsTests.weekIsSundayToSaturdayAroundTheAnchor` (âncora no domingo, no sábado e no meio da semana); `weekAcrossTheTurnOfYearIsOneContinuousRange` (âncora 2026-12-31 → 2026-12-27…2027-01-02).
- [x] `period=MONTH` junho de 2026 → `range` 01…30 e 5 barras (30 dias); fevereiro de 2026 (28 dias) → 4 barras.
  > ✅ 2026-10-29 13:50 +00:00 — `monthBarsAddUpToTheTotalsForAnyReferenceMonthAndLengthOfMonth` (junho: 01…30 e 5 barras; fevereiro/2026: 4; fevereiro/2028 bissexto: 5, com o dia 29 em S5) e `ReportCalculationsTests.monthBarsHaveCeilOfDaysOverSevenBands`.
- [x] `period=YEAR` → 12 barras e `monthlySummary` com 12 itens; `changePercent` ausente no primeiro mês e quando o saldo anterior é 0.
  > ✅ 2026-10-29 13:50 +00:00 — `yearHasTwelveBarsAndAMonthlySummaryWithBalanceChange`: 12 barras `Jan`…`Dez` e `monthlySummary` com 12 itens; `changePercent` ausente em janeiro e quando o saldo anterior é 0 (fev +50.0, mar -100.0, abr ausente, mai ausente porque abril é 0, jun +200.0 sobre saldo anterior negativo `-50`); lançamentos de 2025 e 2027 ficam de fora. `ReportCalculationsTests.balanceChangeUsesTheAbsolutePreviousBalanceAndIsAbsentFromZero`.
- [x] Com mês de referência diferente do atual, a soma das barras de entradas = `summary.income` (regressão de DESC-REP-03, aprovada em P-09).
  > ✅ 2026-10-29 13:50 +00:00 — `monthBarsAddUpToTheTotalsForAnyReferenceMonthAndLengthOfMonth`: junho/2026 e fevereiro/2028 (o servidor está em outubro/2026) com soma das barras de entradas e de saídas igual a `summary` (1250.50 e 75.25). No navegador, junho/2026: entradas R$ 3.250,00 e as faixas S1…S5 somam o mesmo valor. Corrige DESC-REP-03 (P-09).
- [x] Os relatórios somam apenas dados do utilizador autenticado.
  > ✅ 2026-10-29 13:50 +00:00 — `reportsOnlyCountTheAuthenticatedUsersData`: outro utilizador vê `0.00` nas entradas e saídas e ranking vazio; `requiresAuthentication` (401 sem token).
- [x] Ranking e distribuição ignoram entradas e outra moeda; sem despesas → listas vazias.
  > ✅ 2026-10-29 13:50 +00:00 — `distributionAndRankingCountOnlyExpensesOfTheCurrencyAndRankingIsLimited` (10 categorias de despesa de 100 a 10 mais uma entrada de 5000 e uma despesa em EUR: `expenseByCategory` com 10 itens e total 550.00; ranking padrão com 7 (100…40) e com `rankingSize=3`; 18.1818% na primeira; percentuais somam 100 com tolerância de 0.01; EUR separado com 100.0000%) e `emptyPeriodIsZerosAndEmptyListsNotAnError` (listas vazias, zeros, 5 barras). Movimentações de cofrinho não entram (`moneyBoxMovementsAreNotPartOfTheReport`).
- [x] Lançamento em 2026-06-21 00:00 e em 2026-06-27 23:59 entram na semana de 24/06.
  > ✅ 2026-10-29 13:50 +00:00 — O período é por `LocalDate` inclusivo (sem hora): `weekIsSundayToSaturdayWithSevenBarsAndInclusiveEdges` inclui 06-21 e 06-27 e deixa 06-20 e 06-28 de fora; a dependência de `dayEnd` 23:59:59.999 da origem (DESC-DAT-04) deixou de existir.

### Cenários de teste

Semana na virada de mês e de ano; mês de 28/29/30/31 dias; ano com meses vazios; ranking com mais de 7 categorias; percentuais somando ~100%; BRL × EUR.

### Skills de implementação

- **Skills obrigatórias:** `java-modern-engineering`, `java-spring-create-module`, `java-persistence-performance`; `openapi-rest-contract` para o contrato `reports`.
- **Caminhos:** `C:\Users\dowgl\.codex\skills\java-modern-engineering\SKILL.md`; `C:\Users\dowgl\.codex\skills\java-spring-create-module\SKILL.md`; `C:\Users\dowgl\.codex\skills\openapi-rest-contract\SKILL.md`; `.claude/skills/java-persistence-performance/SKILL.md`.
- **Responsabilidade:** contrato, cálculo de intervalos e agrupamentos, adaptador de leitura.
- **Momento de utilização:** depois de `transaction` e `category`.
- **Dependências:** BACKEND-SPEC-05, 10, 11.
- **Limitações:** nenhuma específica.

## Tasks

### Contrato e dados

- [x] Prévia aprovada e contrato `reports`.
  > ✅ 2026-10-29 13:50 +00:00 — Prévia aprovada pelo utilizador em conversa (1 operação, schemas e 8 decisões: `GET /api/v1/reports`, rótulos pt-BR vindos do servidor, `changePercent` com 1 casa, `anchorDate` 2000–2100, semana cruzando mês/ano, cofrinhos fora, consultas de leitura por intervalo, SVG próprio, âncora no dia 15) e gerada em `openapi/modules/reports.yaml` + execução `generate-reports-api`. Desvio: YAML escrito à mão.

### Back-end

- [x] Implementar `ReportPeriod`/intervalos, agrupamentos, ranking e resumo mensal; controller `getReport`.
  > ✅ 2026-10-29 13:50 +00:00 — Módulo `report`: `ReportPeriod`, `ReportRange` (semana domingo–sábado, mês, ano; âncora validada), `PeriodBars`, `Percentage.change`, `GetReportUseCase` e `ReportsController`. Só APIs públicas de leitura: `TransactionQuery.rangeTotals/dailyTotals` e `ExpenseQuery.expenseTotalsByCategory` por intervalo (as versões por mês passaram a delegar às de intervalo). 4 consultas por chamada (totais, totais por dia, despesas por categoria, categorias), qualquer que seja o volume. `./mvnw clean verify` com Docker: 339 testes, 0 falhas (18 novos).

### Validação

- [ ] Executar os cenários e comparar com `report_service_test`.
  > ⏳ 2026-10-29 13:50 +00:00 — Cenários da spec executados em PostgreSQL real (acima): semana na virada de ano, mês de 28/29/30 dias, ano com meses vazios, ranking com mais de 7 categorias, percentuais somando ~100%, BRL × EUR. Falta: comparar caso a caso com `report_service_test`, que é do projeto de origem e não está no repositório. Os meses de 31 dias só têm teste unitário de contagem de faixas (julho).

## Resultado esperado

- Relatório coerente entre totais, barras e distribuição, com as semânticas de período da origem (sujeitas às decisões).
- Preservado o isolamento por moeda.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
