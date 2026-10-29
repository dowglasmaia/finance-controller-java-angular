# 19-backend-modulo-cofrinhos

> **ID:** BACKEND-SPEC-14 · **Categoria:** Backend (módulo `moneybox`) · **Estado:** executada na F8 (2026-10-29); comparação literal com `money_box_service_test` pendente — ver evidências.

## Objetivo

Implementar cofrinhos (reservas), as suas movimentações de depósito e saque, o resumo de reservas e a meta geral por moeda, preservando o saldo derivado e a separação do saldo mensal.

## Escopo

- Inclui: `listMoneyBoxes`, `createMoneyBox`, `getMoneyBox`, `updateMoneyBox`, `deleteMoneyBox`, `listMoneyBoxEntries`, `createMoneyBoxEntry`, `updateMoneyBoxEntry`, `deleteMoneyBoxEntry`, `getReservesSummary`, `saveReserveGoal`.
- Não inclui: lançamentos mensais, interface (FRONTEND-SPEC-12), importação de legado (BACKEND-SPEC-16).

## Contexto técnico local

### Regras aplicadas

RN-BOX-01…10, RN-CUR-02. Evidências: DESC-BOX-01…13. Resumo:

- Saldo, progresso e totais calculados na leitura; nenhum valor derivado persistido.
- Movimentação herda a moeda do cofrinho; o cliente não escolhe moeda (a origem já usava a do cofrinho ao criar — `_EntryFormState._save`).
- Moeda do cofrinho só muda sem movimentações (`CURRENCY_LOCKED`).
- Excluir cofrinho remove movimentações numa transação (hoje duas operações — DESC-BOX-08).
- Meta geral por moeda (`reserve_goal`), independente da soma das metas individuais; progresso `min(total/meta, 1)`; vazio = 0 = sem meta.
- **Saldo nunca negativo (P-04, decidida):** saque recusado se o saldo for `≤ 0` ou se o valor exceder o saldo (`INSUFFICIENT_BALANCE`). Editar uma movimentação ou excluí-la também é recusado se, em qualquer ponto do histórico, o saldo ficaria negativo (por exemplo, apagar um depósito já "gasto" por saques posteriores). O histórico de movimentações é mantido. A origem permitia saldo negativo (DESC-BOX-06); esta é uma mudança deliberada.
- Verificação e gravação do saque na mesma transação, com bloqueio do cofrinho (BACKEND-SPEC-02), para que dois saques simultâneos não excedam o saldo.

### Edição de movimentação (DESC-BOX-11)

Permite alterar tipo (depósito↔saque), valor, data e nota; não permite trocar cofrinho nem moeda.

### Exibição

- `listMoneyBoxes` ordena por data de criação crescente e só devolve a moeda pedida.
- Entradas ordenadas por data decrescente.
- Nota padrão exibida pelo cliente: `Saque` ou `Depósito` quando vazia.

### Pendências e riscos

Meta individual `≥ 0` (P-21; a origem aceitava texto negativo — DESC-BOX-07). Como não há migração de dados (P-16), não existem saldos negativos prévios a conciliar.

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [BACKEND-SPEC-01](../07-backend-dominio-e-regras-de-negocio/spec.md)
- [BACKEND-SPEC-03](../09-backend-api-rest-e-openapi/spec.md)

## Regras e critérios de aceitação

- [x] Depósitos 1000 e 500 e saque 300 → `balance` 1200; BRL e EUR somam separados (750 / 400 no exemplo da origem).
  > ✅ 2026-10-29 13:10 +00:00 — `MoneyBoxFlowIntegrationTest.balanceIsDepositsMinusWithdrawalsAndCurrenciesAreSeparate` (PostgreSQL: depósitos 1000 e 500 e saque 300 → `balance` 1200.00, `progress` 0.6000 com meta 2000; cofrinho EUR com 400.00 não se mistura; totais por moeda 1200.00 e 400.00) e `MoneyBoxDomainTests.balanceIsDepositsMinusWithdrawals`.
- [x] Criar movimentação EUR em cofrinho BRL é impossível (moeda herdada); informar moeda divergente → 409 `CURRENCY_MISMATCH`.
  > ✅ 2026-10-29 13:10 +00:00 — `currencyIsInheritedAndMismatchOrChangeWithEntriesIsRefused`: `currency` EUR em cofrinho BRL → 409 `CURRENCY_MISMATCH`; sem `currency` a movimentação herda a do cofrinho (a coluna de moeda nem existe em `money_box_entry`).
