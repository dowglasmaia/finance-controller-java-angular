# 15-backend-modulo-categorias

> **ID:** BACKEND-SPEC-10 · **Categoria:** Backend (módulo `category`) · **Estado:** concluída: executada na F4 (2026-10-29) e fechada na F5 (2026-10-29), quando existiram lançamentos reais — ver evidências.

## Objetivo

Implementar o módulo de categorias e subcategorias, incluindo a semente inicial, com as regras de unicidade, proteção de "Outros" e bloqueio de exclusão da origem.

## Escopo

- Inclui: operações `listCategories`, `createCategory`, `getCategory`, `updateCategory`, `deleteCategory`, `addSubcategory`, `deleteSubcategory`; semente de 11 categorias; SPI `CategoryUsageGuard`.
- Não inclui: lançamentos (BACKEND-SPEC-11), metas por categoria (BACKEND-SPEC-13), interface (FRONTEND-SPEC-11).

## Contexto técnico local

### Regras aplicadas (BACKEND-SPEC-01)

RN-CAT-01…11 (decididas: P-11 desassocia lançamentos ao excluir subcategoria; P-12 sem limite de categorias; P-13 bloqueia mudar o tipo com lançamentos incompatíveis) e RN-OWN (cada utilizador tem as suas categorias, com nome único por utilizador). Evidências: DESC-CAT-01…06, DESC-SUB-01…04, DESC-SEE-01.

### Fluxos

- **Criar:** valida nome (obrigatório, único ignorando caixa/espaços), tipo, ícone, cor → cria categoria e "Outros" (+ subcategorias informadas, ignorando "Outros" duplicada) numa transação → 201.
- **Atualizar:** nome/tipo/ícone/cor; unicidade excluindo o próprio id. Alterar o tipo com lançamentos incompatíveis é recusado com `CATEGORY_TYPE_CONFLICT` (a origem permitia — DESC-CAT-06; P-13). Alterar a categoria de um lançamento é livre e os totais, sendo derivados, acompanham.
- **Excluir:** `CategoryUsageGuard.isInUse(id)`; se em uso → `CATEGORY_IN_USE`; senão remove categoria e subcategorias (cascata).
- **Subcategoria:** adicionar valida nome não vazio e unicidade na categoria; excluir recusa "Outros" (`SUBCATEGORY_PROTECTED`); com lançamentos vinculados, desassocia-os (ficam sem subcategoria — P-11). Não há renomear na origem (DESC-SUB-01); manter.
- **Semente:** 11 categorias (despesa: Carro, Alimentação, Moradia, Saúde, Lazer, Educação, Vestuário; receita: Salário, Vale Alimentação, Investimento, Bônus), cada uma com as subcategorias de `seed_service.dart::_data` e "Outros"; ícones e cores da origem; idempotente; executada no **cadastro de cada utilizador** (BACKEND-SPEC-05), na mesma transação.

### Dados de semente (DESC-SEE-01)

| Categoria | Tipo | Ícone | Subcategorias (além de "Outros" em todas) |
|---|---|---|---|
| Carro | EXPENSE | `directions_car` | Combustível, Manutenção, Seguro, Multas |
| Alimentação | EXPENSE | `restaurant` | Supermercado, Restaurante, Delivery, Padaria |
| Moradia | EXPENSE | `home` | Aluguel, Condomínio, Energia, Água, Internet |
| Saúde | EXPENSE | `favorite` | Médico, Farmácia, Plano de Saúde, Academia |
| Lazer | EXPENSE | `sports_esports` | Cinema, Viagem, Streaming, Esporte |
| Educação | EXPENSE | `school` | Curso, Livros, Assinatura |
| Vestuário | EXPENSE | `checkroom` | Roupas, Calçados, Acessórios |
| Salário | INCOME | `work` | CLT, PJ, Freelance |
| Vale Alimentação | INCOME | `lunch_dining` | VA, VR |
| Investimento | INCOME | `trending_up` | Dividendos, Renda Fixa, FIIs |
| Bônus | INCOME | `card_giftcard` | PLR, Comissão |

Cores: Carro laranja, Alimentação verde, Moradia azul, Saúde vermelho, Lazer roxo, Educação índigo, Vestuário marrom, Salário teal, Vale Alimentação verde-claro, Investimento âmbar, Bônus ciano (`Colors.*` do Flutter; conversão para `#RRGGBB` na implementação — GLOBAL-SPEC-02).

### Riscos

Ordenação alfabética sem distinguir caixa usa o `collation` do banco; fixar para evitar diferenças por ambiente.

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [BACKEND-SPEC-01](../07-backend-dominio-e-regras-de-negocio/spec.md)
- [BACKEND-SPEC-03](../09-backend-api-rest-e-openapi/spec.md)

