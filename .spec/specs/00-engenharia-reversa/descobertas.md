# Catálogo de descobertas da engenharia reversa

Fonte única de evidências. As specs referenciam estes IDs em vez de repetir a evidência.

- **Origem analisada:** `projetos/finance_controller` (Flutter), revisão Git `03f107936fd2ad06a1d83ac3ba179851716c9722` (2026-09-17), árvore de trabalho limpa (`git status --short` sem alterações).
- **Método:** análise estática por leitura de código. Nenhum teste, build ou servidor foi executado. Caminhos abaixo são relativos à origem.
- **Classificação:** `CONFIRMADO` (código lido, caminho relevante inspecionado) · `INFERIDO` (deduzido) · `PENDENTE` (exige decisão humana) · `PROPOSTO` (decisão técnica recomendada para o destino). Um teste citado foi **lido, não executado**.
- Escopo: apenas o que afeta backend e frontend web. Telas exclusivamente móveis (`*_screen.dart` sem sufixo `_web`, `new_*_screen.dart`, `app_bottom_nav.dart`) foram usadas só para confirmar lógica compartilhada.

## Moedas (CUR)

| ID | Classe | Descoberta | Evidência | Teste |
|---|---|---|---|---|
| DESC-CUR-01 | CONFIRMADO | Moedas suportadas: apenas BRL (`R$`) e EUR (`€`); código textual estável. | `lib/core/models/app_currency.dart::AppCurrency` | `currency_extension_test` |
| DESC-CUR-02 | CONFIRMADO | Sem conversão cambial. Totais, saldo, ranking, metas e reservas filtram por moeda antes de somar; o filtro existe nos providers e de novo nos serviços. | `report_service.dart::totalIncome/totalExpense/balance/groupByCategoryId`; `providers.dart::monthTransactionsProvider, visibleMoneyBoxesProvider, totalReservesProvider` | `report_service_test` (BRL 1000−250=750; EUR 500−100=400), `money_box_service_test` |
| DESC-CUR-03 | CONFIRMADO | Moeda global persistida em `SharedPreferences` (`selected_currency`); padrão BRL. | `providers.dart::SelectedCurrencyNotifier` | — |
| DESC-CUR-04 | CONFIRMADO | Salvar um lançamento chama `setCurrency(moeda do lançamento)`: a moeda global muda como efeito colateral. | `transaction_form_dialog.dart::_save`, `new_transaction_screen.dart::_save` | — |
| DESC-CUR-05 | CONFIRMADO | Dados legados sem moeda → BRL. Leitura tolerante: índice 0/1, texto `eur`, `AppCurrency.eur`. | `app_currency.dart::fromCode/fromStored/normalizeStoredCode` | `currency_extension_test`, `currency_persistence_test` |
| DESC-CUR-06 | CONFIRMADO | Entrada externa (backup) com moeda desconhecida é rejeitada (`FormatException`). | `app_currency.dart::parse`; `backup_service.dart::_validateCurrencies` | `currency_extension_test` |
| DESC-CUR-07 | CONFIRMADO | Formatação `NumberFormat.currency(locale pt_BR, 2 casas)` com símbolo `R$` ou `€`; sinal explícito `+`/`-` em listas. | `currency_extension.dart::toCurrency/toSign` | `currency_extension_test` |
| DESC-CUR-08 | CONFIRMADO | **Divergência documental:** `AGENTS.md` diz "Só R$"; código e `docs/moedas-e-pwa.md` implementam BRL e EUR. Prevalece o código. | `AGENTS.md` × `AppCurrency` | — |

## Lançamentos (TXN)

