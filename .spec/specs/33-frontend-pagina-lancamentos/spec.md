# 33-frontend-pagina-lancamentos

> **ID:** FRONTEND-SPEC-10 · **Categoria:** Frontend (`features/transactions`) · **Estado:** proposta para revisão — não aprovada, não executada.

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

- [ ] A lista mostra só lançamentos do mês e da moeda selecionados, mais recentes primeiro, 15 por página.
- [ ] Os subtotais cobrem todas as páginas do filtro, não só a visível.
- [ ] Salvar uma despesa que cruza 80% mostra um único aviso; que cruza 100% mostra o erro; receita não mostra alerta.
- [ ] Excluir só ocorre após confirmação e atualiza lista e subtotais.
- [ ] Editar abre o diálogo preenchido e preserva a data e a moeda do lançamento; trocar a categoria de um lançamento atualiza imediatamente lista, subtotais e agregados.
- [ ] Salvar um lançamento em EUR com BRL selecionado passa a mostrar a lista de EUR.
- [ ] Despesa datada fora do mês selecionado salva sem alerta de meta.
- [ ] Estados de carregando, vazio, erro e sucesso presentes; toasts anunciados a leitores de tela.

### Skills de implementação

- **Skills obrigatórias:** `angular-module-scaffold`, `angular-tailwind-ui-specialist`, `angular-architect`.
- **Caminhos:** `.claude/skills/angular-module-scaffold/SKILL.md`; `.claude/skills/angular-tailwind-ui-specialist/SKILL.md`; `.claude/skills/angular-architect/SKILL.md`.
- **Responsabilidade:** módulo e `doc.md`; tabela/cartões e diálogo acessíveis; estado e formulários tipados.
- **Momento de utilização:** depois do contrato `transactions`.
- **Dependências:** FRONTEND-SPEC-00…05, 07; BACKEND-SPEC-11.
- **Limitações:** a skill de módulo exige seleção explícita das operações.

## Tasks

### Front-end

- [ ] Criar `features/transactions` com página, tabela, diálogo, serviço de dados e `doc.md`.

### Validação

- [ ] Testar filtros, paginação, alertas, edição e exclusão; revisar UI.

## Resultado esperado

- Gestão completa de lançamentos equivalente à web da origem, com validação servidor+cliente.
- Preservados os textos, a ordenação e os alertas de meta.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
