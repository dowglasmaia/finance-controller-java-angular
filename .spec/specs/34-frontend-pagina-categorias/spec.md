# 34-frontend-pagina-categorias

> **ID:** FRONTEND-SPEC-11 · **Categoria:** Frontend (`features/categories`) · **Estado:** executada na F4 (2026-10-29); critérios com lançamentos fechados na F5; resta o seletor de Metas (F6) e a verificação manual — ver evidências.

## Objetivo

Construir a página de categorias e subcategorias: grade por aba (Despesas/Receitas), criação e edição em diálogo, exclusão com bloqueio quando há lançamentos.

## Escopo

- Inclui: página `/categorias`, abas, cartões, diálogo de categoria, gestão de subcategorias.
- Não inclui: regras de unicidade e exclusão (backend).

## Contexto técnico local

### Página (CONFIRMADO — DESC-CAT-04)

- Cabeçalho `Categorias` / `Organize suas despesas e receitas`; botão `Nova Categoria`.
- Abas `Despesas` (categorias que aceitam despesa) e `Receitas` (as que aceitam receita); `Ambos` aparece nas duas. Vazio: `Nenhuma categoria cadastrada.`
- Cartão: ícone com a cor da categoria, nome, até 3 subcategorias como chips e `+N` para as demais, ações `Editar` e `Excluir`.

### Diálogo (DESC-CAT-01, DESC-SUB-01/04)

- Título `Nova Categoria` / `Editar Categoria`; campos Nome\*, Tipo (Despesa, Receita, Ambos), grade de 24 ícones, 12 cores, Subcategorias e "Nova subcategoria" (Enter ou botão `+`).
- "Outros" aparece com cadeado e sem remover; subcategoria vazia ignorada.
- Criação: subcategorias montadas localmente e enviadas ao salvar. Edição: adicionar/remover grava imediatamente.
- Sucesso `Categoria salva.`; exclusão com confirmação e `Categoria excluída.`; erros do servidor nos campos ou toasts (FRONTEND-SPEC-03).

### Operações consumidas

`listCategories`, `createCategory`, `updateCategory`, `deleteCategory`, `addSubcategory`, `deleteSubcategory`.

### Pendências

Decisões aplicadas: P-11 — excluir subcategoria com lançamentos é permitido e os lançamentos ficam sem subcategoria (a interface avisa isso na confirmação); P-12 — sem limite de categorias; P-13 — mudar o tipo com lançamentos incompatíveis é recusado com mensagem no campo Tipo. Duplicar "Outros" ou nomes iguais é recusado pelo servidor (BACKEND-SPEC-04).

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [BACKEND-SPEC-10](../15-backend-modulo-categorias/spec.md)
- [FRONTEND-SPEC-05](../28-frontend-formularios-e-validacoes/spec.md)

## Regras e critérios de aceitação

- [x] Uma categoria `Ambos` aparece nas duas abas.
  > ✅ 2026-10-29 10:10 +00:00 — `categories-page.spec.ts` (Extras `BOTH` em Despesas e em Receitas) e `categories.store.spec.ts`.
- [x] Criar com nome existente (em qualquer caixa) mostra `Já existe uma categoria com este nome.` no campo Nome sem fechar o diálogo.
  > ✅ 2026-10-29 10:10 +00:00 — `category-dialog.spec.ts` (aria-invalid no campo Nome, mensagem exata, diálogo aberto) e no navegador com a API real (`  pETS ` com `Pets` existente).
- [x] Excluir categoria com lançamentos mostra `Categoria possui lançamentos vinculados e não pode ser excluída.` e nada muda.
  > ⏳ 2026-10-29 10:10 +00:00 — Parcial: `categories-page.spec.ts` com o 409 `CATEGORY_IN_USE` simulado (toast com a mensagem e lista intacta). Falta: com lançamentos reais (F5).
  > ✅ 2026-10-29 10:50 +00:00 — Fechado na F5: no navegador contra a API real, excluir "Alimentação" com um lançamento mostrou o toast `Categoria possui lançamentos vinculados e não pode ser excluída.` e a lista não mudou.
