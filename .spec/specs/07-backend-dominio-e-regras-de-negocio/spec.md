# 07-backend-dominio-e-regras-de-negocio

> **ID:** BACKEND-SPEC-01 · **Categoria:** Backend · **Estado:** proposta para revisão — não aprovada, não executada.

## Objetivo

Registar as regras de negócio e os cálculos do Finance Controller a preservar no domínio Java, com a evidência de origem e as correções propostas, como fonte única para as specs de módulo e para os testes de domínio.

## Escopo

- Inclui: regras `RN-*`, algoritmos, exemplos numéricos e a classificação de cada regra (preservar, endurecer, novo), já com as decisões do produto aplicadas.
- Não inclui: persistência, endpoints e telas (outras specs). Regras específicas de módulo são detalhadas nas specs 15–22 sem contradizer esta.

## Contexto técnico local

Legenda: **P** = preservar comportamento confirmado; **E** = endurecer (a origem só validava na interface); **N** = novo/proposto; **?** = depende de decisão (pendência).

### Moedas e valores

| ID | Regra | Tipo | Evidência |
|---|---|---|---|
| RN-CUR-01 | Só `BRL` e `EUR`; valor desconhecido é rejeitado. | P | DESC-CUR-01/06 |
| RN-CUR-02 | Nunca somar nem converter entre moedas; toda agregação filtra por moeda. | P | DESC-CUR-02 |
| RN-VAL-01 | Valores monetários: `BigDecimal` com escala 2, `amount > 0`, máximo `9 999 999 999,99` (hipótese). | E/N | DESC-TXN-02, DESC-DAT-03; P-07, P-21 |
| RN-VAL-02 | Arredondamento só na exibição; cálculos exatos; percentuais com 4 casas e `HALF_UP`. | N | DESC-DAT-03 |

### Lançamentos

| ID | Regra | Tipo | Evidência |
|---|---|---|---|
| RN-TXN-01 | Um lançamento é entrada ou saída; `amount > 0`; a direção está em `kind`. | P/E | DESC-TXN-01/02 |
| RN-TXN-02 | Descrição obrigatória (após `trim`, não vazia); tamanho máximo 120 (hipótese). | P/N | DESC-TXN-03; P-21 |
| RN-TXN-03 | Categoria obrigatória e deve aceitar o tipo (`BOTH` aceita ambos). Validado no servidor. | P/E | DESC-TXN-03/04 |
| RN-TXN-04 | Subcategoria opcional; se informada, pertence à categoria do lançamento. | P/E | DESC-TXN-05 |
| RN-TXN-05 | Data obrigatória; futuras permitidas; intervalo aceito 2000-01-01…2100-12-31 (limites do seletor de origem). | P/E | DESC-TXN-06 |
| RN-TXN-06 | Moeda obrigatória no lançamento; observações opcionais (`trim`, vazio → ausente); edição preserva `id` e `createdAt`. | P | DESC-TXN-07 |
| RN-TXN-07 | O saldo nunca é persistido (ver RN-SAL-01). | P | DESC-MES-03 |
| RN-TXN-08 | Ordenação padrão: data decrescente; desempate por `createdAt` decrescente e `id`. | N | DESC-TXN-11 |
| RN-TXN-09 | Cada moeda tem os seus lançamentos, despesas e dashboard. Ao salvar um lançamento noutra moeda, a interface passa a selecionar essa moeda (comportamento da origem), e a preferência é gravada. Regra de cliente; o servidor não altera nada por isso. | P | DESC-CUR-04; P-15 (decidida) |

### Categorias e subcategorias

