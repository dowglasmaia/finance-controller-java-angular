# 16-backend-modulo-lancamentos

> **ID:** BACKEND-SPEC-11 · **Categoria:** Backend (módulo `transaction`) · **Estado:** executada na F5 (2026-10-29); o cálculo real do alerta de meta fica para a F6 (porta `BudgetImpactPort` com implementação provisória) — ver evidências.

## Objetivo

Implementar o módulo de lançamentos (receitas e despesas): criação, listagem mensal por moeda, edição e exclusão, com o alerta de meta devolvido ao salvar despesas.

## Escopo

- Inclui: `listTransactions`, `createTransaction`, `getTransaction`, `updateTransaction`, `deleteTransaction`; API pública de consulta de despesas; `CategoryUsageGuard` e `BudgetImpactPort`.
- Não inclui: cálculo de metas (BACKEND-SPEC-13), agregados do dashboard (BACKEND-SPEC-12), interface (FRONTEND-SPEC-10).

## Contexto técnico local

### Regras aplicadas

RN-TXN-01…09, RN-MES-01, RN-SAL-01, RN-CUR-01/02, RN-VAL-01/02. Evidências: DESC-TXN-01…11, DESC-MES-02, DESC-CUR-04.

### Fluxos

- **Criar/atualizar:** valida formato → carrega categoria e subcategoria → confere tipo (`CATEGORY_TYPE_MISMATCH`) e pertinência (`SUBCATEGORY_NOT_IN_CATEGORY`) → grava → se `kind=EXPENSE` e a data pertence ao `referenceMonth` recebido, chama `BudgetImpactPort` com o gasto antes/depois desse mês → devolve `{transaction, budgetAlert?}`; fora desse mês, sem alerta (P-20).
- **Listar:** `month` + `currency` obrigatórios; filtro `type`; ordenação RN-TXN-08; paginação (15 por padrão, DESC-TXN-09); `totals` do conjunto filtrado completo.
- **Excluir:** remove sem verificar dependências; o gasto da meta recalcula-se por consulta.
- **Moeda:** cada moeda tem os seus lançamentos, despesas e dashboard. A troca da moeda selecionada ao salvar um lançamento noutra moeda é comportamento do cliente (DESC-CUR-04; P-15); o servidor apenas guarda cada lançamento na moeda informada.

### Casos limite

- Data fora do `referenceMonth` do cliente: nenhum alerta e o progresso do mês de referência não muda (P-20, comportamento da origem).
- Lançamento sem subcategoria, com `notes` de espaços: `notes` ausente.
- Valores: `0`, `-1`, `1.234,56` com formato errado na API → `VALIDATION_FAILED` (a API só aceita `"1234.56"`).
- Edição que muda de despesa para receita: alerta calculado só se o resultado é despesa.

### Riscos

O cálculo de "antes" na edição precisa excluir o valor antigo do próprio lançamento; testar explicitamente (DESC-BUD-07 só descreve o caminho de criação com clareza).

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [BACKEND-SPEC-01](../07-backend-dominio-e-regras-de-negocio/spec.md)
- [BACKEND-SPEC-03](../09-backend-api-rest-e-openapi/spec.md)
- [BACKEND-SPEC-10](../15-backend-modulo-categorias/spec.md)

## Regras e critérios de aceitação

- [x] Criar lançamento com `amount` `0` ou negativo → 400 `VALIDATION_FAILED` com mensagem `O valor deve ser maior que zero`.
  > ✅ 2026-10-29 10:50 +00:00 — `validationErrorsAre400WithFieldMessages` (HTTP, PostgreSQL): `0` → 400 `VALIDATION_FAILED` com `errors[0]` = `amount` / `O valor deve ser maior que zero`; `-1`, `1.234,56`, `abc` e `10000000000.00` → 400. `TransactionTests` cobre os limites no domínio e o CHECK do banco (`amount > 0` e máximo `9999999999.99`) repete a regra.
