# 34-frontend-pagina-categorias

> **ID:** FRONTEND-SPEC-11 · **Categoria:** Frontend (`features/categories`) · **Estado:** proposta para revisão — não aprovada, não executada.

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

- [ ] Uma categoria `Ambos` aparece nas duas abas.
- [ ] Criar com nome existente (em qualquer caixa) mostra `Já existe uma categoria com este nome.` no campo Nome sem fechar o diálogo.
- [ ] Excluir categoria com lançamentos mostra `Categoria possui lançamentos vinculados e não pode ser excluída.` e nada muda.
- [ ] "Outros" não tem ação de remover.
- [ ] Remover uma subcategoria usada por lançamentos pede confirmação que explica que eles ficarão sem subcategoria.
- [ ] Mudar o tipo de uma categoria com lançamentos incompatíveis mostra o erro no campo Tipo e não altera nada.
- [ ] Após salvar, os seletores de categoria de Lançamentos e Metas refletem a mudança.
- [ ] Vazio, carregando e erro tratados; chips e ações acessíveis por teclado.

### Skills de implementação

- **Skills obrigatórias:** `angular-module-scaffold`, `angular-tailwind-ui-specialist`, `angular-architect`.
- **Caminhos:** `.claude/skills/angular-module-scaffold/SKILL.md`; `.claude/skills/angular-tailwind-ui-specialist/SKILL.md`; `.claude/skills/angular-architect/SKILL.md`.
- **Responsabilidade:** módulo e `doc.md`; grade, abas e diálogo; formulários tipados.
- **Momento de utilização:** depois do contrato `categories`.
- **Dependências:** FRONTEND-SPEC-00…05; BACKEND-SPEC-10.
- **Limitações:** nenhuma específica.

## Tasks

### Front-end

- [ ] Criar `features/categories` com página, cartão, diálogo e serviço de dados.

### Validação

- [ ] Testar abas, unicidade, exclusão bloqueada e proteção de "Outros".

## Resultado esperado

- Gestão de categorias equivalente à web da origem, com erros claros do servidor.
- Preservada a regra de "Outros" imutável.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