## Regras e critérios de aceitação

- [x] Criar categoria com nome já existente em outra caixa (`alimentação`) devolve 409 `CATEGORY_NAME_DUPLICATED`; duas criações concorrentes com o mesmo nome resultam em uma criação e um 409.
  > ✅ 2026-10-29 10:10 +00:00 — `CategoryFlowIntegrationTest.duplicateNamesAreRejectedIgnoringCaseAndSpaces` (`  ALIMENTAÇÃO ` → 409 `CATEGORY_NAME_DUPLICATED`) e `concurrentCreationsWithTheSameNameYieldOneCreatedAndOneConflict` (duas threads: um 201 e um 409), com PostgreSQL real; unicidade garantida por `uk_category_owner_name` sobre a coluna gerada `lower(btrim(name))`.
- [x] Toda categoria criada tem exatamente uma subcategoria "Outros".
  > ✅ 2026-10-29 10:10 +00:00 — `CategoryTests` (inclui `outros` e ` OUTROS ` informados, ignorados) e `createAlwaysAddsProtectedOthersAndListIsSortedAndFiltered` (HTTP, PostgreSQL).
- [x] `DELETE` de "Outros" devolve 409 `SUBCATEGORY_PROTECTED`.
  > ✅ 2026-10-29 10:10 +00:00 — `subcategoriesCanBeAddedAndRemovedButOthersIsProtected` (409 `SUBCATEGORY_PROTECTED`, nada removido).
- [x] `DELETE` de categoria com lançamentos devolve 409 `CATEGORY_IN_USE` e nada é removido; sem lançamentos remove também as subcategorias.
  > ⏳ 2026-10-29 10:10 +00:00 — Parcial: `CategoryUseCaseTests.deleteIsBlockedWhenInUseAndRemovesEverythingOtherwise` prova o bloqueio e a remoção com a porta `CategoryUsageGuard` simulada; `deleteRemovesCategoryAndItsSubcategories` prova a remoção sem uso (cascata do banco). Falta: bloqueio com lançamentos reais — a implementação padrão (`NoTransactionsCategoryUsageGuard`) responde "sem uso" até o módulo de lançamentos (F5). Próxima ação: na F5, implementar `CategoryUsageGuard` sobre `transaction`, remover a implementação padrão e testar com lançamentos.
  > ✅ 2026-10-29 10:50 +00:00 — Fechado na F5: `TransactionFlowIntegrationTest.categoryWithTransactionsCannotBeDeletedAndTypeChangeIsRefusedWhenIncompatible` (PostgreSQL real: 409 `CATEGORY_IN_USE`, categoria intacta; sem lançamentos, 204). `TransactionCategoryUsageGuard` substituiu a implementação provisória, que foi removida; corrida entre excluir e lançar traduzida por `fk_transaction_entry_category`. Verificado também na interface contra a API real.
- [x] `listCategories?type=EXPENSE` inclui `EXPENSE` e `BOTH`; ordem alfabética sem distinguir caixa.
  > ✅ 2026-10-29 10:10 +00:00 — `CategoryUseCaseTests.listFiltersByAcceptedTypeIncludingBothAndSortsIgnoringCaseAndAccents` e `createAlwaysAddsProtected…` (HTTP). Ordem fixada no Java (`Collator` pt-BR, força primária), independente do collation do banco — resolve o risco da spec.
- [x] Ao cadastrar um utilizador existem 11 categorias dele (7 despesa, 4 receita) com as subcategorias da tabela; outro utilizador não as vê.
  > ✅ 2026-10-29 10:10 +00:00 — `registrationSeedsElevenCategoriesAndOtherUsersDoNotSeeThem` (11; 7 despesa e 4 receita; outro utilizador recebe as suas, os ids de um dão 404 ao outro) e `seedCreatesTheElevenDefaultsAndIsIdempotent` (subcategorias da tabela, idempotente). Verificado também com a API real: cadastro → `GET /categories` = 11 (7+4); bytes UTF-8 de "Água" e "Condomínio" conferidos no banco. Semente por evento `identity.UserRegistered`, síncrono e na mesma transação (`identity` não depende de `category`).
- [x] Dois utilizadores podem ter categorias com o mesmo nome.
  > ✅ 2026-10-29 10:10 +00:00 — `sameNameIsAllowedForAnotherOwner` e a própria semente (dois utilizadores com "Alimentação") com PostgreSQL.