| ID | Classe | Descoberta | Evidência | Teste |
|---|---|---|---|---|
| DESC-TXN-01 | CONFIRMADO | Campos: `id` (UUID v4 gerado no cliente), `description`, `amount` (double), `date` (DateTime), `isIncome`, `categoryId`, `subCategoryId?`, `notes?`, `createdAt`, `currencyCode`. | `lib/data/models/transaction.dart` | `transaction_test` |
| DESC-TXN-02 | CONFIRMADO | `amount > 0` é garantido por `assert` (só em modo debug) e, na jornada, por `Validators.positiveAmount`. A importação não valida explicitamente. | `transaction.dart` (assert); `validators.dart::positiveAmount`; `backup_service.dart::importJson` | `transaction_test`, `validators_test` |
| DESC-TXN-03 | CONFIRMADO | Descrição obrigatória (`Campo obrigatório`); sem limite de tamanho. Categoria obrigatória (`Selecione uma categoria`). Validação apenas no formulário. | `transaction_form_dialog.dart`, `validators.dart::required` | `validators_test` |
| DESC-TXN-04 | CONFIRMADO | O seletor de categoria lista só categorias que aceitam o tipo (entrada/saída). Trocar o tipo limpa categoria e subcategoria se incompatíveis. | `transaction_form_dialog.dart::_categories/_toggle`; `category.dart::acceptsIncome/acceptsExpense` | — |
| DESC-TXN-05 | CONFIRMADO | Subcategoria opcional, listada a partir da categoria escolhida; mudar a categoria limpa a subcategoria. | `transaction_form_dialog.dart` | — |
| DESC-TXN-06 | CONFIRMADO | Data inicial: agora, se o mês de referência é o atual; senão dia 1 do mês de referência. Seletor de 2000-01-01 a 2100-12-31. Datas futuras permitidas. | `transaction_form_dialog.dart::didChangeDependencies/_pickDate` | — |
| DESC-TXN-07 | CONFIRMADO | Moeda do formulário: padrão = moeda global; editável. Edição preserva `id` e `createdAt`. Observações vazias viram `null` (trim). | `transaction_form_dialog.dart::_save` | `transaction_test` (copyWith) |
| DESC-TXN-08 | CONFIRMADO | Exclusão pede confirmação e informa `Lançamento excluído.`. Salvar informa `Lançamento salvo.`. | `confirm_delete_dialog.dart`, `transactions_screen_web.dart::_delete` | — |
| DESC-TXN-09 | CONFIRMADO | Listagem web: mês de referência + moeda global; filtro Todos/Entradas/Saídas; paginação de 15; subtotais (entradas, saídas, saldo) do conjunto filtrado inteiro; ordenação por data decrescente; vazio: `Nenhum lançamento no período.`. | `transactions_screen_web.dart`; `transaction_repository.dart::getAll` | — |
| DESC-TXN-10 | CONFIRMADO | Campo de valor mascarado por dígitos (centavos) em pt_BR; `parseAmount` aceita `1.234,56` e `R$ 99,90`. | `amount_input_field.dart::CurrencyInputFormatter`; `validators.dart::parseAmount` | `validators_test` |
| DESC-TXN-11 | INFERIDO | Empate de data não tem desempate definido (ordena só por `date`). | `transaction_repository.dart::getAll` | — |

## Categorias e subcategorias (CAT / SUB / SEE)

