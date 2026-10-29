# 14-backend-testes

> **ID:** BACKEND-SPEC-09 · **Categoria:** Backend · **Estado:** executada ao longo das fatias F1–F11 e fechada na F12 (2026-10-29); comparação literal com os testes da origem e rastreio RN → teste pendentes.

## Objetivo

Definir os testes do backend: os que migram os 7 testes existentes da origem e os novos que cobrem as lacunas de cobertura identificadas.

## Escopo

- Inclui: testes de domínio, de aplicação, de persistência, de API e de módulos, com cenários mínimos.
- Não inclui: estratégia geral e testes ponta a ponta (QUALITY-SPEC-00…05), testes do frontend.

## Contexto técnico local

### Migração dos testes da origem (DESC-TST-01; lidos, não executados)

| Teste de origem | Equivalente no destino | Cenários a preservar |
|---|---|---|
| `validators_test` | Validação de DTOs (formato) | vazio/`null`/espaços; `0` e `-1` inválidos; `100,50` e `1.234,56` válidos (no cliente); no servidor, `amount` string decimal |
| `currency_extension_test` | Formatação no cliente; `Currency` no servidor | `R$1.234,56`, `€1.234,56`; `USD` rejeitado |
| `transaction_test` | Domínio `Transaction` | `amount` ≤ 0 lança exceção de domínio; `signedAmount`; edição preserva `id`/`createdAt` |
| `currency_persistence_test` | Teste de persistência (Testcontainers) | gravar e ler moeda EUR em lançamento, cofrinho e movimentação |
| `budget_service_test` | Domínio de metas | tabela de estados em 700, 800, 950, 1000, 1200; `remaining` com valor negativo; `crossedThreshold` em 4 transições |
| `money_box_service_test` | Domínio de cofrinhos | 1000+500−300=1200; vazio=0; **saque que levaria a −150 é recusado** (a origem aceitava — mudança P-04); BRL/EUR independentes; EUR em cofrinho BRL rejeitado |
| `report_service_test` | Domínio de relatórios | totais 300/80/220; saldos negativo e zero; agrupar só despesas; BRL 750 e EUR 400; semana domingo–sábado; 7 e 12 barras |

### Novos testes (lacunas — DESC-TST-01)

| Área | Cenários |
|---|---|
| Categorias | unicidade por utilizador ignorando caixa/espaços e sob concorrência; exclusão bloqueada com lançamentos; "Outros" protegida e única; subcategoria excluída desassocia lançamentos; mudar tipo com conflito → `CATEGORY_TYPE_CONFLICT`; sem limite de quantidade; tipo `BOTH` aceito nos dois sentidos |
| Lançamentos | categoria incompatível com o tipo; subcategoria de outra categoria; data futura aceita; paginação e `totals` do conjunto filtrado |
| Metas | alerta em despesa que cruza 80% e 100%; categoria prioriza global; edição de despesa; ajuste mensal substitui recorrente; ajuste 0 remove |
| Cofrinhos | alterar moeda com movimentações → `CURRENCY_LOCKED`; excluir cofrinho remove movimentações atomicamente; meta geral independente da soma; saque com saldo 0 ou excedente → `INSUFFICIENT_BALANCE`; saque igual ao saldo aceito; excluir/editar movimentação que negativaria; saques concorrentes |
| Recuperação de senha | ver os cenários de [BACKEND-SPEC-18](../48-backend-recuperacao-de-senha/spec.md): resposta neutra, expiração, uso único, revogação de sessões, limites, falha do SMTP |
| Identidade e isolamento | cadastro cria 11 categorias; e-mail duplicado em qualquer caixa; credenciais inválidas indistinguíveis; rotação e reuso do *refresh*; **para cada recurso** (categoria, lançamento, meta, cofrinho, movimentação, relatório, preferências) o utilizador B recebe 404 ao tentar acessar dados do A e as agregações não os incluem |
| Relatórios | **barras e totais do mesmo intervalo com mês de referência ≠ atual (regressão de DESC-REP-03)**; âncora semanal; ano com `% vs anterior`; ranking limitado |
| Módulos | `ModularityTests`; ausência de ciclos |
| API | cada `code` de erro; contrato (QUALITY-SPEC-03) |

### Riscos

Os testes de domínio da origem usam `double`; ao migrar, as expectativas devem usar `BigDecimal` com comparação por `compareTo`.

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [QUALITY-SPEC-00](../44-quality-estrategia/spec.md)
- [BACKEND-SPEC-01](../07-backend-dominio-e-regras-de-negocio/spec.md)

## Regras e critérios de aceitação