- [x] Categoria `EXPENSE` num lançamento `INCOME` → 409 `CATEGORY_TYPE_MISMATCH`; categoria `BOTH` aceita ambos.
  > ✅ 2026-10-29 10:50 +00:00 — `categoryMustAcceptTheKindAndSubcategoryMustBelongToIt` (409 `CATEGORY_TYPE_MISMATCH`; `SUBCATEGORY_NOT_IN_CATEGORY`; categoria `BOTH` aceita entrada e saída).
- [x] `listTransactions` de um mês devolve só lançamentos desse mês e moeda; `totals.balance` = entradas − saídas do conjunto filtrado completo, não da página.
  > ✅ 2026-10-29 10:50 +00:00 — `listIsScopedToMonthAndCurrencyOrderedWithTotalsOfTheWholeSetAndPaginated` (18 lançamentos, 15 por página; totais de entradas 500,00, saídas 170,00 e saldo 330,00 do conjunto inteiro, sem EUR nem setembro; filtro `INCOME`; mês vazio com 1 página e saldo `0.00`) e `sameDateTransactionsKeepTheMostRecentlyCreatedFirst` (RN-TXN-08).
- [x] Data futura é aceita; datas fora de 2000-01-01…2100-12-31 são rejeitadas.
  > ✅ 2026-10-29 10:50 +00:00 — `futureDatesAreAccepted` (2099-06-01 → 201); `1999-12-31` e `2101-01-01` → 400; `TransactionTests.dateRange…` cobre as bordas 2000-01-01 e 2100-12-31; CHECK `ck_transaction_entry_date` no banco.
- [x] Editar preserva `id` e `createdAt` e atualiza `updatedAt`.
  > ✅ 2026-10-29 10:50 +00:00 — `editPreservesIdAndCreatedAtAndUpdatesTheRest` (PostgreSQL: mesmo `id` e `createdAt`, `updatedAt` diferente; o lançamento muda de BRL/outubro para EUR/novembro). Correção feita ao verificar: o `Instant` é truncado a microssegundos (precisão do PostgreSQL), senão o `createdAt` devolvido ao criar diferia do lido depois.
- [ ] Despesa que leva o gasto de 700 para 850 com meta 1000 devolve `budgetAlert` `WARNING`; de 850 para 1100, `EXCEEDED`; nova despesa já estourada não devolve alerta.
  > ⏳ 2026-10-29 10:50 +00:00 — Parcial: `TransactionUseCaseTests.createPassesSpentBeforeAndAfterAndReturnsTheAlert` e `editDoesNotCountTheOldValueTwice` provam que `BudgetImpactPort` recebe o gasto antes e depois (categoria e mês, só da moeda e do proprietário, sem contar o valor antigo na edição) e que o alerta devolvido chega na resposta. Falta: os limiares 80%/100% e "já estourada não alerta" — são regra do módulo de metas (F6); hoje `NoBudgetImpactPort` nunca alerta. Próxima ação: na F6, implementar a porta e testar 700→850 `WARNING`, 850→1100 `EXCEEDED` e despesa já estourada.
- [x] Receita nunca devolve `budgetAlert`; despesa datada fora do `referenceMonth` também não.
  > ✅ 2026-10-29 10:50 +00:00 — `alertIsOnlyEvaluatedForExpensesInTheReferenceMonth` e `editingToIncomeNeverEvaluatesAnAlert`: receita, despesa fora do `referenceMonth` e despesa sem `referenceMonth` nem chegam a consultar a porta.
- [x] Um utilizador não lê nem altera lançamentos de outro, nem usa categoria de outro (404).
  > ✅ 2026-10-29 10:50 +00:00 — `usersNeverSeeOrChangeEachOthersTransactionsOrUseEachOthersCategories` (PostgreSQL: get/put/delete de lançamento alheio → 404, lista vazia, categoria alheia → 404, nada alterado) e `TransactionUseCaseTests.anotherUsers…`.

