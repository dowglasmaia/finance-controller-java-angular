# 08-backend-persistencia-e-migracoes

> **ID:** BACKEND-SPEC-02 · **Categoria:** Backend · **Estado:** decisões principais aprovadas (P-01, P-07, P-17) — não executada.

## Objetivo

Especificar o modelo relacional PostgreSQL, com isolamento por utilizador, as restrições de integridade, as consultas otimizadas com JPA e as migrações Flyway que substituem as seis coleções Hive da origem.

## Escopo

- Inclui: tabelas, colunas, restrições, índices, estratégia de consultas, ordem das migrações.
- Não inclui: provisionamento do banco (INFRA-SPEC-02), consultas funcionais de cada módulo (specs 15–20), SQL final. **Não há migração de dados da origem** (P-16).

## Contexto técnico local

### Decisões aplicadas

PostgreSQL na versão estável corrente à data da implementação; **Flyway**; **JPA/Hibernate**; dinheiro `NUMERIC(19,2)`; isolamento por `owner_id` (P-01).

### Do Hive para o PostgreSQL

| Coleção Hive (DESC-DAT-02) | Tabela | Observações |
|---|---|---|
| — | `app_user`, `refresh_token`, `password_reset_token` | novas (identidade e recuperação de senha) |
| `categories` | `category` | sem `subCategoryIds` |
| `subcategories` | `subcategory` | |
| `transactions` | `transaction_entry` | evita nome reservado |
| `budget` (singleton) | `budget_setting`, `budget_month_override`, `budget_category_limit` | normalizado por moeda e por proprietário |
| (dentro de `budget`) | `reserve_goal` | meta geral por moeda |
| `money_boxes` | `money_box` | sem `bank`/`balance` legados |
| `money_entries` | `money_box_entry` | sem `bank` legado |
| `SharedPreferences` | `preference` | por utilizador (P-19) |

### Esquema (tipos lógicos; **todas** as tabelas de dados têm `owner_id uuid NOT NULL` com FK para `app_user`)

| Tabela | Colunas principais | Restrições e índices |
|---|---|---|
| `app_user` | `id`, `email` (guardado em minúsculas), `password_hash`, `created_at` | `UNIQUE(email)` |
| `refresh_token` | `id`, `user_id`, `token_hash`, `family_id`, `expires_at`, `revoked_at` | índice em `token_hash`; `family_id` para revogação |
| `password_reset_token` | `id`, `user_id` FK CASCADE, `token_hash` (SHA-256), `expires_at`, `used_at`, `created_at` | `UNIQUE(token_hash)`; limpeza periódica dos expirados (BACKEND-SPEC-18) |
| `category` | `id`, `owner_id`, `name varchar(60)`, `type`, `icon varchar(40)`, `color char(7)`, `created_at` | `UNIQUE(owner_id, lower(btrim(name)))`; `CHECK type` |
| `subcategory` | `id`, `owner_id`, `category_id` FK CASCADE, `name varchar(60)` | `UNIQUE(category_id, lower(btrim(name)))` |
| `transaction_entry` | `id`, `owner_id`, `kind`, `description varchar(120)`, `amount numeric(19,2)`, `currency`, `entry_date date`, `category_id` FK **RESTRICT**, `subcategory_id` FK **SET NULL** (P-11), `notes varchar(500)`, `created_at`, `updated_at` | `CHECK amount > 0`; índices `(owner_id, currency, entry_date)` e `(owner_id, category_id)`; FKs compostas com `owner_id` garantem que categoria e subcategoria pertencem ao mesmo proprietário |
| `budget_setting` | `owner_id`, `currency`, `monthly_limit` | PK `(owner_id, currency)`; `CHECK >= 0` |
| `budget_month_override` | `owner_id`, `currency`, `year_month char(7)`, `amount numeric(19,2) NULL`, `disabled boolean` | PK `(owner_id, currency, year_month)`; `CHECK (disabled OR amount > 0)`; `disabled` = "sem meta neste mês" (P-06) |
| `budget_category_limit` | `owner_id`, `currency`, `category_id` FK CASCADE, `amount` | PK `(owner_id, currency, category_id)`; `CHECK amount > 0` |
| `reserve_goal` | `owner_id`, `currency`, `amount` | PK `(owner_id, currency)`; `CHECK >= 0` |
| `money_box` | `id`, `owner_id`, `name varchar(60)`, `description varchar(120)`, `icon`, `color`, `goal numeric(19,2)`, `currency`, `created_at` | `CHECK goal >= 0`; `UNIQUE(id, currency)` |
| `money_box_entry` | `id`, `owner_id`, `money_box_id`, `currency`, `kind`, `amount`, `entry_date`, `note varchar(200)`, `created_at` | FK composta `(money_box_id, currency)` → `money_box(id, currency)` `ON DELETE CASCADE`; `CHECK amount > 0`; índice `(money_box_id, entry_date)` |
| `preference` | `owner_id` PK, `currency`, `theme` | `CHECK` nos valores |