| ID | Regra | Tipo | Evidência |
|---|---|---|---|
| RN-CAT-01 | Nome obrigatório e único ignorando caixa e espaços nas pontas, garantido no banco. | P/E | DESC-CAT-02 |
| RN-CAT-02 | Tipo ∈ {INCOME, EXPENSE, BOTH}; ícone e cor obrigatórios. | P | DESC-CAT-01 |
| RN-CAT-03 | Categoria com lançamentos não pode ser excluída (`CATEGORY_IN_USE`). | P | DESC-CAT-03 |
| RN-CAT-04 | Excluir categoria sem lançamentos remove as suas subcategorias. | P | DESC-CAT-03 |
| RN-CAT-05 | Toda categoria nova nasce com a subcategoria "Outros". | P | DESC-SUB-01 |
| RN-CAT-06 | "Outros" (ignorando caixa) não pode ser excluída nem renomeada, e não pode haver duas na mesma categoria. | P/E | DESC-SUB-01/02 |
| RN-CAT-07 | Excluir subcategoria com lançamentos vinculados é permitido e **desassocia** os lançamentos (ficam sem subcategoria), como a origem mostrava `—`, sem referência órfã. | P/E | DESC-SUB-03; P-11 (delegada) |
| RN-CAT-08 | Não há limite de quantidade de categorias (o de 50 da origem nunca foi aplicado e não será). | P | DESC-CAT-05; P-12 (decidida) |
| RN-CAT-09 | Mudar o tipo de uma categoria é recusado quando há lançamentos que ficariam incompatíveis (`CATEGORY_TYPE_CONFLICT`). Alterar a categoria de um lançamento é livre e todos os totais, por serem derivados, refletem a mudança. | E | DESC-CAT-06; P-13 (delegada) |
| RN-CAT-11 | Nome de categoria único por utilizador; nome de subcategoria único dentro da categoria. | E | P-01, P-11 |
| RN-CAT-10 | Semente inicial: 11 categorias (7 despesa, 4 receita) com subcategorias e "Outros". | P | DESC-SEE-01 |

### Mês de referência e saldo

| ID | Regra | Tipo | Evidência |
|---|---|---|---|
| RN-MES-01 | O mês de um lançamento é o ano-mês da sua data financeira (`LocalDate`), sem fuso. | P/E | DESC-MES-02, DESC-DAT-04 |
| RN-SAL-01 | Saldo do mês e da moeda = Σ entradas − Σ saídas; calculado a partir dos lançamentos. | P | DESC-MES-03 |
| RN-SAL-02 | Variação vs mês anterior = `(atual − anterior) / anterior × 100`, só se `anterior > 0`; `|v| < 0,5` → `0%`; sem base → ausente. Para saídas, menor é melhor. | P | DESC-MES-03, DESC-DSH-02 |

### Metas de gastos

| ID | Regra | Tipo | Evidência |
|---|---|---|---|
| RN-BUD-01 | Existem, por moeda: limite global recorrente, ajustes por mês e limites por categoria. | P | DESC-BUD-01 |
| RN-BUD-02 | Limite efetivo do mês = ajuste do mês se `> 0`, senão o recorrente. Ajuste `≤ 0` é removido. | P | DESC-BUD-02 |
| RN-BUD-03 | Limite de categoria `> 0` é gravado; `≤ 0` remove. | P | DESC-BUD-03 |
| RN-BUD-04 | Estado: limite `≤ 0` → `NONE`; `gasto ≥ limite` → `EXCEEDED`; `gasto × 5 ≥ limite × 4` → `WARNING`; senão `OK`. Comparação exata. | P (exata: N) | DESC-BUD-04 |
| RN-BUD-05 | Gasto = soma das **despesas** do mês e da moeda; por categoria, só as da categoria. | P | DESC-BUD-05 |
| RN-BUD-06 | Alerta só em despesa e só se o estado **piorar** (`OK→WARNING`, `OK/WARNING→EXCEEDED`); nunca repete nem alerta em melhora. | P | DESC-BUD-06 |
| RN-BUD-07 | Um só alerta por salvamento; a categoria tem prioridade sobre o global. | P | DESC-BUD-07 |
| RN-BUD-08 | Mês avaliado pelo alerta: o **mês de referência enviado pelo cliente**. Despesa datada fora desse mês não altera o progresso e não alerta (comportamento da origem). | P | DESC-BUD-08; P-20 (decidida) |
| RN-BUD-09 | O ajuste de um mês pode ser um valor, "sem meta neste mês" (explícito) ou ausente (volta ao padrão). | N | produto.md; P-06 (decidida) |