- [ ] Todos os cenários da tabela de migração existem como testes e passam.
  > ⏳ 2026-10-29 16:20 +00:00 — Os cenários da tabela existem como testes que passam, mapeados por regra: `budget_service_test` → `BudgetDomainTests` e `BudgetFlowIntegrationTest` (700/800/950/1000/1200, restante negativo, as quatro transições), `money_box_service_test` → `MoneyBoxDomainTests` e `MoneyBoxFlowIntegrationTest` (1000+500−300=1200, saque que negativaria recusado, BRL/EUR independentes, moeda divergente rejeitada), `report_service_test` → `OverviewCalculationsTests`, `ReportCalculationsTests` e `ReportsFlowIntegrationTest` (semana domingo–sábado, 7 e 12 barras, só despesas no ranking), `transaction_test` → `TransactionTests` e `DomainGuardTests`, `currency_persistence_test` → fluxos com EUR em PostgreSQL. Falta: comparar caso a caso com os sete testes da origem, que não estão no repositório.
- [ ] Cada `RN-*` P/E/N de BACKEND-SPEC-01 é coberta por pelo menos um teste.
  > ⏳ 2026-10-29 16:20 +00:00 — Não há (ainda) um levantamento automatizado RN → teste: as regras de cada fatia têm testes com os nomes das regras nas specs 15–20 e 22, mas a conferência regra a regra não foi feita. Próxima ação: gerar a tabela RN × teste a partir dos nomes dos testes.
- [x] Existe teste de regressão para DESC-REP-03, para saldo nunca negativo e para o isolamento entre utilizadores.
  > ✅ 2026-10-29 16:20 +00:00 — DESC-REP-03: `ReportsFlowIntegrationTest.monthBarsAddUpToTheTotalsForAnyReferenceMonthAndLengthOfMonth` (junho/2026 e fevereiro/2028 com o servidor em outubro/2026) e `reports-page.spec.ts`/J-08 no navegador. Saldo nunca negativo: `MoneyBoxFlowIntegrationTest.withdrawalAboveBalanceOrWithZeroBalanceIsRefused…`, `editOrDeleteThatWouldMakeTheBalanceNegativeIsRefused…`, `simultaneousWithdrawalsNeverExceedTheBalance`, `MoneyBoxDomainTests`. Isolamento: um teste por módulo de dados (`usersNeverSeeEachOthers…`, `oneUserNeverReadsOrChangesAnothers`, `anotherUserSeesAndChangesNothing`, `reportsOnlyCountTheAuthenticatedUsersData`) e J-09 no navegador.
- [x] Testes de persistência usam PostgreSQL real; nenhum teste usa dados financeiros reais.
  > ✅ 2026-10-29 16:20 +00:00 — Todos os testes de integração usam `postgres:18.1` por Testcontainers (nunca H2); os dados são sintéticos (`PerformanceIntegrationTest` gera 100 000 lançamentos com `generate_series`).
- [x] `mvnw clean verify` executa tudo e os resultados são registados com evidência.
  > ✅ 2026-10-29 16:20 +00:00 — `./mvnw clean verify` com Docker em 2026-10-29: 387 testes, 0 falhas, BUILD SUCCESS; relatório de cobertura JaCoCo em `target/site/jacoco`.

### Skills de implementação

- **Skills obrigatórias:** `java-modern-engineering`, `java-spring-create-module`.
- **Caminhos:** `C:\Users\dowgl\.codex\skills\java-modern-engineering\SKILL.md`; `C:\Users\dowgl\.codex\skills\java-spring-create-module\SKILL.md`.
- **Responsabilidade:** escrever testes focados por módulo junto da implementação; o segundo exige testes dos comportamentos implementados e verificação Maven.
- **Momento de utilização:** em cada módulo, com o código.
- **Dependências:** módulos 11, 15–20, 22; INFRA-SPEC-02 (Testcontainers).
- **Limitações:** nenhuma skill específica de testes nos diretórios indicados.

## Tasks

### Back-end

- [ ] Portar os testes de domínio da origem.
  > ⏳ 2026-10-29 16:20 +00:00 — Os cenários foram reescritos como testes do destino (ver o primeiro critério), com `BigDecimal` e `compareTo` onde cabe. Falta a comparação literal com os testes da origem (fora do repositório).
- [x] Escrever os testes novos por módulo.
  > ✅ 2026-10-29 16:20 +00:00 — Testes novos por módulo nas fatias F1–F11 e, na F12, `DomainGuardTests` (todos os guardas de validação do domínio), `ApiContractConformanceIntegrationTest`, `SecurityHardeningIntegrationTest`, `ProductionProfileIntegrationTest` e `PerformanceIntegrationTest`.

### Validação

- [x] Executar `mvnw clean verify` e registar contagem de testes e resultado.
  > ✅ 2026-10-29 16:20 +00:00 — 387 testes, 0 falhas; cobertura de linhas 97,8% (sem código gerado) e de ramificações do domínio 96,4% (metas aprovadas: 80% e 90%).

## Resultado esperado

- Suíte que cobre as regras migradas e as lacunas conhecidas, executável localmente e no CI.
- Preservados os valores de referência numéricos da origem.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