Saldos, totais e progressos **não** têm coluna (RN-TXN-07, RN-BOX-02). Saldo de cofrinho nunca negativo (P-04) é regra de aplicação, protegida contra concorrência (ver abaixo).

### JPA e consultas otimizadas (P-17)

- Entidades JPA apenas em `infrastructure`; o domínio é separado (BACKEND-SPEC-00). Relações `LAZY`; sem N+1.
- Agregações no banco, não em memória: somas por mês/moeda/tipo, por categoria, por faixa de dias, saldo de cofrinho (`SUM` com `CASE`), paginação com `Pageable`.
- Projeções e consultas dedicadas por caso de uso (JPQL ou SQL nativo quando mais eficiente), com `EXPLAIN` nos três casos quentes: `listTransactions`, `getMonthlyOverview`, `getReport(YEAR)`.
- Índices guiados por essas consultas; revisão quando surgirem novas.
- `owner_id` sempre no predicado; um filtro central impede esquecê-lo (ver BACKEND-SPEC-05).
- Concorrência em saque: bloquear o cofrinho (`SELECT … FOR UPDATE`/bloqueio otimista) antes de verificar o saldo e gravar.

### Migrações (Flyway)

`V1` identidade; `V2` categorias e subcategorias; `V3` lançamentos; `V4` metas e meta de reservas; `V5` cofrinhos e movimentações; `V6` preferências. Scripts imutáveis; a semeadura de categorias é rotina do cadastro (ADR-012), não migração. Testes com PostgreSQL real (Testcontainers).

### Decisões e riscos

Isolamento depende de todos os repositórios filtrarem por `owner_id`: coberto por teste de isolamento por recurso. A conversão de dados legados da origem **não existe** (P-16).

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [BACKEND-SPEC-01](../07-backend-dominio-e-regras-de-negocio/spec.md)
- [BACKEND-SPEC-05](../11-backend-autenticacao-e-seguranca/spec.md)
- [INFRA-SPEC-02](../40-infra-banco-de-dados/spec.md)

## Regras e critérios de aceitação

- [ ] Todas as restrições `CHECK`, `UNIQUE` e FK acima existem e são exercitadas por testes de integração.
- [ ] Dois utilizadores podem ter categorias com o mesmo nome; o mesmo utilizador não (inclusive sob concorrência → `CATEGORY_NAME_DUPLICATED`).
- [ ] Não é possível referenciar categoria, subcategoria ou cofrinho de outro proprietário.
- [ ] Excluir subcategoria com lançamentos deixa os lançamentos sem subcategoria.
- [ ] Nenhuma coluna armazena saldo ou total derivado.
- [ ] As migrações aplicam-se do zero num PostgreSQL real.
  > ⏳ 2026-10-27 — Parcial: `V1__identity.sql` aplicada do zero em PostgreSQL 18.1 (Testcontainers em `IdentityMigrationTests`, 7 testes; e no `compose`). Verificação: `./mvnw clean verify` com Docker, 159 testes, BUILD SUCCESS. Falta: V2…V6 (F4–F10).
- [ ] As três consultas quentes têm plano com uso de índice e sem N+1 comprovado por teste de contagem de consultas.
- [ ] Dois saques simultâneos que juntos excederiam o saldo: apenas um é aceito.

### Skills de implementação

- **Skills obrigatórias:** `java-modern-engineering`, `java-spring-create-module`.
- **Caminhos:** `C:\Users\dowgl\.codex\skills\java-modern-engineering\SKILL.md`; `C:\Users\dowgl\.codex\skills\java-spring-create-module\SKILL.md`.
- **Responsabilidade:** adaptadores de persistência dentro de cada módulo.
- **Momento de utilização:** junto de cada módulo, após as migrações.
- **Dependências:** P-17 (decidida), BACKEND-SPEC-05; INFRA-SPEC-02.
- **Limitações:** **não há skill nos diretórios indicados para modelagem de banco, Flyway ou PostgreSQL**; nenhuma foi simulada.

## Tasks

### Descoberta e decisão

- [ ] Fixar a versão estável do PostgreSQL e do Flyway na data da implementação.

### Contrato e dados

- [ ] Criar as migrações V1…V6 conforme o esquema.
  > ⏳ 2026-10-27 — Parcial: V1 (`app_user`, `refresh_token`; sem `password_reset_token`, que é da F11) criada e testada. Falta: V2…V6, criadas com o módulo que as usa.

### Back-end

- [ ] Implementar entidades JPA, mapeamentos e consultas otimizadas nos módulos.

### Validação

- [ ] Executar migrações, restrições, isolamento e planos das consultas quentes; registar o resultado.

## Resultado esperado

- Esquema íntegro, versionado e isolado por utilizador, com consultas eficientes.
- Preservada a regra de calcular saldos e totais a partir dos registos de origem.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