### Cofrinhos

| ID | Regra | Tipo | Evidência |
|---|---|---|---|
| RN-BOX-01 | Nome obrigatório; moeda obrigatória; ícone e cor; descrição e meta opcionais (`goal ≥ 0`, 0 = sem meta). | P/E | DESC-BOX-07, DESC-BOX-12 |
| RN-BOX-02 | Saldo do cofrinho = Σ depósitos − Σ saques, derivado. | P | DESC-BOX-03 |
| RN-BOX-03 | Movimentação: `amount > 0`, data obrigatória, nota opcional, moeda = a do cofrinho. | P/E | DESC-BOX-02/04 |
| RN-BOX-04 | Moeda do cofrinho imutável se houver movimentações (`CURRENCY_LOCKED`). | P | DESC-BOX-05 |
| RN-BOX-05 | O saldo do cofrinho **nunca fica negativo**: saque recusado se o saldo for `≤ 0` ou se o valor exceder o saldo (`INSUFFICIENT_BALANCE`); editar ou excluir uma movimentação que deixaria o saldo negativo em qualquer ponto do histórico também é recusado. O histórico é mantido. A origem permitia saldo negativo. | E | DESC-BOX-06; P-04 (decidida) |
| RN-BOX-06 | Excluir cofrinho remove as suas movimentações na mesma transação. | P/E | DESC-BOX-08 |
| RN-BOX-07 | Total de reservas = Σ saldos dos cofrinhos da moeda; independe do mês. | P | DESC-BOX-03 |
| RN-BOX-08 | Meta geral por moeda: `≥ 0`, 0 = sem meta; independente da soma das metas individuais. Progresso = `min(total/meta, 1)`. | P | DESC-BOX-09 |
| RN-BOX-09 | Progresso individual = `clamp(saldo/meta, 0, 1)`; 0 se não há meta. | P | DESC-BOX-06, `money_box.dart::progress` |
| RN-BOX-10 | Movimentações de reserva não criam lançamentos nem alteram o saldo mensal. | P | DESC-BOX-12 |

### Relatórios

| ID | Regra | Tipo | Evidência |
|---|---|---|---|
| RN-REP-01 | Períodos: semana domingo–sábado; mês 1…último dia; ano 1/jan…31/dez; inclusivos por dia. | P | DESC-REP-01 |
| RN-REP-02 | Âncora do período: dia 15 do mês de referência (regra atual mantida), enviada como parâmetro explícito `anchorDate`. | P | DESC-REP-02; P-05 (decidida) |
| RN-REP-03 | Barras: semana 7 (Dom…Sáb), mês `ceil(últimoDia/7)` faixas S1…S5 de 7 dias, ano 12 meses. **As barras usam a mesma âncora dos totais** (corrige a origem). | P + correção | DESC-REP-03/04; P-09 (decidida) |
| RN-REP-04 | Distribuição e ranking consideram só despesas; ranking limitado a **7** categorias (comportamento atual mantido; 5 na documentação não prevalece). | P | DESC-REP-05/07; P-10 (decidida) |
| RN-REP-05 | Resumo mensal (período ano): saldo por mês e `% vs anterior` = `(saldo−anterior)/|anterior|×100`, ausente se anterior = 0. | P | DESC-REP-06 |

### Backup — cancelado

`RN-BKP-01…05` foram **canceladas** (P-03, P-16): não há backup, importação nem migração. Ver [spec 21](../21-backend-modulo-backup-e-importacao/spec.md).

