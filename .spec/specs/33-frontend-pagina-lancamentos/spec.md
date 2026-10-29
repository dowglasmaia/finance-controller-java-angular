# 33-frontend-pagina-lancamentos

> **ID:** FRONTEND-SPEC-10 · **Categoria:** Frontend (`features/transactions`) · **Estado:** executada na F5 (2026-10-29); o alerta real de meta e a verificação manual completa ficam pendentes — ver evidências.

## Objetivo

Construir a página de lançamentos: lista mensal paginada e filtrada, subtotais, formulário em diálogo, exclusão com confirmação e alerta de meta ao salvar despesas.

## Escopo

- Inclui: página `/lancamentos`, filtros, paginação, subtotais, diálogo criar/editar, exclusão, alertas.
- Não inclui: regras de validação oficiais (backend), gerenciamento de categorias.

## Contexto técnico local

### Página (CONFIRMADO — DESC-TXN-09)

- Cabeçalho `Lançamentos` / `Gerencie suas entradas e saídas` com `MonthSwitcher` e botão `Novo Lançamento`.
- Painel de filtros: seletor `Todos | Entradas | Saídas` e subtotais do conjunto filtrado (entradas, saídas, saldo, com cores).
- Tabela: DATA, DESCRIÇÃO, CATEGORIA, SUBCATEGORIA, VALOR (sinal e cor), AÇÕES (editar, excluir); linhas alternadas; página de 15; rodapé `1–15 de N` com anterior/próximo e `página/total`.
- Trocar o filtro volta à primeira página; ajustar a página se o total cair.
- Vazio: `Nenhum lançamento no período.`
- Em telas estreitas as linhas viram cartões (FRONTEND-SPEC-07).

### Formulário (diálogo, FRONTEND-SPEC-05)

Título `Novo Lançamento` ou `Editar Lançamento`; botões `Cancelar` e `Salvar`. Ao salvar: toast `Lançamento salvo.`, atualização da lista e dos agregados; se a resposta trouxer `budgetAlert`, exibe **um** toast conforme a prioridade da origem:

| `budgetAlert` | Mensagem (DESC-BUD-07) | Toast |
|---|---|---|
| `CATEGORY` + `EXCEEDED` | `Meta de {categoria} estourada!` | erro |
| `CATEGORY` + `WARNING` | `Você está perto da meta de {categoria}.` | aviso |
| `GLOBAL` + `EXCEEDED` | `Meta mensal estourada!` | erro |
| `GLOBAL` + `WARNING` | `Você já usou 80% da sua meta mensal.` | aviso |

Exclusão: `ConfirmDialog` com a descrição; sucesso `Lançamento excluído.`.

### Operações consumidas

`listTransactions`, `createTransaction`, `updateTransaction`, `deleteTransaction`, `getTransaction` (edição), `listCategories` (seletores).

### Pendências

Decisões aplicadas: P-15 — cada moeda tem os seus lançamentos, despesas e dashboard, e ao salvar um lançamento noutra moeda a interface passa a selecionar essa moeda (lógica existente); P-20 — o cliente envia `referenceMonth` e o alerta só aparece se a data do lançamento pertence a esse mês. As rotas `new/edit` do mobile não se aplicam.

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [BACKEND-SPEC-11](../16-backend-modulo-lancamentos/spec.md)
- [FRONTEND-SPEC-05](../28-frontend-formularios-e-validacoes/spec.md)

## Regras e critérios de aceitação

- [x] A lista mostra só lançamentos do mês e da moeda selecionados, mais recentes primeiro, 15 por página.
  > ✅ 2026-10-29 10:50 +00:00 — `transactions.store.spec.ts` (consulta com `month`, `currency`, `type`, `page`, `size=15`; trocar mês, moeda ou filtro volta à página 1 e cancela a anterior) e `transactions-page.spec.ts`; visto no navegador (mês e moeda do shell). A ordem mais recentes primeiro vem do servidor (RN-TXN-08, testada no backend).
- [x] Os subtotais cobrem todas as páginas do filtro, não só a visível.
  > ✅ 2026-10-29 10:50 +00:00 — A página mostra `totals` do servidor, calculados sobre o conjunto filtrado inteiro (`transactions-page.spec.ts` com 40 itens e totais independentes da página; backend: `listIsScoped…`).