- [x] "Outros" não tem ação de remover.
  > ✅ 2026-10-29 10:10 +00:00 — `category-dialog.spec.ts` (criação e edição: sem botão `Remover Outros`, com cadeado) e visto no navegador.
- [x] Remover uma subcategoria usada por lançamentos pede confirmação que explica que eles ficarão sem subcategoria.
  > ✅ 2026-10-29 10:10 +00:00 — `category-dialog.spec.ts` (confirmação `ficarão sem subcategoria`, sem chamada à API antes de confirmar). Desvio: o frontend não sabe se há lançamentos, então a confirmação aparece sempre na edição.
- [x] Mudar o tipo de uma categoria com lançamentos incompatíveis mostra o erro no campo Tipo e não altera nada.
  > ⏳ 2026-10-29 10:10 +00:00 — Parcial: `category-dialog.spec.ts` com `CATEGORY_TYPE_CONFLICT` simulado (erro no campo Tipo, `aria-invalid`). Falta: com lançamentos reais (F5).
  > ✅ 2026-10-29 10:50 +00:00 — Fechado na F5 no que cabe ao frontend: o servidor real devolve `CATEGORY_TYPE_CONFLICT` (teste de integração do backend) e o diálogo o mostra no campo Tipo (`category-dialog.spec.ts`).
- [ ] Após salvar, os seletores de categoria de Lançamentos e Metas refletem a mudança.
  > ⏳ 2026-10-29 10:10 +00:00 — Parcial: `CategoriesStore` (`providedIn: 'root'`) já é a fonte única e se atualiza a cada mutação (`categories.store.spec.ts`). Falta: os seletores de Lançamentos (F5) e Metas (F6), que ainda não existem, devem ler dele.
  > ⏳ 2026-10-29 10:50 +00:00 — Parcial: o diálogo de lançamentos (F5) já lê do `CategoriesStore` (seletor por tipo, `transaction-dialog.spec.ts`), então reflete criar/editar/excluir categoria. Falta: o seletor de Metas (F6).
- [ ] Vazio, carregando e erro tratados; chips e ações acessíveis por teclado.
  > ⏳ 2026-10-29 10:10 +00:00 — Parcial: vazio, carregando e erro com "Tentar novamente" testados (`categories-page.spec.ts`) e vistos no navegador; ações e chips são botões/itens nativos. Falta: percurso completo por teclado, 320 px e contraste nos dois temas (verificação manual).

### Skills de implementação

- **Skills obrigatórias:** `angular-module-scaffold`, `angular-tailwind-ui-specialist`, `angular-architect`.
- **Caminhos:** `.claude/skills/angular-module-scaffold/SKILL.md`; `.claude/skills/angular-tailwind-ui-specialist/SKILL.md`; `.claude/skills/angular-architect/SKILL.md`.
- **Responsabilidade:** módulo e `doc.md`; grade, abas e diálogo; formulários tipados.
- **Momento de utilização:** depois do contrato `categories`.
- **Dependências:** FRONTEND-SPEC-00…05; BACKEND-SPEC-10.
- **Limitações:** nenhuma específica.

## Tasks

### Front-end

- [x] Criar `features/categories` com página, cartão, diálogo e serviço de dados.
  > ✅ 2026-10-29 10:10 +00:00 — `features/categories` (`categories.routes.ts`, `pages/`, `ui/`, `data-access/`, `category-catalog.ts`, `doc.md`), tipos gerados em `core/api/generated/categories.ts` (`npm run generate:api`), e `Modal`/`ConfirmDialog` em `shared/ui`. Build de produção verde; rota `/categorias` carregada sob demanda no lugar do placeholder.

### Validação

- [x] Testar abas, unicidade, exclusão bloqueada e proteção de "Outros".
  > ✅ 2026-10-29 10:10 +00:00 — `npm test`: 90 testes Vitest verdes (17 novos em 3 ficheiros). Navegador contra API real: estado vazio, criação com subcategoria (Enter), duplicado no campo, exclusão com confirmação e toasts. Correção feita ao verificar: texto do botão "Excluir" com contraste fraco no tema escuro (`text-surface`).

## Resultado esperado

- Gestão de categorias equivalente à web da origem, com erros claros do servidor.
- Preservada a regra de "Outros" imutável.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
