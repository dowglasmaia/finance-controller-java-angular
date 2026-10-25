# 32-frontend-pagina-dashboard

> **ID:** FRONTEND-SPEC-09 · **Categoria:** Frontend (`features/dashboard`) · **Estado:** proposta para revisão — não aprovada, não executada.

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

- [ ] Para BRL +1000/−250 o Saldo mostra `R$ 750,00` em verde; trocar para EUR mostra apenas valores EUR.
- [ ] Mês anterior com entradas 0 não mostra variação; variações menores que 0,5% mostram `0%`.
- [ ] Mudar o mês atualiza todos os blocos e nunca mostra dados do mês anterior após a resposta nova.
- [ ] A tabela mostra no máximo 8 lançamentos e o link leva a `/lancamentos`.
- [ ] Mensagens de vazio idênticas às da origem; erros mostram repetição.
- [ ] Gráficos têm alternativa textual (tabela ou descrição) e os estados não dependem só de cor.
- [ ] Com meta 1000 e gasto 850 o bloco mostra `Perto da meta`, 85% e 150 restantes; sem meta mostra o convite com link para `/metas`.
- [ ] Trocar a moeda mostra o dashboard da outra moeda (saldo, metas e reservas daquela moeda).

### Skills de implementação

- **Skills obrigatórias:** `angular-module-scaffold`, `angular-tailwind-ui-specialist`, `angular-architect`.
- **Caminhos:** `.claude/skills/angular-module-scaffold/SKILL.md`; `.claude/skills/angular-tailwind-ui-specialist/SKILL.md`; `.claude/skills/angular-architect/SKILL.md`.
- **Responsabilidade:** estrutura do módulo e `doc.md` (primeira); apresentação responsiva (segunda); estado e tipagem (terceira).
- **Momento de utilização:** depois do contrato `overview` e do shell.
- **Dependências:** FRONTEND-SPEC-00…04, 25; BACKEND-SPEC-12, 13.
- **Limitações:** a skill de módulo exige selecionar o módulo/operação (`getMonthlyOverview`).

## Tasks

### Front-end

- [ ] Criar `features/dashboard` com rota, página, componentes de KPI/gráficos e `doc.md`.

### Validação

- [ ] Testar os cenários dos critérios e revisar a UI nos dois temas e em 320 px.

## Resultado esperado

- Dashboard equivalente ao da origem web, reativo a mês e moeda.
- Preservada a regra: o cliente apresenta o que o servidor calcula.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