### Identidade e propriedade (novo — P-01)

| ID | Regra | Tipo | Evidência |
|---|---|---|---|
| RN-OWN-01 | Todo registo de dados pertence a um utilizador (`owner_id`); o proprietário vem do token autenticado, nunca do cliente. | N | P-01 |
| RN-OWN-02 | Um utilizador só lê, cria, altera e apaga os seus próprios registos; o de outro responde 404. Totais, relatórios e dashboards só somam os seus dados. | N | P-01 |
| RN-OWN-03 | Referências cruzadas (lançamento→categoria, movimentação→cofrinho, limite→categoria) só entre registos do mesmo proprietário. | N | P-01 |
| RN-OWN-04 | O cadastro de um utilizador cria as suas 11 categorias padrão. | N | DESC-SEE-01 |

### Exemplos numéricos de referência (origem)

- BRL: +1000 −250 = **750**; EUR: +500 −100 = **400** (DESC-CUR-02).
- Meta 1000: gasto 700 → `OK`; 800 → `WARNING`; 950 → `WARNING`; 1000 e 1200 → `EXCEEDED`; sem meta → `NONE` (DESC-BUD-04).
- Alerta: 700→850 = `WARNING`; 850→1100 = `EXCEEDED`; 1100→1300 = nenhum; 100→300 = nenhum (DESC-BUD-06).
- Cofrinho: +1000 +500 −300 = **1200**; +100 e saque de 250 é **recusado** (a origem aceitava e dava −150 — DESC-BOX-06; regra mudada por P-04).
- Semana de 2026-06-24 (quarta): domingo 2026-06-21 a sábado 2026-06-27 (DESC-REP-01).

### Riscos

Todas as regras antes marcadas "?" foram decididas em [pendencias.md](../00-engenharia-reversa/pendencias.md) e estão refletidas acima. Onde a origem era permissiva e o destino endurece (E), mensagens e códigos devem ser revistos com o produto.

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [Catálogo de descobertas](../00-engenharia-reversa/descobertas.md)
- [GLOBAL-SPEC-02](../04-global-dominio-e-glossario/spec.md)

## Regras e critérios de aceitação

- [ ] Cada `RN-*` com tipo P, E ou N tem pelo menos um teste de domínio ou de integração rastreável.
- [ ] Os exemplos numéricos de referência passam como testes parametrizados.
- [ ] As regras RN-OWN têm teste de isolamento por recurso.
- [ ] O domínio não conhece Spring, HTTP, JPA nem DTOs gerados.

### Skills de implementação

- **Skills obrigatórias:** `java-modern-engineering`, `java-spring-create-module`.
- **Caminhos:** `C:\Users\dowgl\.codex\skills\java-modern-engineering\SKILL.md`; `C:\Users\dowgl\.codex\skills\java-spring-create-module\SKILL.md`.
- **Responsabilidade:** modelar invariantes e cálculos (`java-modern-engineering`); posicionar o domínio no módulo conforme o scaffold (`java-spring-create-module`).
- **Momento de utilização:** na implementação de cada módulo (specs 15–21).
- **Dependências:** GLOBAL-SPEC-02, BACKEND-SPEC-00; decisões P-01…P-21 já registadas.
- **Limitações:** nenhuma.

## Tasks

### Descoberta e decisão

- [ ] Rever as regras marcadas **E** e **N** com o produto antes de codificar.

### Back-end

- [ ] Implementar os tipos de valor (`Money`, `YearMonth` de domínio) e as regras RN-CUR/VAL.
- [ ] Implementar as regras por módulo (specs 15–21) mantendo o domínio puro.

### Validação

- [ ] Executar os testes de domínio e comparar com os exemplos numéricos desta spec.

## Resultado esperado

- Comportamento financeiro idêntico ao da origem, exceto nos pontos de endurecimento aprovados.
- Preservada a regra de que saldos e totais são sempre derivados.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
