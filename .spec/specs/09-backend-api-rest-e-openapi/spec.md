# 09-backend-api-rest-e-openapi

> **ID:** BACKEND-SPEC-03 · **Categoria:** Backend · **Estado:** proposta para revisão — não aprovada, não executada.

## Objetivo

Descrever, por módulo, os recursos, parâmetros, corpos e respostas da API REST que substituem os providers e repositórios da origem, como base para o pedido de autoria dos contratos OpenAPI 3.1.

## Escopo

- Inclui: operações, parâmetros, campos, estados HTTP e erros por módulo, nos formatos de [GLOBAL-SPEC-03](../05-global-contratos-e-integracao/spec.md).
- Não inclui: escrever os YAML (exige a prévia e a aprovação do `openapi-rest-contract`) nem a implementação dos controllers.

## Contexto técnico local

Autenticação: todas as operações abaixo, exceto as de `identity`, exigem `Authorization: Bearer` e operam só sobre os dados do utilizador autenticado; recurso alheio → 404 (P-01, BACKEND-SPEC-05). O proprietário nunca é um parâmetro.

Convenções: dinheiro `"1234.56"` (string), datas `YYYY-MM-DD`, mês `YYYY-MM`, `currency` obrigatório em agregações, erros `application/problem+json` (catálogo em [BACKEND-SPEC-04](../10-backend-validacoes-erros-e-excecoes/spec.md)). Estados: 200/201/204, 400 validação, 404 inexistente, 409 conflito de negócio, 500.

### categories

| operationId | Entrada | Resposta | Erros |
|---|---|---|---|
| `listCategories` | `type?` (`INCOME`\|`EXPENSE`: devolve as que aceitam o tipo, incluindo `BOTH`) | 200 `Category[]` ordenadas por nome sem distinguir caixa | — |
| `createCategory` | `name`, `type`, `icon`, `color`, `subcategories?` (nomes; "Outros" é sempre criada) | 201 `Category` | 400, 409 `CATEGORY_NAME_DUPLICATED` |
| `getCategory` | path | 200 | 404 |
| `updateCategory` | `name`, `type`, `icon`, `color` | 200 | 400, 404, 409 duplicado, 409 `CATEGORY_TYPE_CONFLICT` (P-13) |
| `deleteCategory` | path | 204 | 404, 409 `CATEGORY_IN_USE` |
| `addSubcategory` | `name` | 201 `Subcategory` | 400, 404, 409 `SUBCATEGORY_NAME_DUPLICATED` (novo, DESC-SUB-02) |
| `deleteSubcategory` | paths | 204 (lançamentos vinculados ficam sem subcategoria — P-11) | 404, 409 `SUBCATEGORY_PROTECTED` |

`Category`: `id`, `name`, `type`, `icon`, `color` (`#RRGGBB`), `subcategories[{id, name, protected}]`.

### transactions

| operationId | Entrada | Resposta | Erros |
|---|---|---|---|
| `listTransactions` | `month`, `currency` (obrigatórios); `type` = `ALL`\|`INCOME`\|`EXPENSE` (padrão `ALL`); `page`, `size` | 200 `{items, page, size, totalItems, totalPages, totals{income, expense, balance}}`; `totals` cobre o conjunto filtrado inteiro (DESC-TXN-09); ordenação RN-TXN-08 | 400 |
| `createTransaction` | `kind`, `description`, `amount`, `currency`, `date`, `categoryId`, `subcategoryId?`, `notes?` | 201 `{transaction, budgetAlert?}` | 400, 404 categoria, 409 `CATEGORY_TYPE_MISMATCH`, `SUBCATEGORY_NOT_IN_CATEGORY` |
| `getTransaction` | path | 200 `Transaction` | 404 |
| `updateTransaction` | mesmo corpo | 200 `{transaction, budgetAlert?}` | idem |
| `deleteTransaction` | path | 204 | 404 |

