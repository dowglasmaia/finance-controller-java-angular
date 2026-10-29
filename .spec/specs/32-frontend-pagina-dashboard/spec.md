# 32-frontend-pagina-dashboard

> **ID:** FRONTEND-SPEC-09 · **Categoria:** Frontend (`features/dashboard`) · **Estado:** executada na F7 (2026-10-29); a revisão no tema claro e em 320 px fica pendente — ver evidências.

## Objetivo

Construir o dashboard mensal: resumo de saldo, entradas, saídas e reservas, gráficos e últimos lançamentos, do mês de referência e da moeda selecionados.

## Escopo

- Inclui: página `/`, KPIs, gráfico de barras, pizza por categoria, tabela de últimos lançamentos, estados.
- Não inclui: cálculos (BACKEND-SPEC-12), formulário de lançamento (FRONTEND-SPEC-10).

## Contexto técnico local

### Conteúdo (CONFIRMADO — DESC-DSH-01/02)

| Bloco | Detalhe |
|---|---|
| Cabeçalho | saudação por hora do dia (`Bom dia` <12h, `Boa tarde` <18h, `Boa noite`), subtítulo `Resumo de Junho 2026`, `MonthSwitcher` |
| KPIs | Saldo (cor verde se ≥ 0, coral se < 0), Entradas e Saídas com variação vs mês anterior (para Saídas, menor é melhor), Reservas (total acumulado) |
| Meta | progresso da meta global do mês (P-14; ver abaixo) |
| Entradas × Saídas | barras duplas por faixa S1…S5 do mês; dica com o valor formatado |
| Por Categoria | pizza com percentuais e legenda (ícone, nome, %, valor); vazio: `Sem despesas no período.` |
| Últimas Transações | até 8 do mês: data, descrição, categoria, subcategoria, tipo (chip Entrada/Saída), valor com sinal e cor; link `Ver todas →` para Lançamentos; vazio: `Nenhum lançamento no período.` |

Subcategoria ausente ou categoria sem nome: `—`.

### Operação consumida

`getMonthlyOverview(month, currency)` — uma chamada por mês/moeda (BACKEND-SPEC-12). Recarrega ao mudar mês ou moeda.

### Meta no dashboard (P-14, decidida)

A origem web **não** mostra o progresso de metas (DESC-DSH-03; só o dashboard móvel tinha o `BudgetCard`), mas a meta é um valor de referência — um propósito de gastos — e o produto quer vê-la no dashboard. Acrescentar um bloco `Meta de {Mês Ano}` com estado (ícone, rótulo `Dentro da meta` / `Perto da meta` / `Meta estourada` / `Sem meta definida` e cor), barra de progresso (limitada a 100%), `{gasto} de {limite}`, `N%` e quanto resta ou excedeu, vindos de `budgetProgress` na própria resposta de `getMonthlyOverview`. Sem meta: convite para defini-la com link para `/metas`.

### Estados

Carregando (esqueletos nos KPIs e gráficos), erro com "Tentar novamente", vazio (mês sem lançamentos mostra KPIs zerados e as mensagens acima), sucesso.

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [BACKEND-SPEC-12](../17-backend-modulo-resumo-e-dashboard/spec.md)
- [FRONTEND-SPEC-02](../25-frontend-design-system-e-componentes/spec.md)

## Regras e critérios de aceitação

- [x] Para BRL +1000/−250 o Saldo mostra `R$ 750,00` em verde; trocar para EUR mostra apenas valores EUR.
  > ✅ 2026-10-29 12:45 +00:00 — `dashboard-page.spec.ts` (Saldo `R$ 750,00` com a classe de cor positiva; negativo em coral; uma chamada por mês e moeda; EUR consulta `currency=EUR` e mostra `€` sem `R$`) e no navegador contra a API real (BRL e EUR).
- [x] Mês anterior com entradas 0 não mostra variação; variações menores que 0,5% mostram `0%`.
  > ✅ 2026-10-29 12:45 +00:00 — `dashboard-page.spec.ts` (sem base não aparece `sobre o mês anterior`; `"0"` mostra `0%`) e `dashboard-format.spec.ts`; no navegador, setembro (sem base) não mostrou variação e outubro mostrou `+25,0%`.
