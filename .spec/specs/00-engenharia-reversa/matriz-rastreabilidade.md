# Matriz de rastreabilidade

Relaciona cada funcionalidade com a evidência, as regras, as specs de destino, o critério de aceitação e o estado da descoberta. Nenhuma linha afirma verificação em execução: a análise foi estática. Para os caminhos de código, ver [descobertas.md](descobertas.md).

Legenda de estado: **C** = CONFIRMADO no código · **C\*** = confirmado com ressalva (ver pendência) · **I** = INFERIDO · **P** = PENDENTE decisão · **N** = PROPOSTO (sem evidência de origem).
Specs: B = BACKEND-SPEC, F = FRONTEND-SPEC, I = INFRA-SPEC, Q = QUALITY-SPEC (números de ID, não de pasta).

| Func. | Evidência (origem) | Descobertas | Regras | B | F | I/Q | Critério de aceitação (resumo) | Estado |
|---|---|---|---|---|---|---|---|---|
| F-01 Lançamentos | `transaction.dart`, `transaction_form_dialog.dart`, `transactions_screen_web.dart`, `transaction_repository.dart` | DESC-TXN-01…11 | RN-TXN-01…09, RN-VAL | 11 | 10, 05 | Q-01/02/04 | valor > 0; categoria compatível; paginação 15; totais do filtro completo | C (TXN-02 C\*, TXN-11 I) |
| F-02 Categorias | `category.dart`, `sub_category.dart`, `category_repository.dart`, `category_form_dialog.dart`, `seed_service.dart` | DESC-CAT-01…06, DESC-SUB-01…04, DESC-SEE-01 | RN-CAT-01…10 | 10 | 11 | Q-02 | unicidade; exclusão bloqueada; "Outros" protegida; 11 categorias | C (CAT-05/06, SUB-02/03 divergências → P-11…P-13) |
| F-03 Mês de referência | `providers.dart::referenceMonthProvider`, `month_switcher.dart` | DESC-MES-01/02 | RN-MES-01 | 12 | 04, 09, 10 | Q-04 (J-03) | inicia no mês atual; navegação; não persistido | C |
| F-04 Saldo e dashboard | `report_service.dart`, `providers.dart`, `home_screen_web.dart` | DESC-MES-03, DESC-DSH-01…03 | RN-SAL-01/02 | 12 | 09 | Q-04 | 750/400 por moeda; variação; 8 últimos | C (DSH-03 → P-14) |
| F-05 Multimoeda | `app_currency.dart`, `currency_extension.dart`, `currency_selector.dart` | DESC-CUR-01…08 | RN-CUR-01/02 | todos | 02, 04 | Q-01/04 | nunca somar moedas; padrão BRL; legado → BRL | C (CUR-04 → P-15) |
| F-06 Metas | `budget.dart`, `budget_repository.dart`, `budget_service.dart`, `budget_screen.dart` | DESC-BUD-01…05, 09…11 | RN-BUD-01…05 | 13 | 13 | Q-01/02 | estados 700/800/950/1000/1200; ajuste do mês | C (BUD-13 → P-06) |
| F-07 Alertas | `budget_service.dart::crossedThreshold`, `transaction_form_dialog.dart::_alertBudget` | DESC-BUD-06…08 | RN-BUD-06…08 | 13, 11 | 10 | Q-04 (J-04/05) | transição, um alerta, categoria primeiro | C (BUD-08 I → P-20) |
| F-08 Cofrinhos | `money_box.dart`, `cofrinhos_screen_web.dart`, `cofrinho_dialogs.dart` | DESC-BOX-01, 05, 07, 08, 10, 12 | RN-BOX-01/04/06/09 | 14 | 12 | Q-04 (J-07) | moeda bloqueada; exclusão atómica | C (BOX-07 → P-21) |
| F-09 Movimentações | `money_entry.dart`, `money_box_service.dart`, `providers.dart` | DESC-BOX-02…04, 06, 11 | RN-BOX-02/03/05 | 14 | 12 | Q-01 | 1000+500−300=1200; moeda igual | C (BOX-06 → P-04) |
| F-10 Meta geral reservas | `budget_repository.dart::setReserveGoal`, `cofrinho_dialogs.dart` | DESC-BOX-09 | RN-BOX-08 | 14 | 12 | Q-01 | independente da soma das metas | C |
| F-11 Relatórios | `report_service.dart`, `reports_screen_web.dart`, `providers.dart::filteredTransactionsProvider` | DESC-REP-01…08 | RN-REP-01…05 | 15 | 14 | Q-01/04/05 | totais e barras do mesmo intervalo | C\* (REP-03 defeito → P-09; REP-02 → P-05; REP-07 → P-10) |
| F-12 Backup manual | `backup_service.dart`, `backup_screen.dart` | DESC-BKP-01…05 | RN-BKP cancelada | ✖ (16) | ✖ (15) | — | removido do escopo | **EXCLUÍDA** (P-03/P-16) |
| F-13 Backup automático | `AutoBackupController`, `auto_backup_io_web.dart` | DESC-BKP-06/07 | — | — | — | — | removido do escopo | **EXCLUÍDA** (P-03) |
| F-14 Preferências | `providers.dart::ThemeModeNotifier`, `SelectedCurrencyNotifier` | DESC-PRF-01/02, DESC-CUR-03 | — | 17 | 04, 02 | Q-04 (J-11) | persistem por utilizador; padrões BRL e DARK (tema escuro desde 2026-10-27) | C (servidor, P-19 delegada) |
| F-15 Migração legada | `money_box_migration.dart` | DESC-BOX-13 | RN-BKP-05 cancelada | — | — | — | sem dados legados | **EXCLUÍDA** (P-16) |
| F-16 PWA/offline | `web/manifest.json`, `web/sw.js` | DESC-INF-01 | — | — | — | — | sem offline; entrega em Docker | **EXCLUÍDA** (P-02) |
| F-19 Recuperação de senha (novo) | ausente na origem | — | BACKEND-SPEC-18 (regras no próprio spec) | 18, 07 | 16 | Q-04 (J-00b), Q-05 | link de uso único por e-mail; resposta neutra; sessões revogadas | N (P-24 decidida) |
| F-18 Contas e isolamento (novo) | ausente na origem | DESC-DAT-01 | RN-OWN-01…04 | 05 | 06 | Q-04 (J-00, J-09), Q-05 | e-mail e senha; cada utilizador só vê os seus dados | N (P-01 decidida) |
| F-02 Categorias (implementação) | F4 | Módulo `category` (contrato `categories`, V2, semente por `UserRegistered`, SPI `CategoryUsageGuard`), módulo `example` removido; página `/categorias` com `CategoriesStore`, `Modal` e `ConfirmDialog` (specs 15 e 34) | ✅ `./mvnw clean verify` com Docker (181 testes: unicidade, corrida concorrente, "Outros" protegida, isolamento, semente 7+4); 90 testes Vitest; percurso no navegador contra a API real | ⏳ `CATEGORY_IN_USE`/`CATEGORY_TYPE_CONFLICT` e desassociação de subcategoria com lançamentos reais (F5); seletores de Lançamentos e Metas (F5/F6); teclado completo, 320 px e contraste |
| F-01 Lançamentos (implementação) | F5 | Módulo `transaction` (contrato `transactions`, V3, listagem mensal por moeda com totais e paginação, `ExpenseQuery`, `BudgetImpactPort`, guarda de uso de categoria), página `/lancamentos` com `MoneyInputDirective` (specs 16 e 33) | ✅ `./mvnw clean verify` com Docker (214 testes: validação, compatibilidade de categoria, totais do conjunto filtrado, ordenação, edição, isolamento entre utilizadores e moedas, `CATEGORY_IN_USE`/`CATEGORY_TYPE_CONFLICT` reais); 127 testes Vitest; percurso no navegador contra a API real | ⏳ cálculo real do alerta de meta e concorrência no limiar (F6); agregados do dashboard (F7); teclado completo, 320/768 px e contraste |
| F-17 Navegação/layout | `router.dart`, `web_shell.dart`, `adaptive_layout.dart` | DESC-NAV-01/02, DESC-UX-01/02 | — | — | 01, 02, 07 | Q-04 (J-12) | 7 áreas; menu; responsivo | C (responsividade fina: I) |
| Dados/precisão | `transaction.dart`, `report_service.dart` | DESC-DAT-03/04 | RN-VAL, RT-02/03 | 02 | 03 | Q-01 | `NUMERIC(19,2)`; `LocalDate` | N (P-07) |
| Infra e operação | `tools/*.ps1`, ausência de CI | DESC-INF-02/03 | — | 08 | — | I-00…12 | ambientes, CI/CD, backup do banco | N (P-18, P-23) |
| Testes | `test/*.dart` | DESC-TST-01 | — | 09 | 08 | Q-00…05 | cenários migrados e novos | C (lidos, não executados) |

