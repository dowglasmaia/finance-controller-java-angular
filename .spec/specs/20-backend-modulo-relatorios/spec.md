# 20-backend-modulo-relatorios

> **ID:** BACKEND-SPEC-15 · **Categoria:** Backend (módulo `report`, relatórios) · **Estado:** proposta para revisão — não aprovada, não executada.

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

- [ ] `period=WEEK` com `anchorDate=2026-06-24` → `range` 2026-06-21…2026-06-27 e 7 barras.
- [ ] `period=MONTH` junho de 2026 → `range` 01…30 e 5 barras (30 dias); fevereiro de 2026 (28 dias) → 4 barras.
- [ ] `period=YEAR` → 12 barras e `monthlySummary` com 12 itens; `changePercent` ausente no primeiro mês e quando o saldo anterior é 0.
- [ ] Com mês de referência diferente do atual, a soma das barras de entradas = `summary.income` (regressão de DESC-REP-03, aprovada em P-09).
- [ ] Os relatórios somam apenas dados do utilizador autenticado.
- [ ] Ranking e distribuição ignoram entradas e outra moeda; sem despesas → listas vazias.
- [ ] Lançamento em 2026-06-21 00:00 e em 2026-06-27 23:59 entram na semana de 24/06.

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

- [ ] Prévia aprovada e contrato `reports`.

### Back-end

- [ ] Implementar `ReportPeriod`/intervalos, agrupamentos, ranking e resumo mensal; controller `getReport`.

### Validação

- [ ] Executar os cenários e comparar com `report_service_test`.

## Resultado esperado

- Relatório coerente entre totais, barras e distribuição, com as semânticas de período da origem (sujeitas às decisões).
- Preservado o isolamento por moeda.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
