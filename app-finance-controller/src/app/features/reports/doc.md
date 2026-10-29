# features/reports

Página `/relatorios` (FRONTEND-SPEC-14, fatia F9): análise financeira por semana, mês ou ano — KPIs, evolução de entradas e saídas, despesas por categoria, ranking e, no ano, o resumo mensal.

## Jornadas e rotas

| Jornada | Rota | Resultado esperado |
|---|---|---|
| Ver o relatório | `/relatorios` (`reports.routes.ts`, sob demanda, `showMonth: true`) | `Relatórios` / `Análise financeira do período`; seletor `Semana \| Mês \| Ano` (padrão Mês, não persistido); `Período: dd/MM/yyyy a dd/MM/yyyy`; KPIs `Entradas`, `Saídas`, `Saldo` (cor pelo sinal); `Evolução de Entradas e Saídas`; `Despesas por Categoria`; `Ranking de Categorias` |
| Mudar período, mês ou moeda | seletor da página e do shell | Uma nova chamada; os dados antigos somem enquanto carrega |
| Resumo anual | período Ano | Tabela `Resumo Mensal`: Mês, Entradas, Saídas, Saldo e `% vs anterior` |

## Contrato consumido (`core/api/generated/reports.ts`)

`getReport(period, anchorDate, currency)` — **uma chamada** por período, mês de referência e moeda; `rankingSize` não é enviado (o servidor usa 7). Todas as contas (intervalo, barras, percentuais, ordem, variação do saldo) vêm prontas do servidor. O OpenAPI (`reports.yaml`) é a fonte de verdade; regenerar com `npm run generate:api`.

## Estado e responsabilidades

- `ReportsStore` (`@Injectable()` sem `providedIn`, fornecido pela página): período (local, começa em `MONTH`), mês (`ReferenceMonthStore`) e moeda (`CurrencyStore`); a âncora enviada é o **dia 15** do mês de referência (P-05, regra mantida). `switchMap` cancela a consulta anterior e a resposta de um período antigo nunca sobrescreve a atual; ao recarregar o dado anterior é limpo.
- `ReportsPage`: compõe os blocos. `reports-format.ts`: `monthName` (`Jun/2026`), `rangeText`, `changeText` (`+50,0%`, `-100,0%`, `—`) e `signClass`.
- Componentes: `RankingList` e `MonthlySummaryTable` (`ui/`); `KpiCard`, `IncomeExpenseBars` e `ExpensePie` são do dashboard/`shared` e foram reaproveitados. As barras ganharam três `input` de acessibilidade (`ariaLabel`, `caption`, `columnHeader`) com o texto do dashboard como padrão.
- Gráficos em SVG puro, sem biblioteca (a spec cita ApexCharts, ADR-013; o dashboard da F7 já nasceu sem dependência e o relatório segue o mesmo caminho).

## Regras e decisões

- As barras e os KPIs vêm do mesmo intervalo no servidor (corrige DESC-REP-03, P-09): em Mês a soma das barras de entradas é o KPI Entradas, mesmo com o mês de referência diferente do atual.
- Semana é domingo–sábado e pode cruzar mês e ano; Mês usa faixas de 7 dias (`S1…S5`, o resto na última); Ano tem 12 meses.
- Ranking: as 7 maiores categorias de despesa (P-10); a pizza traz todas. Só despesas e só a moeda selecionada; movimentações de cofrinhos não entram.
- `% vs anterior`: `—` em janeiro e quando o saldo anterior é zero; senão `+`/`-` com 1 casa, em verde ou vermelho (sinal e cor repetem a informação). Mesma paleta do dashboard: Entradas azul, Saídas vermelho, Saldo verde (vermelho se negativo); na tabela anual as colunas Entradas e Saídas também são azul e vermelha.
- Acessibilidade: cada gráfico tem `role="img"` com `aria-label` e alternativa textual (tabela para leitor de tela nas barras; legenda com ícone, nome, percentual e valor na pizza; lista numerada no ranking); o seletor de período é um `radiogroup`.

## Validação

- `pages/reports-page.spec.ts` (Vitest, 10 testes): Mês por padrão com âncora no dia 15 e KPIs, saldo negativo em vermelho, soma das barras = Entradas, pizza e ranking, vazio, Semana (7 barras, sem resumo), Ano (12 barras, `Resumo Mensal` com `—`, `+50,0%` e `-100,0%` com cor), troca de mês/moeda cancelando a consulta antiga, erro com nova tentativa; e os formatadores.
- Verificado em 2026-10-29 no navegador contra a API real com PostgreSQL (tema escuro, desktop; app em outubro, relatório de junho/2026): Mês com Entradas R$ 3.250,00, Saídas R$ 2.339,50 e Saldo R$ 910,50, faixas S1…S5 coerentes, pizza e ranking (Saúde 42,7%, Moradia 34,2%, …); Semana 14/06–20/06 com 7 barras; Ano com 12 barras e `Resumo Mensal` (Mai `—`, Jun `-39,3%`, Jul `-100,0%`); EUR com valores só em euro.
- Pendente: teclado completo, leitor de tela, tema claro, 320/768 px (as 12 barras do ano ficam pequenas em telas estreitas) e contraste.