`Transaction`: `id`, `kind`, `description`, `amount`, `currency`, `date`, `category{id,name,color,icon}`, `subcategory{id,name}?`, `notes?`, `createdAt`, `updatedAt`.
`budgetAlert` (só para despesas, ADR-005): `{scope: CATEGORY|GLOBAL, status: WARNING|EXCEEDED, categoryId?, categoryName?}`; o texto exibido é composto no frontend (DESC-BUD-07). `createTransaction` e `updateTransaction` recebem `referenceMonth` (`YYYY-MM`): o alerta só é avaliado se a data do lançamento pertence a esse mês (P-20, comportamento da origem).

### overview

| operationId | Entrada | Resposta |
|---|---|---|
| `getMonthlyOverview` | `month`, `currency` | 200 `{month, currency, summary{income, expense, balance}, previous{income, expense, balance}, incomeDeltaPercent?, expenseDeltaPercent?, reservesTotal, bars[{label, income, expense}], expenseByCategory[{category, amount, percentage}], latestTransactions[8]}` |

Agrega o que `home_screen_web.dart` monta com quatro providers (DESC-DSH-01) e, por decisão P-14, inclui `budgetProgress` (o progresso da meta global do mês: `{limit, spent, fraction, remaining, status}`, `status = NONE` sem meta).

### budgets

| operationId | Entrada | Resposta |
|---|---|---|
| `getBudget` | path `currency`; query `month` | 200 `{currency, monthlyLimit, month, monthOverride?, effectiveLimit, categoryLimits[{categoryId, limit}]}` |
| `saveBudget` | `monthlyLimit`, `categoryLimits[{categoryId, limit}]`, `monthOverride?{month, limit?, disabled?}` — `limit` define o valor do mês, `disabled=true` = "sem meta neste mês", ausente remove o ajuste (P-06) — gravação atómica (DESC-BUD-09) | 200 igual a `getBudget` |
| `getBudgetProgress` | `currency`, `month` | 200 `{global{limit, spent, fraction, remaining, status}, categories[{categoryId, limit, spent, fraction, remaining, status}]}` incluindo todas as categorias que aceitam despesa |

### money-boxes e reservas

| operationId | Entrada | Resposta | Erros |
|---|---|---|---|
| `listMoneyBoxes` | `currency` | 200 `MoneyBox[]` por data de criação, com `balance` e `progress` calculados | — |
| `createMoneyBox` | `name`, `description?`, `icon`, `color`, `goal?`, `currency` | 201 | 400 |
| `getMoneyBox` | path | 200 | 404 |
| `updateMoneyBox` | mesmos campos | 200 | 400, 404, 409 `CURRENCY_LOCKED` |
| `deleteMoneyBox` | path | 204 (remove movimentações) | 404 |
| `listMoneyBoxEntries` | path | 200 `MoneyBoxEntry[]` por data decrescente | 404 |
| `createMoneyBoxEntry` | `kind`, `amount`, `date`, `note?` (moeda = a do cofrinho) | 201 | 400, 404, 409 `CURRENCY_MISMATCH`, 409 `INSUFFICIENT_BALANCE` (saque com saldo ≤ 0 ou acima do saldo — P-04) |
| `updateMoneyBoxEntry` | `kind`, `amount`, `date`, `note?` | 200 | 400, 404, 409 `INSUFFICIENT_BALANCE` se o saldo ficaria negativo |
| `deleteMoneyBoxEntry` | paths | 204 | 404, 409 `INSUFFICIENT_BALANCE` se a exclusão deixaria o saldo negativo |
| `getReservesSummary` | `currency` | 200 `{total, goal, progress, boxCount}` | — |
| `saveReserveGoal` | path `currency`; `amount` (≥ 0) | 200 | 400 |

### reports

| operationId | Entrada | Resposta |
|---|---|---|
| `getReport` | `period` (`WEEK`\|`MONTH`\|`YEAR`), `anchorDate`, `currency`, `rankingSize?` (padrão 7, comportamento atual — P-10) | 200 `{period, range{start, end}, summary, bars[], expenseByCategory[], ranking[], monthlySummary?[]}`; `bars` e `summary` usam o **mesmo** intervalo (corrige DESC-REP-03); `monthlySummary` só para `YEAR` |