| ID | Classe | Descoberta | Evidência | Teste |
|---|---|---|---|---|
| DESC-CAT-01 | CONFIRMADO | Campos: `id`, `name`, `icon` (nome Material), `color` (ARGB int), `type` ∈ {income, expense, both}, `subCategoryIds`, `createdAt`. Catálogo de 24 ícones; paleta de 12 cores no formulário. | `category.dart`, `icon_catalog.dart`, `category_form_dialog.dart::_palette` | — |
| DESC-CAT-02 | CONFIRMADO | Nome obrigatório; único ignorando caixa e espaços nas pontas. A checagem (`nameExists`) ocorre nos formulários (web e mobile), não em `saveCategory`. Mensagem: `Já existe uma categoria com este nome.`. | `category_repository.dart::nameExists/saveCategory`; `category_form_dialog.dart::_save`; `new_category_screen.dart` | — |
| DESC-CAT-03 | CONFIRMADO | Excluir categoria com lançamentos vinculados é bloqueado (`CategoryException`: `Categoria possui lançamentos vinculados e não pode ser excluída.`). Sem lançamentos, as subcategorias são removidas junto. | `category_repository.dart::deleteCategory` | — |
| DESC-CAT-04 | CONFIRMADO | Abas web: Despesas = `acceptsExpense`, Receitas = `acceptsIncome`; `both` aparece nas duas. Ordenação alfabética sem distinguir caixa. | `categories_screen_web.dart`, `category_repository.dart::getAll` | — |
| DESC-CAT-05 | CONFIRMADO | `kMaxCategories = 50` está definido mas **não é aplicado** em nenhum ponto. | `app_constants.dart` (único uso) | — |
| DESC-CAT-06 | CONFIRMADO | Editar categoria (nome, tipo, ícone, cor) não verifica lançamentos existentes; mudar o tipo pode deixar lançamentos numa categoria que já não aceita o tipo deles. | `category_form_dialog.dart::_save` | — |
| DESC-SUB-01 | CONFIRMADO | "Outros" (comparação por nome, sem distinguir caixa) é imutável: não pode ser excluída (`A subcategoria "Outros" não pode ser excluída.`). Toda categoria nova nasce com "Outros". A UI não oferece renomear subcategorias. | `sub_category.dart::isImmutable`; `category_repository.dart::deleteSubCategory`; `category_form_dialog.dart` | — |
| DESC-SUB-02 | CONFIRMADO | Adicionar subcategoria só rejeita nome vazio; permite duplicados, inclusive um segundo "Outros". | `category_form_dialog.dart::_addSub` | — |
| DESC-SUB-03 | CONFIRMADO | Excluir subcategoria não verifica lançamentos: o `subCategoryId` fica órfão e as tabelas mostram `—`. | `category_repository.dart::deleteSubCategory`; `transactions_screen_web.dart` | — |
| DESC-SUB-04 | CONFIRMADO | Em edição, adicionar/remover subcategoria grava imediatamente (sem esperar "Salvar"); em criação, vive numa lista local até salvar. | `category_form_dialog.dart::_addSub/_removeSub` | — |
| DESC-SEE-01 | CONFIRMADO | Primeira execução (flag `seeded`): 11 categorias — despesa: Carro, Alimentação, Moradia, Saúde, Lazer, Educação, Vestuário; receita: Salário, Vale Alimentação, Investimento, Bônus — cada uma com subcategorias e "Outros". | `seed_service.dart::seedIfFirstRun/_data` | — |

## Mês de referência, saldo e dashboard (MES / DSH)

| ID | Classe | Descoberta | Evidência | Teste |
|---|---|---|---|---|
| DESC-MES-01 | CONFIRMADO | Mês de referência inicia no 1º dia do mês atual; navegação ±1 mês sem limite; clicar no rótulo volta ao mês atual (só se diferente). Não é persistido. | `providers.dart::referenceMonthProvider`; `month_switcher.dart` | — |
| DESC-MES-02 | CONFIRMADO | Lançamentos do mês: mesmo ano e mês de `date` (componentes locais) e mesma moeda. | `providers.dart::monthTransactionsProvider` | — |
| DESC-MES-03 | CONFIRMADO | Saldo = Σ entradas − Σ saídas; nunca persistido. Variação vs mês anterior: `(atual−anterior)/anterior·100` só se anterior > 0; `\|%\|<0,5` → `0%`; formato `+N%`. | `report_service.dart::balance`; `providers.dart::previousMonthSummaryProvider`; `home_screen_web.dart::_delta` | `report_service_test` |
| DESC-DSH-01 | CONFIRMADO | Dashboard web: saudação por hora (<12 Bom dia; <18 Boa tarde; senão Boa noite); KPIs Saldo, Entradas, Saídas (com variação), Reservas; barras Entradas×Saídas do mês por faixas de 7 dias (S1…S5); pizza de despesas por categoria; 8 últimos lançamentos com link "Ver todas". | `home_screen_web.dart` | — |
| DESC-DSH-02 | CONFIRMADO | O KPI Reservas independe do mês (soma acumulada por moeda). Para Saídas, variação "positiva" = gastar menos. | `home_screen_web.dart`, `totalReservesProvider` | — |
| DESC-DSH-03 | CONFIRMADO | O dashboard **web** não mostra o progresso da meta; o `BudgetCard` só é usado no dashboard móvel. A memória de produto cita o acompanhamento de metas no dashboard. | `home_screen.dart:63` × `home_screen_web.dart` | — |

## Metas de gastos (BUD)

