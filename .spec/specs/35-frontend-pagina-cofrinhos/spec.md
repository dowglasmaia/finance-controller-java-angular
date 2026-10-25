# 35-frontend-pagina-cofrinhos

> **ID:** FRONTEND-SPEC-12 · **Categoria:** Frontend (`features/money-boxes`) · **Estado:** proposta para revisão — não aprovada, não executada.

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

- [ ] A lista e o resumo só incluem a moeda selecionada; trocar a moeda atualiza tudo.
- [ ] Depósitos 1000 e 500 e saque 300 mostram saldo `R$ 1.200,00`.
- [ ] Com saldo 0 o botão `Saque` está desabilitado; um saque de 1.300 com saldo 1.200 é recusado com a mensagem de saldo insuficiente e o saldo continua 1.200.
- [ ] Excluir um depósito que deixaria o saldo negativo é recusado e a movimentação permanece no histórico.
- [ ] A meta geral é independente da soma das metas individuais; sem meta mostra `—`.
- [ ] Em cofrinho com movimentações, o campo Moeda está desabilitado com a mensagem de ajuda.
- [ ] Excluir cofrinho pede confirmação e remove também as movimentações.
- [ ] Movimentações não alteram os valores do Dashboard de saldo mensal (só o total de Reservas).
- [ ] Estados de carregando, vazio, erro e sucesso; ações por teclado.

### Skills de implementação

- **Skills obrigatórias:** `angular-module-scaffold`, `angular-tailwind-ui-specialist`, `angular-architect`.
- **Caminhos:** `.claude/skills/angular-module-scaffold/SKILL.md`; `.claude/skills/angular-tailwind-ui-specialist/SKILL.md`; `.claude/skills/angular-architect/SKILL.md`.
- **Responsabilidade:** módulo e `doc.md`; cartões, diálogos e barras; estado e formulários.
- **Momento de utilização:** depois dos contratos `money-boxes` e `reserves`.
- **Dependências:** FRONTEND-SPEC-00…05; BACKEND-SPEC-14.
- **Limitações:** nenhuma específica.

## Tasks

### Front-end

- [ ] Criar `features/money-boxes` com página, cartão, detalhe, diálogos e serviço de dados.

### Validação

- [ ] Testar saldos, moeda bloqueada, meta geral e exclusão; revisar UI.

## Resultado esperado

- Reservas completas e coerentes com a origem web, separadas do saldo mensal.
- Preservada a independência de moedas e de mês.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
