# 35-frontend-pagina-cofrinhos

> **ID:** FRONTEND-SPEC-12 · **Categoria:** Frontend (`features/money-boxes`) · **Estado:** executada na F8 (2026-10-29); teclado completo, tema claro e 320/768 px pendentes (F12) — ver evidências.

## Objetivo

Construir a página de cofrinhos: resumo de reservas, meta geral, grade de cartões, detalhe com histórico e formulários de cofrinho e de movimentação.

## Escopo

- Inclui: página `/cofrinhos`, KPIs, meta geral, cartões, detalhe, diálogos de cofrinho, movimentação e meta geral.
- Não inclui: cálculos (backend), lançamentos mensais.

## Contexto técnico local

### Página (CONFIRMADO — DESC-BOX-10)

- Cabeçalho `Cofrinhos` / `Suas reservas e o progresso rumo às metas`; ações `Definir meta geral` / `Editar meta geral` e `Novo Cofrinho`.
- KPIs: Saldo Atual, Meta Total (`—` sem meta), Progresso (`N%` ou `—`), Reservas (quantidade). Barra de progresso geral e texto `N% concluído — {total} de {meta}` quando há meta.
- Grade de cartões (só a moeda selecionada, ordem de criação): ícone/cor, nome, descrição, saldo grande, barra individual e `N% de {meta}` se houver meta, ações Editar e Excluir, botões `Aporte` e `Saque`.
- Vazio: `Nenhum cofrinho ainda. Crie sua primeira reserva.`

### Diálogos (DESC-BOX-05/07/11/12)

- **Cofrinho:** `Novo Cofrinho` / `Editar Cofrinho`; nome\*, moeda\* (desabilitada com aportes), `Onde está guardado (opcional)`, objetivo (opcional, ≥ 0), 10 ícones, 12 cores; toast `Cofrinho salvo.`
- **Meta geral:** `Meta geral de reservas` com a explicação "O alvo total que você quer guardar somando todos os cofrinhos. Deixe vazio para não usar meta geral."; toast `Meta geral salva.`
- **Movimentação:** `Movimentar {nome}` / `Editar movimento`; alternador Depósito/Saque, valor\*, data, observação; botão `Depositar` (verde) ou `Sacar` (coral); toasts `Aporte registrado.` / `Saque registrado.`
- **Detalhe:** cabeçalho com saldo, barra de meta, lista `Aportes` (mais recentes primeiro; sinal `+`/`−` com cor; nota padrão `Depósito`/`Saque`), menu Editar/Excluir por movimentação com confirmação; vazio `Nenhum aporte ainda.`; botões `Aporte` e `Saque`.
- **Exclusão do cofrinho:** confirmação com o nome; toast `Cofrinho excluído.` (remove também as movimentações).

### Operações consumidas

`listMoneyBoxes`, `createMoneyBox`, `updateMoneyBox`, `deleteMoneyBox`, `listMoneyBoxEntries`, `createMoneyBoxEntry`, `updateMoneyBoxEntry`, `deleteMoneyBoxEntry`, `getReservesSummary`, `saveReserveGoal`.

### Pendências

P-04 (decidida): o saldo nunca fica negativo. Com saldo `≤ 0`, o botão `Saque` fica desabilitado com a explicação "Saldo insuficiente"; com saldo positivo, um saque maior que o saldo, e qualquer edição ou exclusão de movimentação que negativaria o saldo, mostram o erro `INSUFFICIENT_BALANCE` do servidor e nada muda. O histórico de movimentações é sempre mantido.

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [BACKEND-SPEC-14](../19-backend-modulo-cofrinhos/spec.md)
- [FRONTEND-SPEC-05](../28-frontend-formularios-e-validacoes/spec.md)

## Regras e critérios de aceitação

- [x] A lista e o resumo só incluem a moeda selecionada; trocar a moeda atualiza tudo.
  > ✅ 2026-10-29 13:10 +00:00 — `money-boxes-page.spec.ts` (`trocar a moeda recarrega a lista e o resumo daquela moeda`: duas chamadas com a moeda EUR e o cofrinho BRL some). No navegador só foi exercitado o BRL; a troca para EUR não foi repetida lá.
- [x] Depósitos 1000 e 500 e saque 300 mostram saldo `R$ 1.200,00`.
  > ✅ 2026-10-29 13:10 +00:00 — Navegador contra a API real: aporte de R$ 1.500,00 (card mostrou `75% de R$ 2.000,00`) e saque de R$ 300,00 → saldo `R$ 1.200,00` no cartão, no detalhe e nos KPIs; o servidor tem o teste de 1000+500−300 (spec 19). Fica a ressalva: no navegador usei um aporte de 1500, não dois de 1000 e 500.
- [x] Com saldo 0 o botão `Saque` está desabilitado; um saque de 1.300 com saldo 1.200 é recusado com a mensagem de saldo insuficiente e o saldo continua 1.200.
  > ✅ 2026-10-29 13:10 +00:00 — `money-boxes-page.spec.ts` (`com saldo 0 o botão Saque fica desabilitado…`: `disabled`, `title`=`Saldo insuficiente` e texto `Saldo insuficiente para saque.`; saque acima do saldo mostra a mensagem no diálogo, nada fecha e o saldo continua) e no navegador (cofrinho novo: Saque desabilitado; com saldo 1.500, saque de 1.600 recusado com `Saldo insuficiente: o saque excede o saldo do cofrinho (ou o saldo é zero).` e saldo mantido).
