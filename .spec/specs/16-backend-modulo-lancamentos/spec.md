# 16-backend-modulo-lancamentos

> **ID:** BACKEND-SPEC-11 · **Categoria:** Backend (módulo `transaction`) · **Estado:** proposta para revisão — não aprovada, não executada.

## Objetivo

Implementar o módulo de lançamentos (receitas e despesas): criação, listagem mensal por moeda, edição e exclusão, com o alerta de meta devolvido ao salvar despesas.

## Escopo

- Inclui: `listTransactions`, `createTransaction`, `getTransaction`, `updateTransaction`, `deleteTransaction`; API pública de consulta de despesas; `CategoryUsageGuard` e `BudgetImpactPort`.
- Não inclui: cálculo de metas (BACKEND-SPEC-13), agregados do dashboard (BACKEND-SPEC-12), interface (FRONTEND-SPEC-10).

## Contexto técnico local

### Regras aplicadas

RN-TXN-01…09, RN-MES-01, RN-SAL-01, RN-CUR-01/02, RN-VAL-01/02. Evidências: DESC-TXN-01…11, DESC-MES-02, DESC-CUR-04.

### Fluxos

- **Criar/atualizar:** valida formato → carrega categoria e subcategoria → confere tipo (`CATEGORY_TYPE_MISMATCH`) e pertinência (`SUBCATEGORY_NOT_IN_CATEGORY`) → grava → se `kind=EXPENSE` e a data pertence ao `referenceMonth` recebido, chama `BudgetImpactPort` com o gasto antes/depois desse mês → devolve `{transaction, budgetAlert?}`; fora desse mês, sem alerta (P-20).
- **Listar:** `month` + `currency` obrigatórios; filtro `type`; ordenação RN-TXN-08; paginação (15 por padrão, DESC-TXN-09); `totals` do conjunto filtrado completo.
- **Excluir:** remove sem verificar dependências; o gasto da meta recalcula-se por consulta.
- **Moeda:** cada moeda tem os seus lançamentos, despesas e dashboard. A troca da moeda selecionada ao salvar um lançamento noutra moeda é comportamento do cliente (DESC-CUR-04; P-15); o servidor apenas guarda cada lançamento na moeda informada.

### Casos limite

- Data fora do `referenceMonth` do cliente: nenhum alerta e o progresso do mês de referência não muda (P-20, comportamento da origem).
- Lançamento sem subcategoria, com `notes` de espaços: `notes` ausente.
- Valores: `0`, `-1`, `1.234,56` com formato errado na API → `VALIDATION_FAILED` (a API só aceita `"1234.56"`).
- Edição que muda de despesa para receita: alerta calculado só se o resultado é despesa.

### Riscos

O cálculo de "antes" na edição precisa excluir o valor antigo do próprio lançamento; testar explicitamente (DESC-BUD-07 só descreve o caminho de criação com clareza).

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [BACKEND-SPEC-01](../07-backend-dominio-e-regras-de-negocio/spec.md)
- [BACKEND-SPEC-03](../09-backend-api-rest-e-openapi/spec.md)
- [BACKEND-SPEC-10](../15-backend-modulo-categorias/spec.md)

## Regras e critérios de aceitação

- [ ] Criar lançamento com `amount` `0` ou negativo → 400 `VALIDATION_FAILED` com mensagem `O valor deve ser maior que zero`.
- [ ] Categoria `EXPENSE` num lançamento `INCOME` → 409 `CATEGORY_TYPE_MISMATCH`; categoria `BOTH` aceita ambos.
- [ ] `listTransactions` de um mês devolve só lançamentos desse mês e moeda; `totals.balance` = entradas − saídas do conjunto filtrado completo, não da página.
- [ ] Data futura é aceita; datas fora de 2000-01-01…2100-12-31 são rejeitadas.
- [ ] Editar preserva `id` e `createdAt` e atualiza `updatedAt`.
- [ ] Despesa que leva o gasto de 700 para 850 com meta 1000 devolve `budgetAlert` `WARNING`; de 850 para 1100, `EXCEEDED`; nova despesa já estourada não devolve alerta.
- [ ] Receita nunca devolve `budgetAlert`; despesa datada fora do `referenceMonth` também não.
- [ ] Um utilizador não lê nem altera lançamentos de outro, nem usa categoria de outro (404).

### Cenários de teste

Validação de entrada; compatibilidade de categoria/subcategoria; paginação e totais; ordenação e desempate; edição e alerta; exclusão; concorrência de duas despesas simultâneas no limiar; isolamento de moeda (EUR não entra em BRL).

### Skills de implementação

- **Skills obrigatórias:** `java-modern-engineering`, `java-spring-create-module`; `openapi-rest-contract` para o contrato `transactions`.
- **Caminhos:** `C:\Users\dowgl\.codex\skills\java-modern-engineering\SKILL.md`; `C:\Users\dowgl\.codex\skills\java-spring-create-module\SKILL.md`; `C:\Users\dowgl\.codex\skills\openapi-rest-contract\SKILL.md`.
- **Responsabilidade:** contrato, implementação das 5 operações, domínio do lançamento.
- **Momento de utilização:** depois do módulo `category`.
- **Dependências:** BACKEND-SPEC-05, 10; BACKEND-SPEC-13 para o alerta (pode usar porta falsa até existir).
- **Limitações:** nenhuma específica.

## Tasks

### Contrato e dados

- [ ] Prévia aprovada e contrato `transactions`; migração `transaction_entry`.

### Back-end

- [ ] Implementar domínio, casos de uso, adaptadores, controller e a API de consulta de despesas.

### Validação

- [ ] Testes de validação, listagem, totais e alertas com PostgreSQL real.

## Resultado esperado

- Lançamentos equivalentes à origem, com validação completa no servidor e alerta de meta na resposta.
- Preservado o isolamento por moeda e o saldo derivado.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
