# 17-backend-modulo-resumo-e-dashboard

> **ID:** BACKEND-SPEC-12 · **Categoria:** Backend (módulo `report`, consulta de visão geral) · **Estado:** proposta para revisão — não aprovada, não executada.

## Objetivo

Fornecer a consulta de visão geral mensal que alimenta o dashboard — saldo, entradas, saídas, variação, reservas, barras por faixa, despesas por categoria e últimos lançamentos — sem duplicar as regras dos módulos de origem.

## Escopo

- Inclui: `getMonthlyOverview` e a composição dos dados; a navegação por mês é parâmetro.
- Não inclui: relatórios por período (BACKEND-SPEC-15), progresso de metas (BACKEND-SPEC-13), interface (FRONTEND-SPEC-09).

## Contexto técnico local

### Composição (CONFIRMADO na origem; DESC-DSH-01/02, DESC-MES-02/03)

| Bloco | Cálculo |
|---|---|
| `summary` | entradas, saídas e saldo do mês e moeda (RN-SAL-01) |
| `previous` | mesmos totais do mês anterior |
| `incomeDeltaPercent`, `expenseDeltaPercent` | RN-SAL-02 (ausente sem base) |
| `reservesTotal` | saldo acumulado das reservas da moeda, independente do mês |
| `bars` | faixas de 7 dias S1…S5 do mês (RN-REP-03) |
| `expenseByCategory` | despesas do mês por categoria com percentual do total |
| `latestTransactions` | 8 mais recentes do mês, ordem de RN-TXN-08 |
| `budgetProgress` | progresso da meta global do mês e da moeda: `limit`, `spent`, `fraction`, `remaining`, `status` (P-14); `status = NONE` sem meta; fornecido pelo módulo `budget` |

### Decisões

- O módulo `report` consome `transaction`, `category` e `moneybox` por APIs públicas; não lê tabelas alheias (ADR-001).
- O cálculo de variação passa para o servidor (RT-01); o cliente só formata `+N%`.
- Progresso da meta no dashboard web: ausente na origem web (DESC-DSH-03), mas **incluído por decisão P-14**; o módulo `report` depende, portanto, de `budget`.
- A saudação por hora (DESC-DSH-01) é puramente de interface e fica no frontend.

### Riscos

Múltiplas consultas por chamada: medir (BACKEND-SPEC-08); se necessário, consolidar em consultas agregadas únicas com índice `(currency, entry_date)`.

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [BACKEND-SPEC-01](../07-backend-dominio-e-regras-de-negocio/spec.md)
- [FRONTEND-SPEC-09](../32-frontend-pagina-dashboard/spec.md)

## Regras e critérios de aceitação

- [ ] Para BRL +1000/−250 no mês, `summary` = 1000/250/750; lançamentos em EUR do mesmo mês não alteram nada.
- [ ] Mês anterior com entradas 0 → `incomeDeltaPercent` ausente; variação absoluta menor que 0,5% → `"0"`.
- [ ] `reservesTotal` não muda ao trocar o mês.
- [ ] `bars` tem `ceil(últimoDia/7)` itens; dia 29 de fevereiro cai em S5.
- [ ] `latestTransactions` tem no máximo 8 itens do mês e da moeda.
- [ ] Mês sem lançamentos devolve zeros e listas vazias, não erro.
- [ ] `budgetProgress` reflete o limite efetivo do mês (ajuste ou padrão) e o gasto do mês; sem meta devolve `NONE`.

### Cenários de teste

Mês vazio; mês com BRL e EUR; fronteira de mês (31/01 e 01/02); fevereiro bissexto; variações com base zero, pequena e grande; reservas em duas moedas.

### Skills de implementação

- **Skills obrigatórias:** `java-modern-engineering`, `java-spring-create-module`, `java-persistence-performance`; `openapi-rest-contract` para o contrato `overview`.
- **Caminhos:** `C:\Users\dowgl\.codex\skills\java-modern-engineering\SKILL.md`; `C:\Users\dowgl\.codex\skills\java-spring-create-module\SKILL.md`; `C:\Users\dowgl\.codex\skills\openapi-rest-contract\SKILL.md`; `.claude/skills/java-persistence-performance/SKILL.md`.
- **Responsabilidade:** contrato, composição de consultas, cálculos.
- **Momento de utilização:** depois de `transaction`, `category` e `moneybox`.
- **Dependências:** BACKEND-SPEC-05, 10, 11, 13, 14.
- **Limitações:** nenhuma específica.

## Tasks

### Contrato e dados

- [ ] Prévia aprovada e contrato `overview`.

### Back-end

- [ ] Implementar a consulta composta e os cálculos de variação.

### Validação

- [ ] Executar os cenários de teste e medir a duração com volume realista.

## Resultado esperado

- Uma chamada devolve tudo que o dashboard mostra, com cálculos iguais aos da origem.
- Preservado o isolamento por moeda e a independência das reservas em relação ao mês.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