### identity (BACKEND-SPEC-05 e BACKEND-SPEC-18)

| operationId | Entrada | Resposta | Erros |
|---|---|---|---|
| `registerUser` | `email`, `password` | 201 `{user{id, email}, accessToken}` + cookie de renovação | 400, 409 `EMAIL_ALREADY_REGISTERED` |
| `login` | `email`, `password` | 200 `{user, accessToken}` + cookie | 401 `INVALID_CREDENTIALS`, 429 |
| `refreshToken` | cookie | 200 `{accessToken}` | 401 |
| `logout` | cookie | 204 | — |
| `getCurrentUser` | token | 200 `{id, email}` | 401 |
| `requestPasswordReset` | `email` | 202 sempre (resposta neutra) | 400 |
| `confirmPasswordReset` | `token`, `newPassword` | 204 | 400 `PASSWORD_RESET_TOKEN_INVALID`, 400 `VALIDATION_FAILED` |

### preferences (P-19: servidor, por utilizador)

`getPreferences` / `savePreferences`: `{currency, theme}`, padrões `BRL` e `SYSTEM`; valores inválidos → 400.

### backup

Removido do escopo (P-03, P-16): não há `exportBackup` nem `importBackup`.

### Autoria dos contratos

Layout, módulos e ficheiros dependem de P-08. Com a skill, cada módulo acima é um pedido separado (módulo, endpoints, parâmetros, esquemas, erros comuns) e exige aprovação da prévia antes de qualquer ficheiro ser criado.

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [GLOBAL-SPEC-03](../05-global-contratos-e-integracao/spec.md)
- [Scaffold](../01-scaffold-backend-spring-modulith/spec.md)

## Regras e critérios de aceitação

- [ ] Cada operação desta spec existe no OpenAPI com parâmetros, esquemas e respostas equivalentes.
- [ ] `listTransactions` devolve `totals` do conjunto filtrado completo, independentemente da página.
- [ ] Criar/atualizar despesa devolve `budgetAlert` apenas na transição de estado definida em RN-BUD-06/07.
- [ ] `getReport` devolve `bars` e `summary` do mesmo intervalo.
- [ ] Toda operação fora de `identity` sem token devolve 401 e com token de outro utilizador devolve 404 para recursos alheios.
- [ ] Nenhum esquema expõe `bank`, `balance` ou outros campos legados.

### Skills de implementação

- **Skills obrigatórias:** `openapi-rest-contract` (contratos), `java-spring-create-module` (controllers e adaptação).
- **Caminhos:** `C:\Users\dowgl\.codex\skills\openapi-rest-contract\SKILL.md`; `C:\Users\dowgl\.codex\skills\java-spring-create-module\SKILL.md`.
- **Responsabilidade:** a primeira escreve/atualiza os YAML por módulo com Maven Generator isolado; a segunda implementa as operações selecionadas.
- **Momento de utilização:** contratos antes dos módulos; implementação por operação ou recurso escolhido.
- **Dependências:** P-07, P-08 (decididas); specs 10, 11 e 15–20, 22.
- **Limitações:** o scaffold atual usa um único `openapi/openapi.yaml`; a skill de contratos preserva o layout antigo ao continuar nele e só cria o layout por módulo para módulos claramente novos — ver P-08.

## Tasks

### Descoberta e decisão

- [ ] Confirmar o formato JSON do dinheiro (string decimal) antes de redigir os contratos; P-07, P-08, P-10 e P-14 já estão decididas.

### Contrato e dados

- [ ] Preparar um pedido por módulo ao `openapi-rest-contract`, receber a prévia, obter aprovação e gerar.
- [ ] Validar os contratos (OpenAPI 3.1, `$ref`, duplicados) e a geração Maven.

### Back-end

- [ ] Implementar os controllers por operação com `java-spring-create-module`.

### Validação

- [ ] Executar `mvnw clean verify` e os testes de contrato (QUALITY-SPEC-03).

## Resultado esperado

- API versionada, tipada e documentada por contratos, coerente com as regras de domínio.
- Preservada a regra de que o contrato é a única fonte de verdade.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