- [x] Excluir um depósito que deixaria o saldo negativo é recusado e a movimentação permanece no histórico.
  > ✅ 2026-10-29 13:10 +00:00 — `money-boxes-page.spec.ts` (`o detalhe lista os aportes…`: `INSUFFICIENT_BALANCE` → toast `…deixaria o saldo negativo` e o histórico segue) e no navegador (excluir o depósito de 1.500 com saque de 300 → toast de recusa; saldo 1.200,00 e as duas movimentações permanecem).
- [x] A meta geral é independente da soma das metas individuais; sem meta mostra `—`.
  > ✅ 2026-10-29 13:10 +00:00 — No navegador: cofrinho com meta 2.000 e meta geral 4.000 → KPIs `Meta Total R$ 4.000,00`, `Progresso 30%`, barra e `30% concluído — R$ 1.200,00 de R$ 4.000,00`, enquanto o cartão mostra `60% de R$ 2.000,00`; sem meta os KPIs mostram `—` (também em `money-boxes-page.spec.ts`, que ainda cobre o envio de `0` para remover a meta).
- [x] Em cofrinho com movimentações, o campo Moeda está desabilitado com a mensagem de ajuda.
  > ✅ 2026-10-29 13:10 +00:00 — `money-boxes-page.spec.ts` (`em cofrinho com aportes o campo Moeda fica desabilitado…`: `select` desabilitado e a mensagem `Não é possível alterar a moeda de um cofrinho com aportes.`; sem aportes fica habilitado). A decisão vem de `listMoneyBoxEntries` ao abrir a edição (o contrato não traz contagem de movimentações); `CURRENCY_LOCKED` do servidor também aparece no diálogo. Não repetido no navegador.
- [x] Excluir cofrinho pede confirmação e remove também as movimentações.
  > ✅ 2026-10-29 13:10 +00:00 — `money-boxes-page.spec.ts` (`excluir o cofrinho pede confirmação com o nome…`: confirmação com “Reserva”, `DELETE` só depois do sim, toast `Cofrinho excluído.` e estado vazio) e `deletingTheBoxRemovesItsEntriesAndNothingElse` no servidor (remove as movimentações). Não repetido no navegador.
- [x] Movimentações não alteram os valores do Dashboard de saldo mensal (só o total de Reservas).
  > ✅ 2026-10-29 13:10 +00:00 — No navegador: com R$ 1.200,00 no cofrinho, o dashboard mostrou `Reservas R$ 1.200,00` e `Saldo`, `Entradas` e `Saídas` em `R$ 0,00`; no servidor, `reservesTotalAppearsOnTheDashboardWithoutChangingMonthlyBalance`.
- [ ] Estados de carregando, vazio, erro e sucesso; ações por teclado.
  > ⏳ 2026-10-29 13:10 +00:00 — Vitest: carregando, vazio (`Nenhum cofrinho ainda. Crie sua primeira reserva.` e `Nenhum aporte ainda.`), erro com `Tentar novamente`, toasts de sucesso (`Cofrinho salvo.`, `Aporte registrado.`, `Meta geral salva.`). No navegador: Enter envia os diálogos, o foco volta ao botão que abriu, Esc fecha o detalhe. Falta: percurso completo só por teclado, leitor de tela, tema claro e 320/768 px (F12).

### Skills de implementação

- **Skills obrigatórias:** `angular-module-scaffold`, `angular-tailwind-ui-specialist`, `angular-architect`.
- **Caminhos:** `.claude/skills/angular-module-scaffold/SKILL.md`; `.claude/skills/angular-tailwind-ui-specialist/SKILL.md`; `.claude/skills/angular-architect/SKILL.md`.
- **Responsabilidade:** módulo e `doc.md`; cartões, diálogos e barras; estado e formulários.
- **Momento de utilização:** depois dos contratos `money-boxes` e `reserves`.
- **Dependências:** FRONTEND-SPEC-00…05; BACKEND-SPEC-14.
- **Limitações:** nenhuma específica.

## Tasks

### Front-end

- [x] Criar `features/money-boxes` com página, cartão, detalhe, diálogos e serviço de dados.
  > ✅ 2026-10-29 13:10 +00:00 — `features/money-boxes` (`money-boxes.routes.ts`, `pages/money-boxes-page.ts`, `data-access/` com `MoneyBoxesApiService` e `MoneyBoxesStore`, `ui/` com cartão, detalhe e diálogos de cofrinho, movimentação e meta geral, `money-box-catalog.ts`, `doc.md`); tipos gerados em `core/api/generated/money-boxes.ts` e `reserves.ts`; rota `/cofrinhos` real no lugar do placeholder; reaproveita `KpiCard`, `Modal`, `ConfirmDialog` e `MoneyInputDirective`.

### Validação

- [ ] Testar saldos, moeda bloqueada, meta geral e exclusão; revisar UI.
  > ⏳ 2026-10-29 13:10 +00:00 — `npm test`: 186 testes Vitest verdes (15 novos em `money-boxes-page.spec.ts`), `ng build` verde e percurso no navegador (tema escuro, desktop) contra a API real: cofrinho criado, aporte, saque recusado e aceito, detalhe, exclusão recusada, meta geral e dashboard. Falta: tema claro, 320/768 px, teclado completo e leitor de tela (F12); EUR, edição com moeda bloqueada e exclusão do cofrinho só em Vitest.

## Resultado esperado

- Reservas completas e coerentes com a origem web, separadas do saldo mensal.
- Preservada a independência de moedas e de mês.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
