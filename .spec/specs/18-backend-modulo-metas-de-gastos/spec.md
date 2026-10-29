# 18-backend-modulo-metas-de-gastos

> **ID:** BACKEND-SPEC-13 · **Categoria:** Backend (módulo `budget`) · **Estado:** executada na F6 (2026-10-29) — ver evidências.

## Objetivo

Implementar as metas de gastos por moeda — limite global recorrente, ajuste por mês e limite por categoria — com progresso, estado e avaliação de impacto que alimenta o alerta ao salvar despesas.

## Escopo

- Inclui: `getBudget`, `saveBudget`, `getBudgetProgress`; implementação de `BudgetImpactPort`.
- Não inclui: meta geral de reservas (módulo `moneybox`, BACKEND-SPEC-14), interface (FRONTEND-SPEC-13).

## Contexto técnico local

### Regras aplicadas

RN-BUD-01…09. Evidências: DESC-BUD-01…11. Em especial:

- Limite efetivo = ajuste do mês `> 0` ou recorrente (DESC-BUD-02).
- Estado exato: `EXCEEDED` se `gasto ≥ limite`; `WARNING` se `gasto × 5 ≥ limite × 4`; `NONE` se limite `≤ 0` (DESC-BUD-04).
- `fraction = gasto/limite` com 4 casas; `remaining = limite − gasto` (pode ser negativo).
- Alerta: ver RN-BUD-06/07; avalia primeiro a categoria da despesa, depois o global.

### `saveBudget`

Gravação atómica (DESC-BUD-09): substitui o conjunto de limites por categoria (itens com limite `≤ 0` ou ausentes são removidos), atualiza o limite global e aplica o ajuste do mês informado, que é flexível (P-06): `limit > 0` define o valor do mês; `disabled = true` significa "sem meta neste mês" (mesmo com meta padrão); ausente remove o ajuste e volta ao padrão. Categorias inexistentes ou que não aceitam despesa são rejeitadas (novo; a origem listava só categorias de despesa).

### `getBudgetProgress`

Inclui o global e todas as categorias que aceitam despesa, com os gastos do mês, para a tela mostrar "gasto de limite" ou apenas "Gasto: X" (DESC-BUD-09).

### Pendências e riscos

- P-06 (decidida): "sem meta neste mês" explícito, diferente de ausência de ajuste. Estado do mês desativado: `NONE`.
- P-20 (decidida): o alerta avalia o mês de referência enviado pelo cliente; despesa fora dele não alerta.
- Dependência de `transaction` para somas de despesa; `transaction` conhece `budget` só pela porta (ADR-001).

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [BACKEND-SPEC-01](../07-backend-dominio-e-regras-de-negocio/spec.md)
- [BACKEND-SPEC-11](../16-backend-modulo-lancamentos/spec.md)

## Regras e critérios de aceitação

- [x] Limite 1000: gasto 700 → `OK`; 800 → `WARNING`; 950 → `WARNING`; 1000 e 1200 → `EXCEEDED`; limite 0 → `NONE`.
  > ✅ 2026-10-29 11:55 +00:00 — `BudgetDomainTests.statusTableIsExact` (limite 1000: 700 `OK`; 800, 950 e 999,99 `WARNING`; 1000 e 1200 `EXCEEDED`; limite 0, negativo ou ausente `NONE`) e o teste do limiar sem erro de ponto flutuante (1000,10: 800,07 `OK`, 800,08 `WARNING`).
- [x] Limite 200, gasto 50 → `fraction` 0,25 e `remaining` 150; gasto 250 → `remaining` −50.
  > ✅ 2026-10-29 11:55 +00:00 — `BudgetDomainTests.progressHasFractionWithFourDecimalsAndRemaining` (0,2500 e restante 150,00; gasto 250 → restante −50,00; frações 0,3333 e 0,6667 em `HALF_UP`) e `progressShowsExactSpent…` (HTTP, PostgreSQL).
- [x] Ajuste de julho de 1500 com recorrente 1000: `effectiveLimit` de julho = 1500, de agosto = 1000; ajuste removido volta a 1000.
  > ✅ 2026-10-29 11:55 +00:00 — `BudgetUseCaseTests.overrideAppliesOnlyToItsMonth…` e `BudgetFlowIntegrationTest.newUserHasNoGoal…` (julho 1500,00; agosto 1000,00; sem `monthOverride` volta a 1000,00).
- [x] Ajuste de julho com `disabled=true`: estado de julho `NONE` e agosto continua com 1000.
  > ✅ 2026-10-29 11:55 +00:00 — `disabledMonthHasNoGoalAndOtherMonthsKeepTheRecurringOne` (julho: `monthOverride.disabled`, sem `effectiveLimit`, progresso `NONE`; agosto 1000,00) e o alerta silenciado só nesse mês (`disabledMonthSilencesAlertsOnlyForThatMonth`).
- [x] Metas BRL e EUR são independentes.
  > ✅ 2026-10-29 11:55 +00:00 — `currenciesAreIndependent` (HTTP, PostgreSQL) e `BudgetUseCaseTests`; as tabelas guardam a moeda e toda consulta a filtra.
