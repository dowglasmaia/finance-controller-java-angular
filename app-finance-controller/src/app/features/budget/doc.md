# features/budget

Página `/metas` (FRONTEND-SPEC-13, fatia F6): progresso do mês, meta padrão recorrente, ajuste do mês, metas por categoria e gravação conjunta.

## Jornadas e rotas

| Jornada | Rota | Resultado esperado |
|---|---|---|
| Ver progresso | `/metas` (`budget.routes.ts`, sob demanda, com `showMonth: true`) | Cartão `Meta de {Mês Ano}` com `N%`, barra (até 100%), `{gasto} de {limite}` e o estado com texto e ícone. Só aparece se há limite global |
| Meta padrão | campo `Meta padrão` | Teto recorrente; vazio = sem meta global |
| Ajuste do mês | `Meta deste mês` + caixa `Sem meta neste mês` | Valor só para o mês exibido, ou meta desativada só nele; vazio e sem a caixa remove o ajuste |
| Por categoria | uma linha por categoria que aceita despesa | `{gasto} de {limite}` colorido pelo estado, ou `Gasto: {gasto}`; campo vazio remove |
| Salvar | `Salvar meta` | Uma única gravação; `Meta salva.`; tudo recarrega |
| Mudar mês ou moeda | seletores do shell | Recarrega o ajuste daquele mês ou a configuração da outra moeda |

## Contrato consumido (`core/api/generated/budgets.ts`)

`getBudget`, `saveBudget`, `getBudgetProgress`, mais `listCategories` (via `CategoriesStore`, para nome, ícone e cor). O OpenAPI (`budgets.yaml`) é a fonte de verdade; regenerar com `npm run generate:api`.

## Estado e responsabilidades

- `BudgetStore` (`@Injectable()` sem `providedIn`, fornecido pela página): configuração e progresso do mês e da moeda selecionados (`ReferenceMonthStore.monthKey`, `CurrencyStore`), carregados juntos com `forkJoin`; mudar mês ou moeda cancela a consulta anterior. `save()` grava e, em sucesso, recarrega; em falha não altera nada.
- `BudgetPage`: formulário tipado (meta padrão, ajuste, "sem meta" e um `FormRecord` com um controle por categoria). Cada carga preenche todos os campos com o que o servidor tem; em falha de gravação os valores digitados permanecem.
- `BudgetProgressCard` e `CategoryLimitRow` (`ui/`): apresentação. `budget-status.ts` define texto, ícone e cor de cada estado (`Dentro da meta`, `Perto da meta`, `Meta estourada`, `Sem meta definida`).
- Valores monetários: `MoneyInputDirective` (decimal em texto, sem `number`, sem negativos). O estado (`OK`/`WARNING`/`EXCEEDED`), a fração e o restante vêm prontos do servidor; o cliente só mostra.

## Regras e decisões

- Pedido de gravação: `month` (mês exibido), `monthlyLimit?`, `monthOverride?` (`{limit}` ou `{disabled: true}`; ausente remove o ajuste daquele mês, P-06) e o conjunto completo de `categoryLimits`. Campos vazios não vão no pedido.
- "Sem meta neste mês" desativa o campo do ajuste; o servidor recusa `limit` e `disabled` juntos.
- Categoria removida em outra aba (404): recarrega os seletores e avisa; erro de categoria incompatível (409) e de validação aparecem em toast com a mensagem do servidor.
- O percentual é `round(fração × 100)` só para exibição; a barra para em 100%.
- O progresso também aparece no dashboard (P-14, F7), reutilizando `BudgetProgressCard`, que mostra `Restam R$ …` ou `Excedeu R$ …`.

## Validação

- `pages/budget-page.spec.ts` (Vitest): campos preenchidos pelo servidor, os 3 estados com texto, ícone e percentual (70, 80, 95, 100, 120%), sem meta global sem cartão, linha da categoria colorida, gravação com valores exatos e `month`, vazio remove, "sem meta" envia `disabled` e desativa o campo, falha mantém o digitado e não recarrega, troca de mês e de moeda, erro de carregamento com nova tentativa.
- Verificado em 2026-10-29 no navegador contra a API real com PostgreSQL: página com as 7 categorias de despesa, gravação de meta padrão 1000 e meta de Lazer 200 (`Meta salva.`, cartão `0%` / `Dentro da meta`), despesa de 850 em Alimentação com o toast `Você já usou 80% da sua meta mensal.` e o progresso em `85%` / `Perto da meta`.
- Pendente: teclado completo, 320/768 px e contraste nos dois temas.