- [x] Atualizar `currency` de cofrinho com movimentações → 409 `CURRENCY_LOCKED`; sem movimentações → 200.
  > ✅ 2026-10-29 13:10 +00:00 — mesmo teste: sem movimentações a moeda muda (200); com uma movimentação → 409 `CURRENCY_LOCKED`; renomear mantendo a moeda continua 200 e preserva o saldo.
- [x] Excluir cofrinho remove movimentações e falha por completo se qualquer remoção falhar.
  > ✅ 2026-10-29 13:10 +00:00 — `deletingTheBoxRemovesItsEntriesAndNothingElse`: 204; cofrinho e lista de movimentações → 404; `SELECT count(*) FROM money_box_entry` do cofrinho = 0; o outro cofrinho e o total de reservas (5.00) intactos. A atomicidade vem de uma única transação (`@Transactional` em `DeleteMoneyBoxUseCase`: remove as movimentações e depois o cofrinho, mais `ON DELETE CASCADE` na V5); não foi injetada falha para provar o rollback.
- [x] Saque com saldo 0, ou com valor maior que o saldo, devolve 409 `INSUFFICIENT_BALANCE` e nada é gravado; saque igual ao saldo é aceito e deixa 0.
  > ✅ 2026-10-29 13:10 +00:00 — `withdrawalAboveBalanceOrWithZeroBalanceIsRefusedAndNothingIsSaved`: saque com saldo 0 → 409 `INSUFFICIENT_BALANCE`; com saldo 1200, saque de 1300 → 409 e a lista continua com 1 movimentação e saldo 1200.00; saque de 1200 (igual ao saldo) → 201 e saldo 0.00. `MoneyBoxDomainTests` cobre o limite (100.00 ok, 100.01 recusado).
- [x] Excluir ou reduzir um depósito que deixaria o saldo negativo (por saques posteriores) devolve 409 `INSUFFICIENT_BALANCE`.
  > ✅ 2026-10-29 13:10 +00:00 — `editOrDeleteThatWouldMakeTheBalanceNegativeIsRefusedAndHistoryIsKept`: com depósito 100 e saque 80, excluir o depósito, reduzi-lo a 50 e transformá-lo em saque → 409 `INSUFFICIENT_BALANCE`, saldo continua 20.00; editar o saque para 30 (nota `Ajuste`) → 200 e saldo 70.00; excluir o saque → 204 e saldo 100.00. `MoneyBoxDomainTests.negativeAtAnyPointOfTheHistoryIsRefusedEvenIfTheFinalBalanceIsPositive` cobre o ponto intermediário do histórico.
- [x] Dois saques simultâneos que juntos excederiam o saldo: só um é aceito.
  > ✅ 2026-10-29 13:10 +00:00 — `simultaneousWithdrawalsNeverExceedTheBalance`: 8 requisições simultâneas de saque de 30.00 sobre saldo 100.00 → exatamente 3 aceitas (201) e 5 recusadas (409); saldo final 10.00. A serialização vem do bloqueio da linha do cofrinho (`SELECT … FOR UPDATE`, `findForUpdate`) na mesma transação da verificação e da gravação.
- [x] Um utilizador não vê nem altera cofrinhos de outro (404).
  > ✅ 2026-10-29 13:10 +00:00 — `anotherUserSeesAndChangesNothing`: outro utilizador recebe 404 em obter, listar e criar movimentação, atualizar/excluir cofrinho, atualizar/excluir movimentação; a lista dele é `[]` e o total de reservas `0.00`; o saldo do dono não muda.
- [x] `getReservesSummary` não depende de mês e só soma a moeda pedida; `progress` = `min(total/meta, 1)`, 0 sem meta.
  > ✅ 2026-10-29 13:10 +00:00 — `generalGoalIsIndependentAndZeroRemovesIt`: o resumo não tem parâmetro de mês (contrato `reserves`); com cofrinho de meta 100 e total 250, a meta geral 1000 dá `progress` 0.2500 e a 200.00 dá 1.0000 (limitado a 1); sem meta `goal` ausente e `progress` 0.0000; EUR soma separado (`0.00`). Soma por moeda também em `balanceIsDepositsMinusWithdrawalsAndCurrenciesAreSeparate`.