| ID | Classe | Descoberta | Evidência | Teste |
|---|---|---|---|---|
| DESC-BUD-01 | CONFIRMADO | `Budget` é registro único (chave `current`) com campos BRL e EUR: limite mensal, limites por categoria, ajustes por mês (`YYYY-MM`) e meta geral de reservas. | `budget.dart`, `budget_repository.dart` | — |
| DESC-BUD-02 | CONFIRMADO | Limite efetivo do mês = ajuste do mês se > 0; senão o limite recorrente. Ajuste ≤ 0 equivale a remover o ajuste. | `budget.dart::effectiveLimit`; `budget_repository.dart::setMonthlyOverride` | — |
| DESC-BUD-03 | CONFIRMADO | Limite de categoria > 0 é gravado; ≤ 0 remove. Ausente ou ≤ 0 → sem meta da categoria. | `budget_repository.dart::setCategoryLimit`; `budget.dart::limitForCategory` | — |
| DESC-BUD-04 | CONFIRMADO | Status: sem limite (≤0) → `none`; `gasto/limite ≥ 1,0` → `exceeded`; `≥ 0,8` → `warning`; senão `ok`. `remaining = limite − gasto` (pode ser negativo). | `budget_service.dart::BudgetProgress` | `budget_service_test` |
| DESC-BUD-05 | CONFIRMADO | O progresso usa as despesas do **mês de referência** e da moeda global, globais e por categoria. | `providers.dart::globalBudgetProgressProvider, categoryBudgetProgressProvider` | — |
| DESC-BUD-06 | CONFIRMADO | `crossedThreshold`: sem alerta se limite ≤ 0 ou se o status não mudou; alerta `exceeded` ao passar a estourado; `warning` apenas de `ok` para `warning`. Nunca alerta em melhora ou repetição. | `budget_service.dart::crossedThreshold` | `budget_service_test` |
| DESC-BUD-07 | CONFIRMADO | Só despesas disparam alerta ao salvar. Prioridade: categoria (`Meta de X estourada!` / `Você está perto da meta de X.`), depois a global (`Meta mensal estourada!` / `Você já usou 80% da sua meta mensal.`). Um único alerta. | `transaction_form_dialog.dart::_alertBudget` | — |
| DESC-BUD-08 | INFERIDO | Como o progresso só conta o mês de referência, uma despesa datada fora dele não altera o progresso e não alerta. Confirmar em execução. | `providers.dart` + `_alertBudget` | — |
| DESC-BUD-09 | CONFIRMADO | A tela de meta (única, compartilhada com o mobile) grava de uma vez: meta padrão, ajuste do mês de referência e limites de todas as categorias que aceitam despesa; vazio → 0 → remove. O formatador só aceita dígitos, logo não há negativos. | `budget_screen.dart::_save`, `amount_input_field.dart` | — |
| DESC-BUD-10 | CONFIRMADO | Rótulos de status: `Meta estourada`, `Perto da meta`, `Dentro da meta`, `Sem meta definida`; cores coral/âmbar/esmeralda/neutro. | `budget_visuals.dart` | — |
| DESC-BUD-11 | CONFIRMADO | **Divergência documental:** `docs/metas-orcamento.md` diz "apenas despesas do mês atual"; o código usa o mês de referência. Prevalece o código (alinhado à memória de produto). | `docs/metas-orcamento.md` × `providers.dart` | — |

## Cofrinhos e movimentações (BOX)

