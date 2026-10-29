# 37-frontend-pagina-relatorios

> **ID:** FRONTEND-SPEC-14 · **Categoria:** Frontend (`features/reports`) · **Estado:** executada na F9 (2026-10-29); teclado completo, tema claro e 320/768 px pendentes (F12) — ver evidências.

## Objetivo

Construir a página de relatórios por semana, mês ou ano: KPIs, evolução de entradas e saídas, despesas por categoria, ranking e resumo mensal anual.

## Escopo

- Inclui: página `/relatorios`, seletor de período, KPIs, gráficos, ranking, tabela anual.
- Não inclui: cálculos e âncora de período (backend).

## Contexto técnico local

### Página (CONFIRMADO — DESC-REP-04…06)

- Título `Relatórios` / `Análise financeira do período`; `MonthSwitcher` e seletor `Semana | Mês | Ano` (padrão Mês; não persistido).
- KPIs: Entradas, Saídas, Saldo (cor pelo sinal).
- `Evolução de Entradas e Saídas`: barras duplas — semana Dom…Sáb, mês S1…S5, ano Jan…Dez.
- `Despesas por Categoria` (pizza com legenda abaixo) e `Ranking de Categorias` (ícone, nome, %, valor e barra; máximo 7; vazio `Sem despesas no período.`).
- Só no período Ano: tabela `Resumo Mensal` com Mês, Entradas, Saídas, Saldo (cor) e `% vs anterior` (`—` na primeira linha ou com base zero).

### Operação consumida

`getReport(period, anchorDate, currency, rankingSize?)`. O cliente envia `anchorDate` = dia 15 do mês de referência, mantendo a regra atual (P-05, decidida).

### Correção de comportamento (DESC-REP-03; P-09, aprovada)

Na origem web o gráfico de barras era calculado com a data atual e não com o mês de referência, ficando incoerente com os KPIs quando o mês selecionado não é o atual. No destino as barras vêm do mesmo intervalo dos totais.

### Ranking (P-10, decidida)

Mantém-se o comportamento atual: as **7** maiores categorias de despesa (a documentação da origem fala em 5, mas não prevalece).

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [BACKEND-SPEC-15](../20-backend-modulo-relatorios/spec.md)
- [FRONTEND-SPEC-02](../25-frontend-design-system-e-componentes/spec.md)

## Regras e critérios de aceitação

- [x] Em Mês, a soma das barras de entradas é igual ao KPI Entradas, mesmo com o mês de referência diferente do atual (P-09 aprovada).
  > ✅ 2026-10-29 13:50 +00:00 — `reports-page.spec.ts` (tabela de leitor de tela com as 5 faixas e soma igual ao KPI) e no navegador contra a API real, junho/2026 com o app em outubro: Entradas R$ 3.250,00 e faixas S1 R$ 3.000,00 + S4 R$ 250,00; Saídas R$ 2.339,50 = 800,00 + 320,50 + 160,00 + 1.059,00.
- [x] Em Semana há 7 barras Dom…Sáb; em Ano há 12 barras e a tabela `Resumo Mensal`.
  > ✅ 2026-10-29 13:50 +00:00 — `reports-page.spec.ts` (Semana: pede `WEEK` e mostra Dom…Sáb sem `Resumo Mensal`; Ano: 12 barras e a tabela) e no navegador: semana 14/06 a 20/06 com 7 barras (saída de R$ 10,00 em Sáb) e ano 2026 com 12 barras e `Resumo Mensal`.
- [x] Pizza e ranking só incluem despesas da moeda selecionada.
  > ✅ 2026-10-29 13:50 +00:00 — Servidor: `distributionAndRankingCountOnlyExpensesOfTheCurrencyAndRankingIsLimited`. No navegador: BRL (Saúde 42,7%, Moradia 34,2%, …) e, ao trocar para EUR, só os valores em euro (entradas € 100,00 e saídas € 40,00 no ano); `reports-page.spec.ts` cobre a nova consulta ao trocar a moeda, descartando a resposta antiga.
- [x] `% vs anterior` aparece como `—` na primeira linha e quando o saldo anterior é 0.
  > ✅ 2026-10-29 13:50 +00:00 — `reports-page.spec.ts` (primeira linha e base zero mostram `—`; `+50,0%` e `-100,0%` com cor e sinal) e no navegador (ano 2026: Mai `—` com abril zerado, Jun `-39,3%`, Jul `-100,0%`).
- [ ] Gráficos têm alternativa textual; vazio, carregando e erro tratados.
  > ⏳ 2026-10-29 13:50 +00:00 — Barras: `role="img"` com `aria-label` por período e tabela para leitor de tela (`Dia`/`Faixa`/`Mês`); pizza com legenda por extenso e ranking com texto. Estados testados em `reports-page.spec.ts`: carregando, vazio (`Sem despesas no período.`), erro com `Tentar novamente`. Falta: leitor de tela de verdade, teclado completo e tema claro (F12).

### Skills de implementação

- **Skills obrigatórias:** `angular-module-scaffold`, `angular-tailwind-ui-specialist`, `angular-architect`.
- **Caminhos:** `.claude/skills/angular-module-scaffold/SKILL.md`; `.claude/skills/angular-tailwind-ui-specialist/SKILL.md`; `.claude/skills/angular-architect/SKILL.md`.
- **Responsabilidade:** módulo e `doc.md`; apresentação dos gráficos e tabelas; estado por período.
- **Momento de utilização:** depois do contrato `reports`.
- **Dependências:** FRONTEND-SPEC-00…04, 25; BACKEND-SPEC-15; ADR-013 (ApexCharts).
- **Limitações:** nenhuma específica.

## Tasks

### Front-end

- [x] Criar `features/reports` com página, gráficos, ranking, tabela anual e serviço de dados.
  > ✅ 2026-10-29 13:50 +00:00 — `features/reports` (`reports.routes.ts`, `pages/reports-page.ts`, `data-access/` com `ReportsApiService` e `ReportsStore`, `ui/` com `RankingList` e `MonthlySummaryTable`, `reports-format.ts`, `doc.md`); tipos gerados em `core/api/generated/reports.ts`; rota `/relatorios` real no lugar do último placeholder. Reaproveita `KpiCard`, `IncomeExpenseBars` e `ExpensePie` do dashboard (as barras ganharam rótulos de acessibilidade configuráveis, com o padrão do dashboard). Gráficos em SVG próprio, sem ApexCharts (ADR-013 não foi seguida: o dashboard da F7 já nasceu sem biblioteca).

### Validação

- [ ] Testar coerência totais × barras, os três períodos e os estados vazios.
  > ⏳ 2026-10-29 13:50 +00:00 — `npm test`: 196 testes Vitest verdes (10 novos em `reports-page.spec.ts` e `reports-format`), `ng build` verde e percurso no navegador contra a API real (tema escuro, desktop; mês, semana, ano e EUR). Falta: tema claro, 320/768 px, teclado completo e leitor de tela (F12).

## Resultado esperado

- Relatórios equivalentes à origem, com totais e gráfico consistentes.
- Preservado o isolamento por moeda.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
