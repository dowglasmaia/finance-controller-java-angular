# 37-frontend-pagina-relatorios

> **ID:** FRONTEND-SPEC-14 · **Categoria:** Frontend (`features/reports`) · **Estado:** proposta para revisão — não aprovada, não executada.

## Objetivo

Construir a página de relatórios por semana, mês ou ano: KPIs, evolução de entradas e saídas, despesas por categoria, ranking e resumo mensal anual.

## Escopo

- Inclui: página `/relatorios`, seletor de período, KPIs, gráficos, ranking, tabela anual.
- Não inclui: cálculos e âncora de período (backend).

## Contexto técnico local

### Página (CONFIRMADO — DESC-REP-04…06)

- Título `Relatórios` / `Análise financeira do período`; `MonthSwitcher` e seletor `Semana | Mês | Ano` (padrão Mês; não persistido).
- KPIs: Entradas, Saídas, Saldo (cor pelo sinal).
- `Evolução de Entradas e Saídas`: barras duplas — semana Dom…Sáb, mês S1…S5, ano Jan…Dez.
- `Despesas por Categoria` (pizza com legenda abaixo) e `Ranking de Categorias` (ícone, nome, %, valor e barra; máximo 7; vazio `Sem despesas no período.`).
- Só no período Ano: tabela `Resumo Mensal` com Mês, Entradas, Saídas, Saldo (cor) e `% vs anterior` (`—` na primeira linha ou com base zero).

### Operação consumida

`getReport(period, anchorDate, currency, rankingSize?)`. O cliente envia `anchorDate` = dia 15 do mês de referência, mantendo a regra atual (P-05, decidida).

### Correção de comportamento (DESC-REP-03; P-09, aprovada)

Na origem web o gráfico de barras era calculado com a data atual e não com o mês de referência, ficando incoerente com os KPIs quando o mês selecionado não é o atual. No destino as barras vêm do mesmo intervalo dos totais.

### Ranking (P-10, decidida)

Mantém-se o comportamento atual: as **7** maiores categorias de despesa (a documentação da origem fala em 5, mas não prevalece).

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [BACKEND-SPEC-15](../20-backend-modulo-relatorios/spec.md)
- [FRONTEND-SPEC-02](../25-frontend-design-system-e-componentes/spec.md)

## Regras e critérios de aceitação

- [ ] Em Mês, a soma das barras de entradas é igual ao KPI Entradas, mesmo com o mês de referência diferente do atual (P-09 aprovada).
- [ ] Em Semana há 7 barras Dom…Sáb; em Ano há 12 barras e a tabela `Resumo Mensal`.
- [ ] Pizza e ranking só incluem despesas da moeda selecionada.
- [ ] `% vs anterior` aparece como `—` na primeira linha e quando o saldo anterior é 0.
- [ ] Gráficos têm alternativa textual; vazio, carregando e erro tratados.

### Skills de implementação

- **Skills obrigatórias:** `angular-module-scaffold`, `angular-tailwind-ui-specialist`, `angular-architect`.
- **Caminhos:** `.claude/skills/angular-module-scaffold/SKILL.md`; `.claude/skills/angular-tailwind-ui-specialist/SKILL.md`; `.claude/skills/angular-architect/SKILL.md`.
- **Responsabilidade:** módulo e `doc.md`; apresentação dos gráficos e tabelas; estado por período.
- **Momento de utilização:** depois do contrato `reports`.
- **Dependências:** FRONTEND-SPEC-00…04, 25; BACKEND-SPEC-15; ADR-013 (ApexCharts).
- **Limitações:** nenhuma específica.

## Tasks

### Front-end

- [ ] Criar `features/reports` com página, gráficos, ranking, tabela anual e serviço de dados.

### Validação

- [ ] Testar coerência totais × barras, os três períodos e os estados vazios.

## Resultado esperado

- Relatórios equivalentes à origem, com totais e gráfico consistentes.
- Preservado o isolamento por moeda.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
