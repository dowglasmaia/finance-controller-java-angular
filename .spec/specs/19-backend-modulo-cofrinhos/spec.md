# 19-backend-modulo-cofrinhos

> **ID:** BACKEND-SPEC-14 · **Categoria:** Backend (módulo `moneybox`) · **Estado:** proposta para revisão — não aprovada, não executada.

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

- [ ] Depósitos 1000 e 500 e saque 300 → `balance` 1200; BRL e EUR somam separados (750 / 400 no exemplo da origem).
- [ ] Criar movimentação EUR em cofrinho BRL é impossível (moeda herdada); informar moeda divergente → 409 `CURRENCY_MISMATCH`.
- [ ] Atualizar `currency` de cofrinho com movimentações → 409 `CURRENCY_LOCKED`; sem movimentações → 200.
- [ ] Excluir cofrinho remove movimentações e falha por completo se qualquer remoção falhar.
- [ ] Saque com saldo 0, ou com valor maior que o saldo, devolve 409 `INSUFFICIENT_BALANCE` e nada é gravado; saque igual ao saldo é aceito e deixa 0.
- [ ] Excluir ou reduzir um depósito que deixaria o saldo negativo (por saques posteriores) devolve 409 `INSUFFICIENT_BALANCE`.
- [ ] Dois saques simultâneos que juntos excederiam o saldo: só um é aceito.
- [ ] Um utilizador não vê nem altera cofrinhos de outro (404).
- [ ] `getReservesSummary` não depende de mês e só soma a moeda pedida; `progress` = `min(total/meta, 1)`, 0 sem meta.
- [ ] `saveReserveGoal` com valor negativo → 400; 0 remove a meta.
- [ ] Movimentações não criam lançamentos nem alteram o overview mensal.

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

- [ ] Prévia aprovada e contratos; migrações `money_box`, `money_box_entry`, `reserve_goal`.

### Back-end

- [ ] Implementar domínio, casos de uso, adaptadores e controllers das 11 operações.

### Validação

- [ ] Executar os cenários de teste e comparar com `money_box_service_test`.

## Resultado esperado

- Reservas equivalentes à origem, com atomicidade e integridade garantidas pelo servidor.
- Preservado: saldo derivado, independência do mês e separação de moedas.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