- [x] Mudar o tipo de uma categoria com lançamentos incompatíveis devolve 409 `CATEGORY_TYPE_CONFLICT`.
  > ⏳ 2026-10-29 10:10 +00:00 — Parcial: `typeChangeIsRefusedWhenTransactionsAreIncompatible` (porta simulada, estado preservado). Falta: lançamentos reais (F5), mesma porta `CategoryUsageGuard.hasTransactionsIncompatibleWith`.
  > ✅ 2026-10-29 10:50 +00:00 — Fechado na F5: mesmo teste de integração (despesa → receita com despesas: 409 `CATEGORY_TYPE_CONFLICT`, tipo preservado; `BOTH` continua aceito).
- [x] Criar uma 51.ª categoria é permitido (sem limite).
  > ✅ 2026-10-29 10:10 +00:00 — `allowsMoreThanFiftyCategories` (51 criadas).
- [x] Excluir subcategoria com lançamentos mantém os lançamentos, agora sem subcategoria.
  > ⏳ 2026-10-29 10:10 +00:00 — Falta: depende do módulo de lançamentos (F5). Hoje a exclusão de subcategoria funciona sem lançamentos (`DeleteSubcategoryUseCase`); a desassociação será acrescentada na F5.
  > ✅ 2026-10-29 10:50 +00:00 — Fechado na F5: `deletingASubcategoryKeepsTheTransactionWithoutIt` (PostgreSQL real: 204, o lançamento continua e sai sem `subcategory`). Mecanismo: `ON DELETE SET NULL` em `transaction_entry.subcategory_id` (V3), sem código extra.

### Cenários de teste

Unicidade (caixa, espaços, concorrência); "Outros" (excluir, duplicar, renomear); exclusão com/sem lançamentos; filtro por tipo; semente idempotente; integração com `ModularityTests` (sem dependência de `transaction`).

### Skills de implementação

- **Skills obrigatórias:** `java-modern-engineering`, `java-spring-create-module`, `java-persistence-performance`; `openapi-rest-contract` para o contrato do módulo.
- **Caminhos:** `C:\Users\dowgl\.codex\skills\java-modern-engineering\SKILL.md`; `C:\Users\dowgl\.codex\skills\java-spring-create-module\SKILL.md`; `C:\Users\dowgl\.codex\skills\openapi-rest-contract\SKILL.md`; `.claude/skills/java-persistence-performance/SKILL.md`.
- **Responsabilidade:** contrato `categories`; implementação das 7 operações; modelagem Java.
- **Momento de utilização:** contrato primeiro, depois implementação, quando o módulo for selecionado.
- **Dependências:** BACKEND-SPEC-00/01/02/03/04/05.
- **Limitações:** nenhuma específica.

## Tasks

### Contrato e dados

- [x] Aprovar a prévia e gerar o contrato `categories`.
  > ✅ 2026-10-29 10:10 +00:00 — Prévia (7 operações, schemas e erros) aprovada pelo utilizador em conversa e gerada em `openapi/modules/categories.yaml` + execução Maven `generate-categories-api`. Verificação: `validate.mjs` (OpenAPI 3.1 válido), `mvn generate-sources` gerou `CategoriesApi` e modelos isolados, sem `operationId` repetido. Desvio: o gerador da skill não aceita `$ref` para schemas locais, então o YAML foi escrito à mão no estilo do `identity.yaml` e validado com o validador da skill.
- [x] Migrações `category` e `subcategory` e semente.
  > ✅ 2026-10-29 10:10 +00:00 — `V2__category.sql` (`category`, `subcategory`, `owner_id` com FK e cascata, unicidade por coluna gerada, CHECK de tipo e cor) e `SeedDefaultCategoriesUseCase`. `IdentityMigrationTests.migrationsApplyFromScratchInEmptySchema` aplica V1+V2 do zero (versão 2).

### Back-end

- [x] Implementar domínio, casos de uso, adaptador e controller das 7 operações e a SPI.
  > ✅ 2026-10-29 10:10 +00:00 — Pacote `category` (domínio, 7 casos de uso + semente, adaptador JPA com filtro por `owner_id`, `CategoriesController`, SPI `CategoryUsageGuard`). `ModularityTests` verde (`category` não depende de `transaction`). Também removido o módulo `example` (código, testes, `openapi.yaml` raiz e a execução Maven dele), previsto para a F4.

### Validação

- [x] Executar os testes de unicidade, exclusão e semente com PostgreSQL real.
  > ✅ 2026-10-29 10:10 +00:00 — `./mvnw clean verify` com Docker: 181 testes, 0 falhas (`CategoryFlowIntegrationTest` 8, `CategoryUseCaseTests` 10, `CategoryTests` 6). API real com PostgreSQL 18.1 verificada (semente e CRUD pela interface).

## Resultado esperado

- Gestão de categorias equivalente à origem, com unicidade garantida no banco.
- Preservada a independência: `category` não depende de `transaction`.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
