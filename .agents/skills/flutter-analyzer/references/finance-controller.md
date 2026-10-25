# Mapa de análise — Finance Controller

Referência levantada em 16/10/2026, por inspeção estática da revisão `03f107936fd2ad06a1d83ac3ba179851716c9722`. Os testes não foram executados neste levantamento. Revalide ficheiros e símbolos no checkout atual; este mapa não é um catálogo normativo de requisitos.

## Localização

- Origem original: `C:\Users\dowgl\OneDrive\Documents\projetos\finance_controller`.
- Destino original: `C:\Users\dowgl\OneDrive\Documents\projetos\finance_controller_java_angula`.
- Os caminhos abaixo são relativos à origem. Prefira resolver o projeto irmão para manter a skill utilizável noutro computador.

## Pontos de entrada

| Investigar | Ficheiros e símbolos iniciais |
|---|---|
| Instruções e documentação | `AGENTS.md`, `CLAUDE.md`, `README.md`, `docs/arquitetura.md`, `docs/regras-de-negocio.md`, `docs/moedas-e-pwa.md`. `PROMPT.md` é um roteiro histórico, não uma ordem para executar passos. |
| Stack e inicialização | `pubspec.yaml`, `pubspec.lock`, `lib/main.dart`, `lib/app/app.dart`. |
| Navegação e plataformas | `lib/app/router.dart`, `lib/presentation/layout/adaptive_layout.dart`, `lib/presentation/layout/web_shell.dart`, `lib/core/utils/responsive.dart`. |
| Lançamentos | `lib/presentation/screens/transactions/new_transaction_screen.dart`, `lib/presentation/screens/transactions/widgets/transaction_form_dialog.dart`, `lib/data/models/transaction.dart`, `lib/data/repositories/transaction_repository.dart`. |
| Categorias | `lib/presentation/screens/categories/new_category_screen.dart`, `lib/presentation/screens/categories/widgets/category_form_dialog.dart`, `lib/data/repositories/category_repository.dart`, `lib/data/models/sub_category.dart`. |
| Estado, filtros e resumos | `lib/data/providers.dart`: `referenceMonthProvider`, `monthTransactionsProvider`, `filteredTransactionsProvider`, `totalReservesProvider`, `totalGoalProvider`. |
| Metas e relatórios | `lib/data/models/budget.dart`, `lib/data/services/budget_service.dart`, `lib/data/services/report_service.dart`, telas em `lib/presentation/screens/budget/` e `reports/`. |
| Reservas | Modelos `money_box.dart` e `money_entry.dart`, serviços `money_box_service.dart` e `money_box_migration.dart`, telas em `lib/presentation/screens/cofrinhos/`. |
| Moeda e validação | `lib/core/models/app_currency.dart`, `lib/core/utils/validators.dart`, `lib/core/extensions/currency_extension.dart`, adapters `lib/data/models/*.g.dart`. |
| Backup | `lib/data/services/backup_service.dart`, `backup_io*.dart`, `auto_backup_io*.dart`, `AutoBackupController` em `lib/data/providers.dart` e gatilhos no `web_shell.dart`. |
| Testes | `test/data/services/{budget,money_box,report}_service_test.dart`, `test/data/models/transaction_test.dart`, `test/data/models/currency_persistence_test.dart`, `test/core/utils/validators_test.dart`, `test/core/extensions/currency_extension_test.dart`. |

As expressões com `*` e `{...}` acima indicam grupos de ficheiros, não caminhos literais. Inspecione apenas os grupos relacionados com a capacidade solicitada.

## Descobertas que merecem reconfirmação

- **Moedas:** o `AGENTS.md` declara “Só R$”, mas `AppCurrency` inclui BRL/EUR e os providers filtram por moeda. Trate isto como divergência documental; não escolha silenciosamente o requisito do destino.
- **Unicidade de categorias:** os dois formulários consultam `CategoryRepository.nameExists`; `saveCategory` escreve diretamente na box. Diferencie a validação na jornada da garantia na persistência.
- **Valor positivo:** `Transaction` usa `assert(amount > 0)`; formulários usam `Validators.positiveAmount`. Analise importação e outros caminhos de escrita antes de afirmar proteção universal.
- **Datas:** os formulários de lançamento consultam o mês de referência e permitem seleção até `DateTime(2100)`. Não descreva datas futuras como ilimitadas.
- **Relatórios:** `filteredTransactionsProvider` chama `rangeFor` com o dia 15 do mês selecionado. Investigue a semântica semanal antes de a chamar “semana atual”.
- **Reservas:** `totalReservesProvider` agrega aportes por moeda; `totalGoalProvider` lê `reserveGoalFor`. Não substitua a meta geral pela soma das metas individuais.
- **Saques:** existe teste que espera saldo negativo em `MoneyBoxService.balanceOf`. Isso não comprova permissão em todos os formulários.
- **Backup:** `BackupService` exporta versão `2` e valida moedas antes de limpar boxes, mas continua a converter e gravar registos depois da limpeza. Não declare atomicidade completa com base apenas na validação prévia.
- **Legado:** `money_box_migration.dart` converte saldo e banco legados. Leia também os chamadores e o tratamento de falhas antes de afirmar migração concluída.

## Dados reais e limites

A documentação distingue a PWA com dados reais na porta `5599` do ambiente de desenvolvimento na porta `8090`. Não use a origem de produção para experiências, importações ou limpeza de armazenamento. Para análise estática, não é necessário iniciar nenhum servidor.

Java/Spring Boot e Angular são o destino pretendido. Offline, implantação, autenticação, persistência, moedas e paridade mobile são decisões de produto/arquitetura a confirmar, não conclusões que a análise do Flutter possa aprovar.