- [x] `saveReserveGoal` com valor negativo → 400; 0 remove a meta.
  > ✅ 2026-10-29 13:10 +00:00 — `invalidInputsAre400` (`-1` e corpo vazio → 400) e `generalGoalIsIndependentAndZeroRemovesIt` (`0` remove: `goal` ausente e nenhuma linha em `reserve_goal`).
- [x] Movimentações não criam lançamentos nem alteram o overview mensal.
  > ✅ 2026-10-29 13:10 +00:00 — `reservesTotalAppearsOnTheDashboardWithoutChangingMonthlyBalance`: depósito 700 e saque 200 → `reservesTotal` 500.00 no `overview`, enquanto `summary.income/expense/balance` seguem 0.00; EUR 0.00. Fecha a pendência da F7 (`NoReservesPort` removida; `MoneyBoxReservesPort` usa `ReservesQuery`).

### Cenários de teste

Saldos mistos; saque excedente e com saldo zero; edição/exclusão que negativaria; concorrência de saques; moeda bloqueada; cascata atómica; meta individual 0 e positiva; meta geral ≠ soma das individuais; ordem das listagens; edição trocando tipo.

### Skills de implementação

- **Skills obrigatórias:** `java-modern-engineering`, `java-spring-create-module`, `java-persistence-performance`; `openapi-rest-contract` para os contratos `money-boxes` e `reserves`.
- **Caminhos:** `C:\Users\dowgl\.codex\skills\java-modern-engineering\SKILL.md`; `C:\Users\dowgl\.codex\skills\java-spring-create-module\SKILL.md`; `C:\Users\dowgl\.codex\skills\openapi-rest-contract\SKILL.md`; `.claude/skills/java-persistence-performance/SKILL.md`.
- **Responsabilidade:** contrato, domínio de saldo/progresso, adaptadores.
- **Momento de utilização:** módulo independente; pode ser implementado em paralelo com `budget`.
- **Dependências:** BACKEND-SPEC-00…05.
- **Limitações:** nenhuma específica.

## Tasks

### Contrato e dados

- [x] Prévia aprovada e contratos; migrações `money_box`, `money_box_entry`, `reserve_goal`.
  > ✅ 2026-10-29 13:10 +00:00 — Prévia aprovada pelo utilizador em conversa (11 operações, schemas e 7 decisões: caminhos `/money-boxes…` e `/reserves/{currency}/summary|goal`, `saveReserveGoal` devolve o resumo, ordem do histórico por data e criação, bloqueio da linha do cofrinho, datas 2000–2100 sem limite de futuro, V5, `reservesTotal` real) e gerada em `openapi/modules/money-boxes.yaml` e `reserves.yaml` + execuções `generate-money-boxes-api` e `generate-reserves-api`; `generate-sources` sem `operationId` repetido. V5 cria `money_box`, `money_box_entry` (FK `ON DELETE CASCADE`) e `reserve_goal` (`IdentityMigrationTests` atualizado: versão 5 e as 11 tabelas). Desvio: YAML escrito à mão.

### Back-end

- [x] Implementar domínio, casos de uso, adaptadores e controllers das 11 operações.
  > ✅ 2026-10-29 13:10 +00:00 — Módulo `moneybox`: domínio (`MoneyBox`, `MoneyBoxEntry`, `Ledger`, `Amounts`), 11 casos de uso (`List/Create/Get/Update/DeleteMoneyBox`, `List/Create/Update/DeleteMoneyBoxEntry`, `GetReservesSummary`, `SaveReserveGoal`), portas `MoneyBoxRepository` e `ReserveGoalRepository`, adaptadores JPA com filtro por `owner_id` em toda consulta, `MoneyBoxesController` e `ReservesController`, API pública `ReservesQuery`. A lista usa 2 comandos SQL (cofrinhos + saldos agrupados) qualquer que seja o número de cofrinhos. `./mvnw clean verify` com Docker: 321 testes, 0 falhas (21 novos).

### Validação

- [ ] Executar os cenários de teste e comparar com `money_box_service_test`.
  > ⏳ 2026-10-29 13:10 +00:00 — Cenários da spec executados em PostgreSQL real (lista acima) e `ModularityTests` verde (`report → moneybox` só pela API pública `ReservesQuery`). Falta: comparar caso a caso com `money_box_service_test`, que é do projeto de origem e não está no repositório; os cenários vieram de `descobertas.md` (DESC-BOX-*) e da spec.

## Resultado esperado

- Reservas equivalentes à origem, com atomicidade e integridade garantidas pelo servidor.
- Preservado: saldo derivado, independência do mês e separação de moedas.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