- [x] `saveBudget` falha por completo se qualquer categoria for inválida (nada é gravado).
  > ✅ 2026-10-29 11:55 +00:00 — `invalidSaveFailsCompletelyAndKeepsThePreviousConfiguration` (categoria só de receita → 409 `CATEGORY_TYPE_MISMATCH`; inexistente ou alheia → 404; valor negativo, texto, mês inválido, `limit` e `disabled` juntos, categoria repetida → 400; a configuração anterior fica intacta) e `BudgetUseCaseTests.invalidCategory…`. Tudo é validado antes de gravar e a gravação é uma só transação.
- [x] O alerta nunca repete em estado já estourado e nunca dispara em receita.
  > ✅ 2026-10-29 11:55 +00:00 — `alertGoesFromOkToWarningToExceededAndNeverRepeats` (700 → 850 `WARNING`, 850 → 1100 `EXCEEDED`, 1100 → 1300 sem alerta), `BudgetImpactServiceTests` (700→850, 850→1100, 1100→1300, 100→300, melhora, prioridade categoria × global, limiares exatos 80% e 100%) e `noAlertForIncome…` (receita, fora do mês de referência, sem `referenceMonth` e outra moeda não alertam).

### Cenários de teste

Tabela de estados; transições (700→850, 850→1100, 1100→1300, 100→300); prioridade categoria × global; edição de despesa; ajuste mensal; atomicidade de `saveBudget`; moeda.

### Skills de implementação

- **Skills obrigatórias:** `java-modern-engineering`, `java-spring-create-module`, `java-persistence-performance`; `openapi-rest-contract` para o contrato `budgets`.
- **Caminhos:** `C:\Users\dowgl\.codex\skills\java-modern-engineering\SKILL.md`; `C:\Users\dowgl\.codex\skills\java-spring-create-module\SKILL.md`; `C:\Users\dowgl\.codex\skills\openapi-rest-contract\SKILL.md`; `.claude/skills/java-persistence-performance/SKILL.md`.
- **Responsabilidade:** contrato, domínio de metas (estados, transições) e adaptador.
- **Momento de utilização:** depois de `transaction`.
- **Dependências:** BACKEND-SPEC-05, 10, 11.
- **Limitações:** nenhuma específica.

## Tasks

### Contrato e dados

- [x] Prévia aprovada e contrato `budgets`; migração das tabelas de metas.
  > ✅ 2026-10-29 11:55 +00:00 — Prévia aprovada pelo utilizador em conversa (3 operações, schemas, erros e as 3 decisões de contrato: `month` no topo do pedido, `PUT /budgets/{currency}`, progresso em `/budgets/{currency}/progress`) e gerada em `openapi/modules/budgets.yaml` + execução `generate-budgets-api`; `validate.mjs` válido, `generate-sources` gerou `BudgetsApi` e modelos, sem `operationId` repetido. `V4__budget.sql` (`budget_setting`, `budget_month_override`, `budget_category_limit`: `numeric(12,2)`, CHECKs, `ON DELETE CASCADE` da categoria) aplicada do zero (`IdentityMigrationTests`) e sobre o banco local já com a V3. Desvio: o YAML foi escrito à mão (a skill não aceita `$ref` para schemas locais) e `month` saiu de `monthOverride`.

### Back-end

- [x] Implementar domínio, casos de uso, adaptador, controller e `BudgetImpactPort`.
  > ✅ 2026-10-29 11:55 +00:00 — Módulo `budget` (domínio `BudgetStatus`, `Progress`, `BudgetConfig`, `MonthOverride`, `LimitAmount`; `GetBudget`, `SaveBudget`, `GetBudgetProgress`; adaptador JPA com filtro por `owner_id` e moeda; `BudgetsController`; `BudgetImpactService` real). `BudgetImpactPort` e `BudgetAlert` passaram para a raiz de `transaction` (API pública) para o `budget` poder implementá-los: dependência `budget → transaction`, sem ciclo (`ModularityTests` verde); `ExpenseQuery` ganhou `expenseTotalsByCategory` (uma consulta agrupada) e `CategoryDirectory` ganhou `listAccepting`. Controle de concorrência escolhido pela invariante: lock consultivo do PostgreSQL por utilizador+moeda na gravação das metas e por utilizador+moeda+mês nas despesas, sem retry.

### Validação

- [x] Executar os cenários e comparar com os valores de `budget_service_test`.
  > ✅ 2026-10-29 11:55 +00:00 — `./mvnw clean verify` com Docker: 261 testes, 0 falhas (`BudgetFlowIntegrationTest` 16, `BudgetUseCaseTests` 9, `BudgetImpactServiceTests` 4, `BudgetDomainTests` 18), incluindo duas despesas simultâneas no limiar (4 threads: exatamente um `WARNING` e nenhum `EXCEEDED`) e gravações simultâneas da mesma meta (todas 200, conjunto consistente). Os valores comparados são os da tabela desta spec, derivada de `budget_service_test`; o ficheiro de origem não foi relido. Verificado também na interface contra a API real.

## Resultado esperado

- Metas e alertas idênticos aos da origem, calculados no servidor com comparação exata.
- Preservada a regra: um alerta, só na piora do estado, categoria primeiro.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
