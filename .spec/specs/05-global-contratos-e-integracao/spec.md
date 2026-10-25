# 05-global-contratos-e-integracao

> **ID:** GLOBAL-SPEC-03 · **Categoria:** Global · **Estado:** proposta para revisão — não aprovada, não executada.

## Objetivo

Estabelecer as convenções de contrato e integração entre frontend e backend e o índice das operações, para que ambos os lados implementem a mesma interface a partir de uma única fonte (OpenAPI).

## Escopo

- Inclui: convenções REST/JSON, formatos de dinheiro, data, mês e moeda, paginação, erros, versionamento e índice de operações.
- Não inclui: esquemas campo a campo (BACKEND-SPEC-03), consumo no cliente (FRONTEND-SPEC-03), autoria dos YAML (futura execução com `openapi-rest-contract`).

## Contexto técnico local

### Convenções (PROPOSTO)

| Tema | Regra |
|---|---|
| Base e versão | `/api/v1`; mudança incompatível cria `/api/v2`. |
| JSON | `camelCase`; enumerações em `MAIÚSCULAS_COM_SUBLINHADO`. |
| Dinheiro | **String decimal** com ponto e 2 casas (`"1234.56"`), mais `currency` ao lado. Evita perda de precisão no TypeScript (ADR-002; P-07). |
| Data | `YYYY-MM-DD` (`LocalDate`). Instantes de auditoria em ISO-8601 UTC. |
| Mês | `YYYY-MM`. |
| Moeda | Parâmetro obrigatório `currency` (`BRL`\|`EUR`) em toda consulta agregada. |
| Paginação | `page` (base 0) e `size` (padrão 15, máximo 100) na lista de lançamentos; resposta com `items`, `page`, `size`, `totalItems`, `totalPages`. |
| Erros | `application/problem+json` (RFC 9457): `type`, `title`, `status`, `detail`, `code`, `errors[{field, message}]`. |
| Idempotência | `PUT`/`DELETE` idempotentes; `POST` não. |
| Tempo real | Não aplicável; sem WebSocket/SSE. |
| CORS | Origem do frontend configurada por ambiente; em desenvolvimento, proxy do Angular (fora do scaffold atual). |

### Índice de operações (PROPOSTO)

| Módulo | `operationId` | Método e caminho |
|---|---|---|
| categories | `listCategories` | GET `/categories` |
| | `createCategory` | POST `/categories` |
| | `getCategory` | GET `/categories/{categoryId}` |
| | `updateCategory` | PUT `/categories/{categoryId}` |
| | `deleteCategory` | DELETE `/categories/{categoryId}` |
| | `addSubcategory` | POST `/categories/{categoryId}/subcategories` |
| | `deleteSubcategory` | DELETE `/categories/{categoryId}/subcategories/{subcategoryId}` |
| transactions | `listTransactions` | GET `/transactions` |
| | `createTransaction` | POST `/transactions` |
| | `getTransaction` | GET `/transactions/{transactionId}` |
| | `updateTransaction` | PUT `/transactions/{transactionId}` |
| | `deleteTransaction` | DELETE `/transactions/{transactionId}` |
| overview | `getMonthlyOverview` | GET `/overview/monthly` |
| budgets | `getBudget` | GET `/budgets/{currency}` |
| | `saveBudget` | PUT `/budgets/{currency}` |
| | `getBudgetProgress` | GET `/budgets/{currency}/progress` |
| money-boxes | `listMoneyBoxes` | GET `/money-boxes` |
| | `createMoneyBox` | POST `/money-boxes` |
| | `getMoneyBox` | GET `/money-boxes/{moneyBoxId}` |
| | `updateMoneyBox` | PUT `/money-boxes/{moneyBoxId}` |
| | `deleteMoneyBox` | DELETE `/money-boxes/{moneyBoxId}` |
| | `listMoneyBoxEntries` | GET `/money-boxes/{moneyBoxId}/entries` |
| | `createMoneyBoxEntry` | POST `/money-boxes/{moneyBoxId}/entries` |
| | `updateMoneyBoxEntry` | PUT `/money-boxes/{moneyBoxId}/entries/{entryId}` |
| | `deleteMoneyBoxEntry` | DELETE `/money-boxes/{moneyBoxId}/entries/{entryId}` |
| | `getReservesSummary` | GET `/reserves/summary` |
| | `saveReserveGoal` | PUT `/reserves/goals/{currency}` |
| reports | `getReport` | GET `/reports` |
| identity | `registerUser`, `login`, `refreshToken`, `logout`, `getCurrentUser` | POST `/auth/register`, POST `/auth/login`, POST `/auth/refresh`, POST `/auth/logout`, GET `/auth/me` |
| | `requestPasswordReset`, `confirmPasswordReset` | POST `/auth/password-reset/request`, POST `/auth/password-reset/confirm` (públicas; BACKEND-SPEC-18) |
| preferences | `getPreferences`, `savePreferences` | GET/PUT `/preferences` (P-19: servidor, por utilizador) |

