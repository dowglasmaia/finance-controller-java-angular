# features/transactions

Página `/lancamentos` (FRONTEND-SPEC-10, fatia F5): lista mensal por moeda, filtro por direção, subtotais, paginação e criação, edição e exclusão de lançamentos.

## Jornadas e rotas

| Jornada | Rota | Resultado esperado |
|---|---|---|
| Listar | `/lancamentos` (`transactions.routes.ts`, sob demanda, com `showMonth: true`) | Lançamentos do mês e da moeda selecionados no shell, mais recentes primeiro, 15 por página. Vazio: `Nenhum lançamento no período.` |
| Filtrar | `Todos \| Entradas \| Saídas` | Nova consulta, volta à página 1; subtotais refletem o filtro |
| Paginar | anterior/próxima | Rodapé `1–15 de N` e `página/total` |
| Criar | `Novo Lançamento` | Diálogo; `Lançamento salvo.`; lista recarrega; alerta de meta, se houver |
| Editar | `Editar` na linha | Mesmo diálogo preenchido (data e moeda preservadas) |
| Excluir | `Excluir` na linha | Confirmação; `Lançamento excluído.` |

## Contrato consumido (`core/api/generated/transactions.ts`)

`listTransactions`, `createTransaction`, `updateTransaction`, `deleteTransaction`, mais `listCategories` (via `CategoriesStore`). `getTransaction` existe no contrato e não é usada: a lista já traz todos os campos necessários à edição. O OpenAPI (`transactions.yaml`) é a fonte de verdade; regenerar com `npm run generate:api`.

## Estado e responsabilidades

- `TransactionsStore` (`@Injectable()` **sem** `providedIn`; fornecido pela página, então nada fica em memória entre utilizadores nem entre visitas). Dono de filtro, página, estado (`loading/loaded/error`) e dados. A consulta depende de `ReferenceMonthStore.monthKey`, `CurrencyStore.selectedCurrency`, filtro e página; trocar mês, moeda ou filtro volta à página 1 e a consulta anterior é cancelada (`switchMap`). `refresh()` recarrega depois de salvar/excluir; se a página deixar de existir, vai para a última.
- `TransactionsApiService`: cliente HTTP sem estado.
- `TransactionsPage` (filtro, subtotais, estados, paginação, confirmação), `TransactionsList` (tabela a partir de `md`, cartões abaixo; apresentação pura) e `TransactionDialog` (formulário tipado).
- `budget-alert-message.ts`: texto e tipo do toast do alerta de meta (DESC-BUD-07).
- Valor monetário: `MoneyInputDirective` (`shared/forms`) guarda o decimal da API como texto (`"1234.56"`), mostra com vírgula e nunca usa `number`. A formatação de exibição é `formatMoney`/`formatDate` (`shared`).

## Regras e decisões

- O cliente envia `referenceMonth` (mês exibido); o servidor só avalia alerta de meta se a data do lançamento pertence a ele (P-20). O alerta é calculado pelo módulo de metas (F6): só a piora de estado alerta, categoria antes do global; o mapeamento de toasts está em `budget-alert-message.ts`.
- Ao salvar um lançamento em moeda diferente da selecionada, a interface passa a selecionar essa moeda (P-15).
- As categorias do diálogo vêm do `CategoriesStore` (a mesma fonte da página de categorias), filtradas pelo tipo (`Ambos` nos dois); trocar o tipo limpa uma categoria incompatível e trocar a categoria limpa a subcategoria.
- Erros do servidor: `CATEGORY_TYPE_MISMATCH` e `SUBCATEGORY_NOT_IN_CATEGORY` aparecem no campo; `VALIDATION_FAILED` por campo (`errors[]`); categoria removida em outra aba (404) recarrega os seletores.
- Valor com sinal e cor (entrada `+` azul, saída `-` vermelha; subtotais `Entradas` azul, `Saídas` vermelho e `Saldo` verde, vermelho se negativo), sem depender só da cor.
- Pendente: edição por `getTransaction` não é necessária; o saldo/aggregates do dashboard (F7) leem os mesmos lançamentos.

## Validação

- `data-access/transactions.store.spec.ts`, `pages/transactions-page.spec.ts`, `ui/transaction-dialog.spec.ts`, `budget-alert-message.spec.ts` e `shared/forms/money-input.spec.ts` (Vitest): consulta por mês/moeda/filtro/página, volta à página 1, erro e nova tentativa, página que deixou de existir, intervalo `1–15 de N`, formatação e sinais, subtotais, exclusão com confirmação, validações e mensagens exatas, categorias por tipo, decimal exato no corpo, `referenceMonth`, troca de moeda, alertas, erros do servidor por campo, edição preenchida.
- Verificado em 2026-10-29 no navegador contra a API real com PostgreSQL: cadastro (11 categorias), criação de despesa com subcategoria e valor digitado `1234,5x9` (normalizado para `1234,59`), lista, subtotais e toast; exclusão de categoria com lançamento recusada com a mensagem esperada.
- Verificado em 2026-10-29 (F6): com meta padrão 1000, uma despesa de 850 mostrou o toast `Você já usou 80% da sua meta mensal.`.
- Pendente: teclado completo, 320 px e contraste nos dois temas.
