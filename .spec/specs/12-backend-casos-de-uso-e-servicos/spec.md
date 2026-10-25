# 12-backend-casos-de-uso-e-servicos

> **ID:** BACKEND-SPEC-06 · **Categoria:** Backend · **Estado:** proposta para revisão — não aprovada, não executada.

## Objetivo

Catalogar os casos de uso da camada de aplicação que substituem as ações dos notifiers Riverpod e dos serviços da origem, com as suas pré-condições, fronteira transacional e portas.

## Escopo

- Inclui: lista de casos de uso por módulo, orquestração, transações e portas de saída.
- Não inclui: regras (BACKEND-SPEC-01), endpoints (BACKEND-SPEC-03), detalhes de cada módulo (specs 15–22).

## Contexto técnico local

### Mapa origem → caso de uso

| Módulo | Caso de uso | Origem | Transação | Portas de saída |
|---|---|---|---|---|
| category | `ListCategories`, `GetCategory` | `CategoriesNotifier`, `CategoryRepository.getAll/getByType` | leitura | `CategoryRepository` |
| category | `CreateCategory` (com "Outros" e subcategorias) | `category_form_dialog::_save` | sim | `CategoryRepository` |
| category | `UpdateCategory`, `DeleteCategory` (usa `CategoryUsageGuard`) | idem; `deleteCategory` | sim | `CategoryRepository`, `CategoryUsageGuard` |
| category | `AddSubcategory`, `DeleteSubcategory` | `saveSubCategory`, `deleteSubCategory` | sim | `CategoryRepository` |
| category | `SeedDefaultCategories` | `SeedService.seedIfFirstRun` | sim, idempotente | `CategoryRepository` |
| transaction | `CreateTransaction`, `UpdateTransaction` (+ `BudgetImpactPort`) | `TransactionsNotifier.save`, `_alertBudget` | sim | `TransactionRepository`, `CategoryReader`, `BudgetImpactPort` |
| transaction | `DeleteTransaction`, `GetTransaction`, `ListTransactions` | `delete`, `monthTransactionsProvider` | sim / leitura | `TransactionRepository` |
| transaction | `QueryExpenseTotals` (API pública para `budget`/`report`) | `monthExpensesByCategoryProvider` | leitura | `TransactionRepository` |
| budget | `GetBudget`, `SaveBudget`, `GetBudgetProgress` | `BudgetNotifier`, `BudgetRepository`, `BudgetService.progress` | sim / leitura | `BudgetRepository`, `ExpenseTotals` |
| budget | `EvaluateBudgetImpact` (implementa `BudgetImpactPort`) | `BudgetService.crossedThreshold` | leitura | `ExpenseTotals` |
| moneybox | `List/Create/Update/DeleteMoneyBox` | `MoneyBoxesNotifier` | sim | `MoneyBoxRepository` |
| moneybox | `List/Create/Update/DeleteEntry` | `MoneyEntriesNotifier`, `MoneyBoxService.validateEntry` | sim | `MoneyBoxRepository`, `EntryRepository` |
| moneybox | `GetReservesSummary`, `SaveReserveGoal` | `totalReservesProvider`, `totalGoalProvider`, `BudgetRepository.setReserveGoal` | leitura / sim | `ReserveGoalRepository` |
| report | `GetMonthlyOverview`, `GetReport` | `home_screen_web`, `ReportService` | leitura | `TransactionReader`, `CategoryReader`, `ReservesReader` |
| identity | `RegisterUser` (cria o utilizador **e** as 11 categorias padrão), `Login`, `RefreshToken`, `Logout`, `GetCurrentUser` | — (novo, P-01) | cadastro: sim | `UserRepository`, `RefreshTokenRepository`, `PasswordHasher`, `SeedDefaultCategories` |
| preferences | `Get/SavePreferences` | `SelectedCurrencyNotifier`, `ThemeModeNotifier` | sim | `PreferenceRepository` |

### Orquestração notável

- **Criar/atualizar despesa:** valida → grava → pede `BudgetImpactPort.evaluate(before, after)` → devolve transação e alerta. O estado "antes" é o gasto do **mês de referência recebido** sem o lançamento antigo (na edição) e o "depois" com o novo; se a data do lançamento não pertence a esse mês, não há alerta (RN-BUD-08; P-20). Tudo na mesma transação.
- **Registar saque:** bloqueia o cofrinho, calcula o saldo, recusa se `≤ 0` ou menor que o valor (`INSUFFICIENT_BALANCE`), grava. Editar/excluir movimentações revalida o saldo ao longo do histórico (RN-BOX-05).
- **Excluir cofrinho:** uma transação; movimentações caem por FK em cascata.
- **Cadastrar utilizador:** cria o utilizador e semeia as suas 11 categorias na mesma transação; falha desfaz tudo.
- **Contexto de proprietário:** cada caso de uso recebe o proprietário autenticado como parâmetro implícito; nenhum caso de uso o aceita do cliente.

### Riscos

P-01 decidiu multiutilizador: a assinatura dos casos de uso prevê desde o início o contexto do proprietário (`Principal`), evitando refatoração posterior.

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [BACKEND-SPEC-00](../06-backend-arquitetura-java-spring/spec.md)
- [BACKEND-SPEC-01](../07-backend-dominio-e-regras-de-negocio/spec.md)

## Regras e critérios de aceitação

- [ ] Cada caso de uso listado é uma classe concreta `<Verbo><Nome>UseCase` em `application/usecase`, transacional quando altera dados; interface só quando outro módulo o consome (`@NamedInterface`).
- [ ] Nenhum caso de uso depende de tipos de `api` ou `generated`.
- [ ] `CreateTransaction` e `UpdateTransaction` devolvem o alerta de meta calculado na mesma transação.
- [ ] `RegisterUser` é atómico: falha ao semear as categorias não deixa utilizador criado.
- [ ] Nenhum caso de uso aceita o proprietário vindo do cliente.
- [ ] As portas `CategoryUsageGuard` e `BudgetImpactPort` evitam ciclos (verificado por `ModularityTests`).

### Skills de implementação

- **Skills obrigatórias:** `java-modern-engineering`, `java-spring-create-module`.
- **Caminhos:** `C:\Users\dowgl\.codex\skills\java-modern-engineering\SKILL.md`; `C:\Users\dowgl\.codex\skills\java-spring-create-module\SKILL.md`.
- **Responsabilidade:** implementar casos de uso e portas respeitando o scaffold; manter o domínio sem Spring.
- **Momento de utilização:** na implementação de cada módulo.
- **Dependências:** BACKEND-SPEC-00/01/03.
- **Limitações:** nenhuma.

## Tasks

### Back-end

- [ ] Implementar os casos de uso por módulo na ordem de IMPLEMENTATION-SPEC-00.
- [ ] Implementar `BudgetImpactPort`/`CategoryUsageGuard` e as consultas públicas entre módulos.

### Validação

- [ ] Testar unitariamente os casos de uso com portas falsas e de integração as operações atómicas.

## Resultado esperado

- Camada de aplicação clara, transacional e testável, sem lógica de negócio nos controllers.
- Preservado o comportamento dos fluxos observados na origem.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