Não há módulo de backup (P-03). Todas as rotas, exceto `registerUser`, `login`, `refreshToken`, `requestPasswordReset` e `confirmPasswordReset`, exigem `Authorization: Bearer` e operam só sobre os dados do utilizador autenticado (P-01).

Cada módulo tem um contrato independente (ADR-004, aprovada por P-08); o índice acima é a base do pedido ao `openapi-rest-contract`.

### Integração frontend/backend

- O frontend só consome operações aprovadas no OpenAPI; nenhum campo é inventado no cliente.
- Alteração de contrato exige revisão dos dois lados e atualização das specs afetadas.
- Mensagens de erro exibidas ao utilizador derivam de `code` e `errors[].field`, nunca de `detail` técnico (FRONTEND-SPEC-03).
- A moeda selecionada e o mês de referência são estado do cliente enviado em cada chamada (`currency`, `month`; no salvamento de despesas também `referenceMonth`, para o alerta de meta — P-20). A moeda e o tema preferidos são guardados em `/preferences`, mas o servidor não usa a preferência para decidir respostas.

### Riscos

- Dinheiro como string exige conversão explícita no cliente; alternativa (número JSON) simplifica, mas arrisca precisão (P-07).
- O scaffold atual criou `openapi/openapi.yaml` em entrada única; por P-08 os módulos novos usam contratos por módulo e o contrato único é preservado só até o módulo de exemplo ser removido (ADR-004).

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [Scaffold do backend](../01-scaffold-backend-spring-modulith/spec.md)
- [BACKEND-SPEC-03](../09-backend-api-rest-e-openapi/spec.md)
- [FRONTEND-SPEC-03](../26-frontend-integracao-http/spec.md)

## Regras e critérios de aceitação

- [ ] Todas as operações do índice existem no OpenAPI com o mesmo `operationId`.
- [ ] Nenhum endpoint de agregação aceita chamada sem `currency`.
- [ ] Valores monetários, datas e meses seguem os formatos desta spec em todos os contratos.
- [ ] Respostas de erro validam contra o esquema `ProblemDetail` comum.
- [ ] A geração de interfaces (backend) e de tipos (frontend) parte do mesmo arquivo de contrato.

### Skills de implementação

- **Skills obrigatórias:** `openapi-rest-contract`.
- **Caminhos:** `C:\Users\dowgl\.codex\skills\openapi-rest-contract\SKILL.md`.
- **Responsabilidade:** redigir os contratos por módulo e as respostas comuns, **só após aprovação explícita da prévia** que a skill exige.
- **Momento de utilização:** antes de cada módulo backend e frontend que dependa do contrato.
- **Dependências:** esta spec; BACKEND-SPEC-03; P-08 (aprovada: layout por módulo).
- **Limitações:** a skill não infere módulos novos apenas pelo nome; cada módulo deve ser indicado. Esta spec não substitui a aprovação da prévia.

## Tasks

### Descoberta e decisão

- [ ] Confirmar o JSON de dinheiro como string decimal (P-07 aprovou `BigDecimal`; o formato JSON é detalhe técnico) e o layout por módulo (P-08, aprovado).

### Contrato e dados

- [ ] Preparar o pedido ao `openapi-rest-contract` por módulo a partir do índice e do BACKEND-SPEC-03.

### Validação

- [ ] Validar cada contrato (OpenAPI 3.1, `$ref`, `operationId` únicos) após a geração.

## Resultado esperado

- Um contrato por módulo, consistente com estas convenções, usado por backend e frontend.
- Preservada a regra de uma única fonte de verdade.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
