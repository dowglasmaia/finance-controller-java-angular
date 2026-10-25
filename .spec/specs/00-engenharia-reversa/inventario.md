# Inventário funcional e técnico da origem

Resumo do que existe em `projetos/finance_controller` (revisão `03f1079…`). Evidência completa em [descobertas.md](descobertas.md). Análise estática; nada foi executado.

## Visão geral

| Item | Valor |
|---|---|
| Aplicação | Finance Controller — controlo financeiro pessoal |
| Tecnologia | Flutter (`sdk ^3.12.2`), Riverpod 2.x, go_router 14, Hive 2.2, shared_preferences, fl_chart 0.69, flutter_animate, intl 0.20, uuid 4, package:web |
| Plataformas na origem | Android, iOS, Web/PWA (`app_desktop/`); **destino só web** |
| Tamanho | 83 ficheiros em `lib/` (incl. 6 `*.g.dart` gerados), 7 ficheiros em `test/`, 8 documentos em `docs/` |
| Dados | Hive em IndexedDB (6 coleções), sem servidor |
| Locale | `pt_BR`, moedas BRL e EUR |

## Funcionalidades

| ID | Funcionalidade | Telas web | Estado na análise |
|---|---|---|---|
| F-01 | Lançamentos | `transactions_screen_web`, `transaction_form_dialog` | analisada |
| F-02 | Categorias, subcategorias, semente | `categories_screen_web`, `category_form_dialog`, `seed_service` | analisada |
| F-03 | Mês de referência | `month_switcher`, `referenceMonthProvider` | analisada |
| F-04 | Saldo mensal e dashboard | `home_screen_web`, `kpi_card`, gráficos | analisada |
| F-05 | Multimoeda e seletor | `currency_selector`, `app_currency` | analisada |
| F-06/07 | Metas e alertas | `budget_screen` (compartilhada), `budget_service` | analisada |
| F-08/09/10 | Cofrinhos, movimentações, meta geral | `cofrinhos_screen_web`, `cofrinho_dialogs` | analisada |
| F-11 | Relatórios | `reports_screen_web`, `report_service` | analisada (defeito candidato DESC-REP-03) |
| F-12/13 | Backup manual e automático | `backup_screen` (compartilhada), `backup_service`, `auto_backup_io_web` | analisada; interop web lida só no início |
| F-14 | Tema e moeda | `ThemeModeNotifier`, `SelectedCurrencyNotifier` | analisada |
| F-15 | Migração de saldo legado | `money_box_migration` | analisada |
| F-16 | PWA / offline | `web/manifest.json`, `web/sw.js` | listada; conteúdo de `sw.js` **não** lido em detalhe |
| F-17 | Navegação e layout web | `router.dart`, `web_shell`, `adaptive_layout` | analisada |

## Entidades e coleções

`Category` (0, `categories`), `SubCategory` (1, `subcategories`), `Transaction` (2, `transactions`), `Budget` (3, `budget`, registro `current`), `MoneyBox` (4, `money_boxes`), `MoneyEntry` (5, `money_entries`). Detalhes em [GLOBAL-SPEC-02](../04-global-dominio-e-glossario/spec.md).

## Estado e serviços

Estado: providers Riverpod em `data/providers.dart` (reativos, derivados e de configuração). Serviços puros: `ReportService`, `BudgetService`, `MoneyBoxService`; infraestrutura: `HiveService`, `SeedService`, `BackupService`, `money_box_migration`, E/S de backup (stub/web). Repositórios: `TransactionRepository`, `CategoryRepository`, `BudgetRepository`, `MoneyBoxRepository`, `MoneyEntryRepository`.

## Integrações e infraestrutura

- Nenhuma chamada de rede, API externa, autenticação ou variável de ambiente (DESC-INF-03).
- Scripts PowerShell: `build-pwa`, `serve-build`, `reconstruir`, `gerar_icones` (DESC-INF-02).
- Sem CI/CD; sem logging estruturado; sem telemetria.

## Testes

Sete ficheiros: `validators`, `currency_extension`, `transaction`, `currency_persistence`, `budget_service`, `money_box_service`, `report_service`. Execução não realizada (DESC-TST-01).

## Documentação existente na origem

`AGENTS.md`, `CLAUDE.md`, `README.md`, `PROMPT.md` (roteiro histórico), `docs/{README,funcionalidades,guia-de-uso,metas-orcamento,relatorios,regras-de-negocio,arquitetura,moedas-e-pwa}.md`. Três divergências entre documentação e código estão em DESC-CUR-08, DESC-BUD-11 e DESC-REP-07.

## Fora do escopo do destino

Por decisão do produto (2026-10-24): backup manual e automático, importação, migração de dados da origem, PWA e uso offline. Além disso: telas móveis (`*_screen.dart` sem `_web`, `new_*_screen`), `app_bottom_nav`, `android/`, `ios/`, ícones/instalador desktop, swipe-para-excluir, agrupamento "Hoje/Ontem" da lista móvel.