### Cenários de teste

Validação de entrada; compatibilidade de categoria/subcategoria; paginação e totais; ordenação e desempate; edição e alerta; exclusão; concorrência de duas despesas simultâneas no limiar; isolamento de moeda (EUR não entra em BRL).

### Skills de implementação

- **Skills obrigatórias:** `java-modern-engineering`, `java-spring-create-module`, `java-persistence-performance`; `openapi-rest-contract` para o contrato `transactions`.
- **Caminhos:** `C:\Users\dowgl\.codex\skills\java-modern-engineering\SKILL.md`; `C:\Users\dowgl\.codex\skills\java-spring-create-module\SKILL.md`; `C:\Users\dowgl\.codex\skills\openapi-rest-contract\SKILL.md`; `.claude/skills/java-persistence-performance/SKILL.md`.
- **Responsabilidade:** contrato, implementação das 5 operações, domínio do lançamento.
- **Momento de utilização:** depois do módulo `category`.
- **Dependências:** BACKEND-SPEC-05, 10; BACKEND-SPEC-13 para o alerta (pode usar porta falsa até existir).
- **Limitações:** nenhuma específica.

## Tasks

### Contrato e dados

- [x] Prévia aprovada e contrato `transactions`; migração `transaction_entry`.
  > ✅ 2026-10-29 10:50 +00:00 — Prévia aprovada pelo utilizador em conversa (5 operações, schemas, erros e as 4 decisões de contrato: `page` a partir de 1, `referenceMonth` no corpo, limites do valor, alerta ausente até a F6) e gerada em `openapi/modules/transactions.yaml` + execução `generate-transactions-api`. `validate.mjs` válido; `mvn generate-sources` gerou `TransactionsApi` e modelos; sem `operationId` repetido. `V3__transaction_entry.sql` (`numeric(12,2)`, CHECKs, FKs, `ON DELETE SET NULL` da subcategoria, índice de listagem `(owner_id, currency, entry_date DESC, created_at DESC, id DESC)`) aplicada do zero (`IdentityMigrationTests`) e sobre o banco local já com a V2. Desvio: o YAML foi escrito à mão (a skill não aceita `$ref` para schemas locais).

### Back-end

- [x] Implementar domínio, casos de uso, adaptadores, controller e a API de consulta de despesas.
  > ✅ 2026-10-29 10:50 +00:00 — Pacote `transaction`: domínio (`Transaction`, `Totals`, `BudgetAlert`), 5 casos de uso, adaptador JPA com filtro por `owner_id` e moeda, `TransactionsController`, `ExpenseQuery` (API pública para F6/F7/F9), `BudgetImpactPort` e `TransactionCategoryUsageGuard`. Em `category`: API pública de leitura `CategoryDirectory`. Em `shared`: `FieldValidationException` (erro de campo vindo do domínio, com mensagem pt-BR em `errors[]`) e `Currency`. `ModularityTests` verde. Desvio: `spring.jackson.default-property-inclusion=non_null` para que campos opcionais do contrato fiquem ausentes (não `null`) nas respostas.

### Validação

- [ ] Testes de validação, listagem, totais e alertas com PostgreSQL real.
  > ⏳ 2026-10-29 10:50 +00:00 — `./mvnw clean verify` com Docker: 214 testes, 0 falhas (`TransactionFlowIntegrationTest` 13, `TransactionUseCaseTests` 11, `TransactionTests` 9). Verificação na API real com PostgreSQL 18.1 pela interface. Falta apenas o cálculo real do alerta e o teste de concorrência no limiar (duas despesas simultâneas), que pertencem à F6.

## Resultado esperado

- Lançamentos equivalentes à origem, com validação completa no servidor e alerta de meta na resposta.
- Preservado o isolamento por moeda e o saldo derivado.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
