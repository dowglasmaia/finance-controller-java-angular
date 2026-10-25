# 04-global-dominio-e-glossario

> **ID:** GLOBAL-SPEC-02 · **Categoria:** Global · **Estado:** proposta para revisão — não aprovada, não executada.

## Objetivo

Fixar o modelo conceitual (entidades, atributos, valores e enumerações) e o glossário compartilhados por backend, frontend e contratos, para que todos usem os mesmos termos e tipos.

## Escopo

- Inclui: catálogo de entidades e enumerações, glossário, convenções de nomes.
- Não inclui: regras e algoritmos (BACKEND-SPEC-01), tabelas SQL (BACKEND-SPEC-02) e campos de API (BACKEND-SPEC-03).

## Contexto técnico local

### Glossário

O glossário vigente está na [memória de produto](../../memory/produto.md#linguagem-comum-do-time) e não é repetido. Acréscimos desta análise:

| Termo | Significado |
|---|---|
| Ajuste mensal (`monthOverride`) | Limite global de um mês específico; substitui o recorrente se for > 0 (DESC-BUD-02). |
| Estado da meta (`BudgetStatus`) | `NONE`, `OK`, `WARNING`, `EXCEEDED` (DESC-BUD-04). |
| Alerta de meta | Aviso único ao salvar uma despesa que piora o estado da meta (DESC-BUD-06/07). |
| Âncora do relatório | Data que define o período do relatório; na origem, o dia 15 do mês de referência (DESC-REP-02). |
| Movimentação de reserva | Depósito ou saque de um cofrinho (`MoneyEntry` na origem). |

### Entidades (nomes de domínio em inglês, rótulos de UI em português)

| Entidade | Atributos de domínio | Notas | Origem |
|---|---|---|---|
| `Category` | id, name, type, icon, color, createdAt | `type` ∈ {INCOME, EXPENSE, BOTH}; possui subcategorias | DESC-CAT-01 |
| `Subcategory` | id, categoryId, name | "Outros" protegida | DESC-SUB-01 |
| `Transaction` | id, kind (INCOME/EXPENSE), description, amount, currency, date, categoryId, subcategoryId?, notes?, createdAt, updatedAt | `amount` > 0; direção em `kind` | DESC-TXN-01 |
| `BudgetSettings` (por moeda) | monthlyLimit, categoryLimits[categoryId→limit], monthOverrides[YYYY-MM→limit] | Sem meta quando o limite é 0/ausente | DESC-BUD-01 |
| `ReserveGoal` (por moeda) | amount | Independe das metas individuais | DESC-BOX-09 |
| `MoneyBox` | id, name, description?, icon, color, goal, currency, createdAt | Moeda imutável com movimentações | DESC-BOX-01/05 |
| `MoneyEntry` | id, moneyBoxId, kind (DEPOSIT/WITHDRAWAL), amount, date, note?, createdAt | Moeda = a do cofrinho | DESC-BOX-02/04 |
| `User` | id, email, passwordHash, createdAt | Proprietário de todos os dados; a senha nunca sai do servidor | P-01 |
| `Preferences` | ownerId, currency, theme | Guardada no servidor por utilizador (P-19) | DESC-PRF-01, DESC-CUR-03 |

Todas as entidades de dados (menos `User`) têm `ownerId` e só são visíveis ao seu proprietário (RN-OWN).

Campos legados `MoneyBox.bank`, `MoneyBox.balance` e `MoneyEntry.bank` **não** entram no modelo de domínio; existem apenas na leitura de backups antigos (DESC-BOX-01/02/13).

### Enumerações

| Enumeração | Valores | Valor na origem |
|---|---|---|
| `Currency` | `BRL`, `EUR` | `BRL`, `EUR` |
| `CategoryType` | `INCOME`, `EXPENSE`, `BOTH` | `income`, `expense`, `both` |
| `EntryKind` | `DEPOSIT`, `WITHDRAWAL` | `isWithdrawal` false/true |
| `TransactionKind` | `INCOME`, `EXPENSE` | `isIncome` true/false |
| `ReportPeriod` | `WEEK`, `MONTH`, `YEAR` | `PeriodType` |
| `BudgetStatus` | `NONE`, `OK`, `WARNING`, `EXCEEDED` | idem |
| `ThemeMode` | `LIGHT`, `DARK`, `SYSTEM` | `light`, `dark`, `system` |

### Convenções

- Identificadores: UUID. Na origem eram gerados no cliente (DESC-TXN-01); no destino o servidor os gera.
- A API usa os nomes de domínio acima; não há formato de backup da origem a preservar (P-03, P-16).
- Ícones guardados como nome textual do catálogo; cores como `#RRGGBB`.

### Riscos e pendências

- Catálogo de ícones: 24 na categoria, 10 no cofrinho (DESC-CAT-01, DESC-BOX-12); mantidos como dois conjuntos, renderizados com a biblioteca Lucide, com tabela de correspondência dos nomes Material da origem (P-22, ADR-013).
- Cores: ARGB com alfa `FF` na origem; guardadas como `#RRGGBB`.

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [Catálogo de descobertas](../00-engenharia-reversa/descobertas.md)

## Regras e critérios de aceitação

- [ ] Todo atributo e enumeração usados em contrato, banco e interface constam neste catálogo.
- [ ] Nenhum campo legado entra no modelo novo.
- [ ] Os nomes de domínio são idênticos no OpenAPI, no código Java e nos tipos TypeScript.

### Skills de implementação

- **Skills obrigatórias:** `java-modern-engineering`.
- **Caminhos:** `C:\Users\dowgl\.codex\skills\java-modern-engineering\SKILL.md`.
- **Responsabilidade:** modelagem do domínio (records, enums, invariantes) a partir deste catálogo.
- **Momento de utilização:** na implementação de BACKEND-SPEC-01.
- **Dependências:** BACKEND-SPEC-01.
- **Limitações:** nenhuma.

## Tasks

### Descoberta e decisão

- [ ] Montar a tabela de correspondência de ícones Material → Lucide e conferir a compatibilidade da biblioteca com Angular 21.

### Validação

- [ ] Rever o catálogo contra o contrato OpenAPI e o esquema de banco quando existirem.

## Resultado esperado

- Vocabulário e tipos únicos para todas as camadas.
- Preservados os termos da memória de produto.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
