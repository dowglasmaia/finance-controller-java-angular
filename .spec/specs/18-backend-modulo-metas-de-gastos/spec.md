# 18-backend-modulo-metas-de-gastos

> **ID:** BACKEND-SPEC-13 · **Categoria:** Backend (módulo `budget`) · **Estado:** proposta para revisão — não aprovada, não executada.

## Objetivo

Implementar as metas de gastos por moeda — limite global recorrente, ajuste por mês e limite por categoria — com progresso, estado e avaliação de impacto que alimenta o alerta ao salvar despesas.

## Escopo

- Inclui: `getBudget`, `saveBudget`, `getBudgetProgress`; implementação de `BudgetImpactPort`.
- Não inclui: meta geral de reservas (módulo `moneybox`, BACKEND-SPEC-14), interface (FRONTEND-SPEC-13).

## Contexto técnico local

### Regras aplicadas

RN-BUD-01…09. Evidências: DESC-BUD-01…11. Em especial:

- Limite efetivo = ajuste do mês `> 0` ou recorrente (DESC-BUD-02).
- Estado exato: `EXCEEDED` se `gasto ≥ limite`; `WARNING` se `gasto × 5 ≥ limite × 4`; `NONE` se limite `≤ 0` (DESC-BUD-04).
- `fraction = gasto/limite` com 4 casas; `remaining = limite − gasto` (pode ser negativo).
- Alerta: ver RN-BUD-06/07; avalia primeiro a categoria da despesa, depois o global.

### `saveBudget`

Gravação atómica (DESC-BUD-09): substitui o conjunto de limites por categoria (itens com limite `≤ 0` ou ausentes são removidos), atualiza o limite global e aplica o ajuste do mês informado, que é flexível (P-06): `limit > 0` define o valor do mês; `disabled = true` significa "sem meta neste mês" (mesmo com meta padrão); ausente remove o ajuste e volta ao padrão. Categorias inexistentes ou que não aceitam despesa são rejeitadas (novo; a origem listava só categorias de despesa).

### `getBudgetProgress`

Inclui o global e todas as categorias que aceitam despesa, com os gastos do mês, para a tela mostrar "gasto de limite" ou apenas "Gasto: X" (DESC-BUD-09).

### Pendências e riscos

- P-06 (decidida): "sem meta neste mês" explícito, diferente de ausência de ajuste. Estado do mês desativado: `NONE`.
- P-20 (decidida): o alerta avalia o mês de referência enviado pelo cliente; despesa fora dele não alerta.
- Dependência de `transaction` para somas de despesa; `transaction` conhece `budget` só pela porta (ADR-001).

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [BACKEND-SPEC-01](../07-backend-dominio-e-regras-de-negocio/spec.md)
- [BACKEND-SPEC-11](../16-backend-modulo-lancamentos/spec.md)

## Regras e critérios de aceitação

- [ ] Limite 1000: gasto 700 → `OK`; 800 → `WARNING`; 950 → `WARNING`; 1000 e 1200 → `EXCEEDED`; limite 0 → `NONE`.
- [ ] Limite 200, gasto 50 → `fraction` 0,25 e `remaining` 150; gasto 250 → `remaining` −50.
- [ ] Ajuste de julho de 1500 com recorrente 1000: `effectiveLimit` de julho = 1500, de agosto = 1000; ajuste removido volta a 1000.
- [ ] Ajuste de julho com `disabled=true`: estado de julho `NONE` e agosto continua com 1000.
- [ ] Metas BRL e EUR são independentes.
- [ ] `saveBudget` falha por completo se qualquer categoria for inválida (nada é gravado).
- [ ] O alerta nunca repete em estado já estourado e nunca dispara em receita.

### Cenários de teste

Tabela de estados; transições (700→850, 850→1100, 1100→1300, 100→300); prioridade categoria × global; edição de despesa; ajuste mensal; atomicidade de `saveBudget`; moeda.

### Skills de implementação

- **Skills obrigatórias:** `java-modern-engineering`, `java-spring-create-module`; `openapi-rest-contract` para o contrato `budgets`.
- **Caminhos:** `C:\Users\dowgl\.codex\skills\java-modern-engineering\SKILL.md`; `C:\Users\dowgl\.codex\skills\java-spring-create-module\SKILL.md`; `C:\Users\dowgl\.codex\skills\openapi-rest-contract\SKILL.md`.
- **Responsabilidade:** contrato, domínio de metas (estados, transições) e adaptador.
- **Momento de utilização:** depois de `transaction`.
- **Dependências:** BACKEND-SPEC-05, 10, 11.
- **Limitações:** nenhuma específica.

## Tasks

### Contrato e dados

- [ ] Prévia aprovada e contrato `budgets`; migração das tabelas de metas.

### Back-end

- [ ] Implementar domínio, casos de uso, adaptador, controller e `BudgetImpactPort`.

### Validação

- [ ] Executar os cenários e comparar com os valores de `budget_service_test`.

## Resultado esperado

- Metas e alertas idênticos aos da origem, calculados no servidor com comparação exata.
- Preservada a regra: um alerta, só na piora do estado, categoria primeiro.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