- [x] Mudar o mês atualiza todos os blocos e nunca mostra dados do mês anterior após a resposta nova.
  > ✅ 2026-10-29 12:45 +00:00 — `dashboard-page.spec.ts` (ao mudar o mês os esqueletos substituem o dado antigo, a nova consulta usa o `month` novo e só a resposta nova aparece; `switchMap` cancela a anterior) e no navegador (outubro → setembro).
- [x] A tabela mostra no máximo 8 lançamentos e o link leva a `/lancamentos`.
  > ✅ 2026-10-29 12:45 +00:00 — O limite de 8 é do servidor (`latestTransactionsAreAtMostEight…`); `dashboard-page.spec.ts` verifica as linhas, o tipo, o valor com sinal e o link `Ver todas →` para `/lancamentos`.
- [x] Mensagens de vazio idênticas às da origem; erros mostram repetição.
  > ✅ 2026-10-29 12:45 +00:00 — `dashboard-page.spec.ts` (`Nenhum lançamento no período.`, `Sem despesas no período.`, KPIs zerados; erro com `Tentar novamente` que consulta de novo) e no navegador (EUR vazio).
- [x] Gráficos têm alternativa textual (tabela ou descrição) e os estados não dependem só de cor.
  > ✅ 2026-10-29 12:45 +00:00 — `dashboard-page.spec.ts` (barras: `role="img"`, `aria-label`, tabela para leitor de tela e dica com o valor; pizza: legenda com ícone, nome, percentual e valor; variação com sinal, ícone e frase por extenso; meta com ícone e texto). Falta: verificação com leitor de tela real.
- [x] Com meta 1000 e gasto 850 o bloco mostra `Perto da meta`, 85% e 150 restantes; sem meta mostra o convite com link para `/metas`.
  > ✅ 2026-10-29 12:45 +00:00 — `dashboard-page.spec.ts` (`Perto da meta`, `85%`, `Restam R$ 150,00`; estourada mostra `Excedeu`; sem meta, convite com link para `/metas`) e no navegador (meta 2000: `53%`, `Restam R$ 949,50`).
- [x] Trocar a moeda mostra o dashboard da outra moeda (saldo, metas e reservas daquela moeda).
  > ✅ 2026-10-29 12:45 +00:00 — `dashboard-page.spec.ts` e no navegador (EUR: zeros, convite sem meta e mensagens de vazio). As reservas vêm do servidor por moeda (reais desde a F8: no navegador o dashboard mostrou `Reservas R$ 1.200,00` com os cofrinhos).

### Skills de implementação

- **Skills obrigatórias:** `angular-module-scaffold`, `angular-tailwind-ui-specialist`, `angular-architect`.
- **Caminhos:** `.claude/skills/angular-module-scaffold/SKILL.md`; `.claude/skills/angular-tailwind-ui-specialist/SKILL.md`; `.claude/skills/angular-architect/SKILL.md`.
- **Responsabilidade:** estrutura do módulo e `doc.md` (primeira); apresentação responsiva (segunda); estado e tipagem (terceira).
- **Momento de utilização:** depois do contrato `overview` e do shell.
- **Dependências:** FRONTEND-SPEC-00…04, 25; BACKEND-SPEC-12, 13.
- **Limitações:** a skill de módulo exige selecionar o módulo/operação (`getMonthlyOverview`).

## Tasks

### Front-end

- [x] Criar `features/dashboard` com rota, página, componentes de KPI/gráficos e `doc.md`.
  > ✅ 2026-10-29 12:45 +00:00 — `features/dashboard` (`dashboard.routes.ts`, `pages/`, `ui/`, `data-access/`, `dashboard-format.ts`, `doc.md`); `KpiCard` em `shared/ui`; tipos gerados em `core/api/generated/overview.ts`; gráficos em SVG puro, sem biblioteca nova; `BudgetProgressCard` reutilizado (agora mostra `Restam`/`Excedeu`). Build de produção verde; a rota `/` substituiu o placeholder.

### Validação

- [ ] Testar os cenários dos critérios e revisar a UI nos dois temas e em 320 px.
  > ⏳ 2026-10-29 12:45 +00:00 — Parcial: `npm test` com 171 testes Vitest verdes (29 novos) e revisão visual no navegador (tema escuro, desktop) contra a API real; uma correção feita (nome da categoria sumia na legenda da pizza). Falta: tema claro, 320 px, teclado completo. Próxima ação: fazer a revisão manual (F12).

## Resultado esperado

- Dashboard equivalente ao da origem web, reativo a mês e moeda.
- Preservada a regra: o cliente apresenta o que o servidor calcula.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