## Estado da implementação por funcionalidade (atualizado em 2026-10-27, após F1 e F2)

A tabela acima descreve a origem (análise estática). Esta mostra o que já foi implementado no destino e como foi verificado; só entram funcionalidades com algum trabalho executado. Legenda: ✅ verificado · ⏳ parcial · — não iniciado.

| Func. | Fatia | Implementado | Verificação | Pendente |
|---|---|---|---|---|
| F-18 Contas e isolamento | F1 (backend), F2 (frontend) | Módulo `identity` (cadastro, entrada, renovação rotativa, saída, utilizador atual), `CurrentOwner`, V1 `app_user`/`refresh_token`; páginas `/entrar` e `/cadastrar`, guardas, interceptores, sessão em memória | ✅ `./mvnw clean verify` com Docker (159 testes: migração, persistência, fluxo com rotação/reuso/429, isolamento entre dois utilizadores); 72 testes Vitest; percurso real no navegador contra o backend (specs 11 e 29) | ⏳ 11 categorias na semeadura (F4); isolamento por módulo de dados (F4–F10); e-mail de recuperação (F11) |
| F-17 Navegação/layout | F2 | Shell com seis entradas, gaveta, rotas lazy, página não encontrada, títulos e foco por rota (specs 24 e 30) | ✅ build e testes; 320 px e gaveta verificados no navegador | ⏳ páginas reais das rotas (F4–F9); percurso completo por teclado; 768/1280 px; verificador de acessibilidade |
| F-03 Mês de referência | F2 | `ReferenceMonthStore` (sessão) e `MonthSwitcher` | ✅ testes unitários | ⏳ consultas dependentes do mês (F5 em diante) |
| F-05 Multimoeda | F2 (estado) | `CurrencyStore` (`localStorage`), `CurrencySelector`, `formatMoney` sem `number` | ✅ testes unitários | ⏳ moeda por utilizador no servidor (F10); dados por moeda (F5 em diante) |
| F-14 Preferências | F2 (parte) | Tema claro/escuro/sistema em `ThemeStore` com `localStorage` | ✅ testes unitários | ⏳ servidor (F10); cores do tema claro escurecidas, a aprovar (spec 25) |
| Dados/precisão (RT-02/03) | F2 | Dinheiro como string decimal na formatação e nos validadores | ✅ testes e busca sem `parseFloat` | ⏳ `NUMERIC(19,2)` e `BigDecimal` a partir da F4/F5 |
| Infra e operação | F1 | `compose` com PostgreSQL 18.1, variáveis `APP_*`, proxy de desenvolvimento | ✅ banco e backend executados localmente (specs 39 e 40) | ⏳ imagens Docker, fallback SPA, CI (F3), máquina limpa (spec 41) |
| Testes | F1, F2 | Testes de domínio, casos de uso, segurança, controller, integração com Testcontainers; Vitest | ✅ executados | ⏳ E2E, isolamento por módulo, verificador de acessibilidade (F12) |
| F-19 Recuperação de senha | F11 | — | — | ⏳ `password_reset_token`, e-mail e páginas (F11) |