| ID | Classe | Descoberta | Evidência | Teste |
|---|---|---|---|---|
| DESC-BOX-01 | CONFIRMADO | `MoneyBox`: `id`, `name`, `description?`, `icon`, `color`, `goal` (0 = sem meta), `createdAt`, `currencyCode`; legados `bank` e `balance`. | `money_box.dart` | `currency_persistence_test` |
| DESC-BOX-02 | CONFIRMADO | `MoneyEntry`: `id`, `moneyBoxId`, `amount` (>0 por assert), `isWithdrawal`, `note?`, `date`, `createdAt`, `currencyCode`; `bank` legado. | `money_entry.dart` | `money_box_service_test` |
| DESC-BOX-03 | CONFIRMADO | Saldo do cofrinho = Σ depósitos − Σ saques, derivado e nunca persistido. Total de reservas = mesma soma filtrada pela moeda global. Reservas não dependem do mês. | `money_box_service.dart::balanceOf`; `providers.dart::balanceOfBoxProvider, totalReservesProvider` | `money_box_service_test` |
| DESC-BOX-04 | CONFIRMADO | Movimentação exige a mesma moeda do cofrinho (`ArgumentError`) e cofrinho existente (`Cofrinho não encontrado.`). | `money_box_service.dart::validateEntry`; `providers.dart::MoneyEntriesNotifier.save` | `money_box_service_test` |
| DESC-BOX-05 | CONFIRMADO | Não se altera a moeda de um cofrinho com movimentações (campo desabilitado + `StateError` no notifier). | `cofrinho_dialogs.dart`; `providers.dart::MoneyBoxesNotifier.save` | — |
| DESC-BOX-06 | CONFIRMADO | Nenhum caminho impede que um saque exceda o saldo; há teste que espera saldo negativo (−150). O progresso da barra é limitado a 0..1. | `cofrinho_dialogs.dart::_EntryFormState._save`; `providers.dart::boxProgressProvider` | `money_box_service_test` |
| DESC-BOX-07 | CONFIRMADO | O campo de meta individual do formulário não tem validador nem máscara; `parseAmount` aceita `-`. Valor ≤ 0 equivale a sem meta (`hasGoal` falso). | `cofrinho_dialogs.dart::_CofrinhoFormState`, `money_box.dart::hasGoal` | — |
| DESC-BOX-08 | CONFIRMADO | Excluir cofrinho (com confirmação) remove todas as movimentações e depois o cofrinho, em duas operações sequenciais sem transação. | `cofrinhos_screen_web.dart::_delete` | — |
| DESC-BOX-09 | CONFIRMADO | Meta geral de reservas por moeda (`Budget.reserveGoal/eurReserveGoal`), editável por diálogo; independente da soma das metas individuais; progresso geral = total/meta limitado a 1; vazio = sem meta. | `cofrinho_dialogs.dart::_ReserveGoalForm`, `budget_repository.dart::setReserveGoal` | — |
| DESC-BOX-10 | CONFIRMADO | Tela web: KPIs (Saldo Atual, Meta Total, Progresso, nº de Reservas), barra geral quando há meta, grade de cartões só com cofrinhos da moeda global (ordem de criação), vazio: `Nenhum cofrinho ainda. Crie sua primeira reserva.`. | `cofrinhos_screen_web.dart` | — |
| DESC-BOX-11 | CONFIRMADO | Detalhe do cofrinho: saldo, meta, descrição, histórico (mais recentes primeiro; vazio: `Nenhum aporte ainda.`), editar/excluir movimentação (com confirmação), botões Aporte e Saque. A edição permite trocar depósito↔saque, valor, data e observação. | `cofrinho_dialogs.dart::_CofrinhoDetail, _EntryForm` | — |
| DESC-BOX-12 | CONFIRMADO | Nome do cofrinho obrigatório; "onde está guardado" opcional; 10 ícones e 12 cores. Movimentações não criam lançamentos nem alteram o saldo mensal. | `cofrinho_dialogs.dart` | — |
| DESC-BOX-13 | CONFIRMADO | Migração legada idempotente: `balance > 0` sem movimentações → depósito "Saldo inicial" (data = criação) e `balance = 0`; `bank` legado → `description` se vazia. Roda no boot (falhas engolidas) e após importar (sem proteção). | `money_box_migration.dart`; `main.dart`; `backup_service.dart` | — |

## Relatórios (REP)

