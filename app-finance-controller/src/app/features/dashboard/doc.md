# features/dashboard

Página `/` (FRONTEND-SPEC-09, fatia F7): resumo do mês e da moeda selecionados — KPIs, progresso da meta, barras Entradas × Saídas, pizza por categoria e últimas transações.

## Jornadas e rotas

| Jornada | Rota | Resultado esperado |
|---|---|---|
| Ver o resumo | `/` (`dashboard.routes.ts`, sob demanda, com `showMonth: true`) | Saudação por hora (`Bom dia` <12h, `Boa tarde` <18h, `Boa noite`), `Resumo de {Mês Ano}`, KPIs, meta, gráficos e as últimas 8 transações |
| Mudar mês ou moeda | seletores do shell | Uma nova chamada; os dados antigos somem enquanto carrega (esqueletos) |
| Definir meta | `Definir meta` (sem meta) | Vai para `/metas` |
| Ver lançamentos | `Ver todas →` | Vai para `/lancamentos` |

## Contrato consumido (`core/api/generated/overview.ts`)

`getMonthlyOverview(month, currency)` — **uma chamada** por mês e moeda; todas as contas (saldo, variação, faixas, percentuais, ordem) vêm prontas do servidor. O OpenAPI (`overview.yaml`) é a fonte de verdade; regenerar com `npm run generate:api`.

## Estado e responsabilidades

- `OverviewStore` (`@Injectable()` sem `providedIn`, fornecido pela página): consulta dependente de `ReferenceMonthStore.monthKey` e `CurrencyStore`; `switchMap` cancela a anterior, então a resposta de um mês antigo nunca sobrescreve a do atual. Ao recarregar, limpa o dado anterior (esqueletos). `refresh()` = "Tentar novamente".
- `DashboardPage`: compõe os blocos. `dashboard-format.ts`: `greetingFor`, `deltaFor` (texto `+25,0%`, tom e ícone; para saídas, menor é melhor) e `formatPercent`.
- Componentes: `KpiCard` (`shared/ui`, genérico), `IncomeExpenseBars` (reaproveitado pelos relatórios, com textos de acessibilidade configuráveis), `ExpensePie` e `LatestTransactions` (`ui/`). O bloco de meta reutiliza `BudgetProgressCard` de `features/budget` (mostra `Restam`/`Excedeu`).
- Gráficos em SVG puro, sem biblioteca (nenhuma dependência nova): a altura das barras e o comprimento das fatias são só geometria; os valores exibidos são o texto do servidor formatado.

## Regras e decisões

- Cores por natureza do valor: `Entradas` em azul (`text-income`), `Saídas` em vermelho (`text-expense`), `Reservas` em violeta (`text-reserve`) e `Saldo` em verde (`text-success`) se `>= 0` ou vermelho se negativo. A variação mostra verde (favorável) ou vermelho (desfavorável). Variação ausente (sem base) não é mostrada; `"0"` mostra `0%`.
- Acessibilidade: cada gráfico tem `role="img"` com `aria-label`, e uma alternativa textual (tabela para leitor de tela nas barras; legenda com ícone, nome, percentual e valor na pizza); a variação tem sinal, ícone e frase por extenso; os estados de meta têm ícone e texto.
- Reservas: o servidor soma os saldos dos cofrinhos da moeda (módulo `moneybox`, F8), independente do mês; o contrato não mudou com a F8.
- Sem meta global: convite com link para `/metas` (P-14).

## Validação

- `dashboard-format.spec.ts` e `pages/dashboard-page.spec.ts` (Vitest): saudação nas fronteiras de hora, variações (`+25,0%`, `-3,2%`, `0%`, ausente, saídas menores como boa notícia), esqueletos, Saldo `R$ 750,00` em verde e negativo em coral, uma chamada por mês/moeda, meta 1000 com gasto 850 (`Perto da meta`, `85%`, `Restam R$ 150,00`), meta estourada (`Excedeu`), convite sem meta, barras e pizza com alternativa textual, tabela com tipo/valor com sinal/`—`/link, mês vazio, troca de mês descartando o dado anterior, troca de moeda (EUR), erro com nova tentativa.
- Verificado em 2026-10-29 no navegador contra a API real com PostgreSQL: dados de teste (entradas 1250/1000, saídas 1050,50/200, meta 2000) mostraram Saldo `R$ 199,50`, `+25,0%`, `+425,3%`, meta `53%` / `Restam R$ 949,50`, barras S1…S5, pizza (Moradia 80,9%) e a tabela; setembro sem variação; EUR com zeros, convite e mensagens de vazio. Correção feita ao verificar: o nome da categoria sumia na legenda da pizza (coluna estreita); a pizza passou a ficar acima da legenda.
- Pendente: revisão no tema claro e em 320 px, teclado completo e leitor de tela.