## Cobertura reversa: specs → funcionalidades

Specs sem funcionalidade de origem (PROPOSTO): BACKEND-SPEC-00/02 (parte)/04/05/08, FRONTEND-SPEC-00/03/06/07/08, INFRA-SPEC-*, QUALITY-SPEC-*, IMPLEMENTATION-SPEC-00, GLOBAL-SPEC-01/03. São requisitos da nova arquitetura, não comportamento da origem.

## Funcionalidades não cobertas (por decisão do produto, 2026-10-24)

- Backup manual e automático (F-12, F-13) e migração de saldo legado (F-15): excluídos por P-03 e P-16.
- PWA e uso offline (F-16): excluídos por P-02.
- Itens exclusivamente móveis: excluídos por escopo.

## Decisões que mudam comportamento da origem

| Decisão | Origem | Destino |
|---|---|---|
| P-04 | saque podia deixar saldo negativo (DESC-BOX-06) | saldo nunca negativo; saque recusado com saldo ≤ 0 ou excedente |
| P-09 | barras do relatório com data atual (DESC-REP-03) | barras e totais do mesmo intervalo |
| P-14 | meta ausente no dashboard web (DESC-DSH-03) | progresso da meta exibido |
| P-06 | ajuste ≤ 0 volta ao padrão | também "sem meta neste mês" explícito |
| P-11, P-13 | subcategoria órfã; tipo mudável sem verificação | desassocia; tipo bloqueado em conflito |
| P-01 | sem utilizadores | login e isolamento |