| ID | Classe | Descoberta | Evidência | Teste |
|---|---|---|---|---|
| DESC-REP-01 | CONFIRMADO | Períodos: semana domingo–sábado, mês do dia 1 ao último, ano 1/jan–31/dez; `dayEnd` = 23:59:59.999. | `report_service.dart::currentWeek/currentMonth/currentYear` | `report_service_test` |
| DESC-REP-02 | CONFIRMADO | Os dados do relatório ancoram em `DateTime(ano, mês, 15)` do mês de referência: a "semana" é a que contém o dia 15; o ano é o do mês de referência. | `providers.dart::filteredTransactionsProvider` | — |
| DESC-REP-03 | CONFIRMADO | **Defeito candidato:** a tela web chama `groupBySubPeriod(txs, period, null, currency)`; com `ref` nulo as barras usam a semana/mês/ano **atuais**, enquanto os lançamentos já foram filtrados pelo período do mês de referência. Com o mês de referência diferente do atual, o gráfico de barras fica zerado ou incoerente com os KPIs. (O dashboard passa o mês corretamente.) Não executado. | `reports_screen_web.dart:33` × `report_service.dart::groupBySubPeriod` | `report_service_test` cobre só a contagem de barras |
| DESC-REP-04 | CONFIRMADO | Barras: semana 7 (Dom…Sáb); mês `ceil(últimoDia/7)` faixas S1…S5 de 7 dias (não semanas de calendário); ano 12 (Jan…Dez). | `report_service.dart::_groupBy*` | `report_service_test` (7 e 12) |
| DESC-REP-05 | CONFIRMADO | Pizza e ranking consideram só despesas; ranking = 7 maiores com % do total; categoria ausente = `Sem categoria`; vazio: `Sem despesas no período.`. | `reports_screen_web.dart::_Ranking`, `category_pie_chart.dart` | `report_service_test` (groupByCategoryId) |
| DESC-REP-06 | CONFIRMADO | "Resumo Mensal" só no período Ano: entradas, saídas, saldo e `% vs anterior = (saldo−anterior)/|anterior|·100`, `—` na primeira linha ou se anterior = 0. | `reports_screen_web.dart::_MonthlyTable` | — |
| DESC-REP-07 | CONFIRMADO | **Divergência documental:** `docs/relatorios.md` e `docs/metas-orcamento.md` falam em ranking de 5 categorias; o código usa 7. | docs × `_Ranking` | — |
| DESC-REP-08 | CONFIRMADO | O período (semana/mês/ano) começa em Mês e não é persistido. | `providers.dart::selectedPeriodProvider` | — |

## Backup (BKP)

| ID | Classe | Descoberta | Evidência | Teste |
|---|---|---|---|---|
| DESC-BKP-01 | CONFIRMADO | Exportação JSON versão 2: `{app:"finance_controller", version:2, categories, subCategories, transactions, budget, moneyBoxes, moneyEntries}`, indentado; datas em ISO-8601 local; arquivo `finance_controller_backup_AAAAMMDD_HHmm.json`. | `backup_service.dart::exportJson`; `backup_screen.dart::_export` | — |
| DESC-BKP-02 | CONFIRMADO | Importação substitui tudo. Antes de limpar valida: raiz é mapa com `categories` lista (`Arquivo de backup inválido.`), moedas conhecidas, movimentação com cofrinho existente e moeda compatível. | `backup_service.dart::importJson/_validateCurrencies` | — |
| DESC-BKP-03 | CONFIRMADO | **Risco:** depois da validação, as 6 boxes são limpas e só então os registros são convertidos e gravados. Campo ausente, tipo errado ou data inválida lança exceção **após** a limpeza → perda parcial sem rollback. A UI mostra `Arquivo inválido ou corrompido.` para qualquer exceção. | `backup_service.dart` (clear em 149–154) | — |
| DESC-BKP-04 | CONFIRMADO | A importação não valida: integridade lançamento→categoria e subcategoria→categoria, `amount > 0` fora do modo debug, unicidade de nome de categoria, nem o número de `version` (v1 sem moeda → BRL). `budget` só é importado se for mapa. | `backup_service.dart::importJson` | — |
| DESC-BKP-05 | CONFIRMADO | O diálogo de confirmação e a mensagem de sucesso citam categorias, lançamentos e metas/contagens, mas a importação também substitui cofrinhos e movimentações. | `backup_screen.dart::_import` | — |
| DESC-BKP-06 | CONFIRMADO | Backup automático (somente navegadores com File System Access API): escolhe uma pasta, grava `finance_controller_backup.json` (sobrescrito) e uma cópia diária `finance_controller_AAAA-MM-DD.json`; debounce de 3 s após qualquer mudança em lançamentos, categorias, meta, cofrinhos ou movimentações; permissão a reativar ao reabrir; desligável. | `providers.dart::AutoBackupController`; `web_shell.dart`; `auto_backup_io_web.dart` | — |
| DESC-BKP-07 | INFERIDO | `navigator.storage.persist()` é pedido no boot para evitar descarte do IndexedDB (comentário em `main.dart` e `auto_backup_io_web.dart`). | `main.dart` | — |

## Preferências, navegação e experiência (PRF / NAV / UX)