- [ ] Salvar uma despesa que cruza 80% mostra um único aviso; que cruza 100% mostra o erro; receita não mostra alerta.
  > ⏳ 2026-10-29 10:50 +00:00 — Parcial: `transaction-dialog.spec.ts` (resposta com `budgetAlert` → um toast `Meta de Alimentação estourada!` além de `Lançamento salvo.`; receita sem alerta) e `budget-alert-message.spec.ts` (as 4 combinações de escopo e estado com texto e tipo exatos). Falta: alerta real, que depende do módulo de metas (F6).
- [x] Excluir só ocorre após confirmação e atualiza lista e subtotais.
  > ✅ 2026-10-29 10:50 +00:00 — `transactions-page.spec.ts` (nenhum `DELETE` antes de confirmar; depois, `Lançamento excluído.`, recarga e subtotais atualizados).
- [ ] Editar abre o diálogo preenchido e preserva a data e a moeda do lançamento; trocar a categoria de um lançamento atualiza imediatamente lista, subtotais e agregados.
  > ⏳ 2026-10-29 10:50 +00:00 — Parcial: `transaction-dialog.spec.ts` (campos preenchidos, data e moeda preservadas, `PUT`) e o `refresh()` recarrega lista e subtotais após salvar. Falta: os agregados do dashboard (F7), que ainda não existem.
- [x] Salvar um lançamento em EUR com BRL selecionado passa a mostrar a lista de EUR.
  > ✅ 2026-10-29 10:50 +00:00 — `transaction-dialog.spec.ts` (resposta em EUR → `CurrencyStore` passa a EUR e a lista recarrega nessa moeda, P-15).
- [x] Despesa datada fora do mês selecionado salva sem alerta de meta.
  > ✅ 2026-10-29 10:50 +00:00 — O cliente envia `referenceMonth` (`transaction-dialog.spec.ts`) e o servidor só avalia alerta dentro dele (`alertIsOnlyEvaluatedForExpensesInTheReferenceMonth`).
- [ ] Estados de carregando, vazio, erro e sucesso presentes; toasts anunciados a leitores de tela.
  > ⏳ 2026-10-29 10:50 +00:00 — `transactions-page.spec.ts` (carregando, vazio, erro com "Tentar novamente", sucesso) e toasts no contêiner `aria-live` do shell (F2). Falta: verificação com leitor de tela.

### Skills de implementação

- **Skills obrigatórias:** `angular-module-scaffold`, `angular-tailwind-ui-specialist`, `angular-architect`.
- **Caminhos:** `.claude/skills/angular-module-scaffold/SKILL.md`; `.claude/skills/angular-tailwind-ui-specialist/SKILL.md`; `.claude/skills/angular-architect/SKILL.md`.
- **Responsabilidade:** módulo e `doc.md`; tabela/cartões e diálogo acessíveis; estado e formulários tipados.
- **Momento de utilização:** depois do contrato `transactions`.
- **Dependências:** FRONTEND-SPEC-00…05, 07; BACKEND-SPEC-11.
- **Limitações:** a skill de módulo exige seleção explícita das operações.

## Tasks

### Front-end

- [x] Criar `features/transactions` com página, tabela, diálogo, serviço de dados e `doc.md`.
  > ✅ 2026-10-29 10:50 +00:00 — `features/transactions` (`transactions.routes.ts`, `pages/`, `ui/`, `data-access/`, `budget-alert-message.ts`, `doc.md`); tipos gerados em `core/api/generated/transactions.ts`; `MoneyInputDirective` em `shared/forms`. Build de produção verde; rota `/lancamentos` sob demanda no lugar do placeholder. Desvio: a edição usa os dados da lista, não `getTransaction`.

### Validação

- [ ] Testar filtros, paginação, alertas, edição e exclusão; revisar UI.
  > ⏳ 2026-10-29 10:50 +00:00 — `npm test`: 127 testes Vitest verdes (37 novos em 5 ficheiros). Navegador contra API real (desktop): criação com subcategoria, valor `1234,5x9` normalizado, lista, subtotais, toast e bloqueio de exclusão de categoria em uso. Falta: revisão em 320/768 px, teclado completo e contraste nos dois temas.

## Resultado esperado

- Gestão completa de lançamentos equivalente à web da origem, com validação servidor+cliente.
- Preservados os textos, a ordenação e os alertas de meta.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