| ID | Classe | Descoberta | Evidência | Teste |
|---|---|---|---|---|
| DESC-PRF-01 | CONFIRMADO | Tema `light`/`dark`/`system` persistido (`theme_mode`); padrão `system`; "alternar" vai de dark para light e de qualquer outro para dark. | `providers.dart::ThemeModeNotifier` | — |
| DESC-PRF-02 | CONFIRMADO | Locale fixo `pt_BR`; datas `dd/MM/yyyy`; mês `Junho 2026`. | `date_extension.dart`, `main.dart` | — |
| DESC-NAV-01 | CONFIRMADO | Rotas: `/`, `/transactions`, `/transactions/new`, `/transactions/:id/edit`, `/categories`, `/categories/new`, `/categories/:id/edit`, `/reports`, `/budget`, `/backup-data`, `/cofrinhos`. Na web, criar/editar lançamento, categoria e cofrinho usa diálogos; as rotas `new/edit` são do mobile. | `router.dart`; `AGENTS.md` | — |
| DESC-NAV-02 | CONFIRMADO | Menu lateral fixo na web: Dashboard, Lançamentos, Relatórios, Categorias, Cofrinhos, Meta, Backup; seletor de moeda; alternância de tema; rótulo `v1.0.0`; conteúdo com largura máxima. | `web_shell.dart` | — |
| DESC-UX-01 | CONFIRMADO | Feedback por snackbar de sucesso (esmeralda), erro (coral) e aviso (âmbar); exclusões pedem confirmação; estados vazios com ícone e mensagem. | `feedback.dart`, `confirm_delete_dialog.dart`, `empty_state.dart` | — |
| DESC-UX-02 | CONFIRMADO | Tema Neon Graphite: income `#34D399`, expense `#FB7185`, warning `#FBBF24`, primary `#0F172A`, accent `#22D3EE`, surface `#F1F5F9`, surfaceDark `#0B1120`; fonte Inter. | `app_colors.dart`, `AGENTS.md` | — |

## Dados, infraestrutura e testes (DAT / INF / TST)

| ID | Classe | Descoberta | Evidência | Teste |
|---|---|---|---|---|
| DESC-DAT-01 | CONFIRMADO | Persistência local em Hive (IndexedDB na web), isolada por origem (porta incluída). Sem servidor, sem autenticação e sem multiutilizador. | `hive_service.dart`, `docs/moedas-e-pwa.md` | — |
| DESC-DAT-02 | CONFIRMADO | Seis coleções: `categories`, `subcategories`, `transactions`, `budget`, `money_boxes`, `money_entries`; relações por `id` sem garantia de integridade do armazenamento. | `app_constants.dart`, `hive_service.dart` | — |
| DESC-DAT-03 | CONFIRMADO | Valores monetários em `double` (binário), sem arredondamento explícito; somas por `fold<double>`. | `transaction.dart`, `report_service.dart` | — |
| DESC-DAT-04 | CONFIRMADO | Datas como `DateTime` local, com hora em lançamentos criados "agora" e 00:00 quando escolhidas no seletor; o mês vem dos componentes locais; sem fuso. | `transaction_form_dialog.dart`, `providers.dart` | — |
| DESC-INF-01 | CONFIRMADO | PWA: manifest, service worker com cache versionado e fallback offline; instalação diária em `localhost:5599`, desenvolvimento em `8090`. | `web/manifest.json`, `web/sw.js`, `docs/moedas-e-pwa.md` | — |
| DESC-INF-02 | CONFIRMADO | Scripts PowerShell: `build-pwa.ps1` (limpeza, dependências, adapters, análise, testes, build), `serve-build.ps1`, `reconstruir.ps1`, `gerar_icones.ps1`. | `tools/` | — |
| DESC-INF-03 | CONFIRMADO | Sem CI/CD (sem `.github` na raiz), sem variáveis de ambiente, sem chamadas de rede, sem logging estruturado ou telemetria (busca por `http`, `print`, `debugPrint`, `Logger` sem ocorrências). | busca em `lib/` | — |
| DESC-TST-01 | CONFIRMADO | Sete ficheiros de teste, todos de lógica pura ou serialização: validators, currency_extension, transaction, currency_persistence, budget_service, money_box_service, report_service. Não há testes de widget, repositórios, backup, migração, providers nem E2E. Execução não realizada. | `test/` | — |
