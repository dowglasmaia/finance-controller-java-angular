# features/categories

Página `/categorias` (FRONTEND-SPEC-11, fatia F4): lista por aba (Despesas/Receitas; `Ambos` aparece nas duas), criação, edição e exclusão de categorias, e gestão de subcategorias.

## Jornadas e rotas

| Jornada | Rota | Resultado esperado |
|---|---|---|
| Listar | `/categorias` (`categories.routes.ts`, carregada sob demanda) | Cartões com ícone e cor, nome e até 3 subcategorias (+N). Vazio: `Nenhuma categoria cadastrada.` |
| Criar | botão `Nova Categoria` | Diálogo; subcategorias montadas localmente e enviadas ao salvar; `Categoria salva.` |
| Editar | `Editar` no cartão | Mesmo diálogo; adicionar/remover subcategoria grava na hora |
| Excluir | `Excluir` no cartão | Confirmação; `Categoria excluída.` ou erro do servidor em toast |

## Contrato consumido (`core/api/generated/categories.ts`)

`listCategories`, `createCategory`, `updateCategory`, `deleteCategory`, `addSubcategory`, `deleteSubcategory`. `getCategory` existe no contrato e não é usada (a lista já traz tudo). O OpenAPI (`categories.yaml`) é a fonte de verdade; regenerar com `npm run generate:api`.

## Estado e responsabilidades

- `CategoriesStore` (`data-access`, `providedIn: 'root'`) é o único dono da lista (`categories`, `status`, `expenseCategories`, `incomeCategories`). Toda mutação atualiza o estado a partir da resposta do servidor, então os seletores de Lançamentos (F5) e Metas (F6) devem ler daqui e refletem a mudança sem recarregar. É limpo no logout via `AppStateReset`.
- `CategoriesApiService`: cliente HTTP sem estado.
- `CategoriesPage` (abas, estados carregando/erro/vazio, diálogos), `CategoryCard` (apresentação) e `CategoryDialog` (formulário tipado; usa o store).
- `category-catalog.ts`: 24 ícones e 12 cores oferecidos. O servidor guarda o identificador do ícone (nomes da semente, ex.: `directions_car`); o frontend o traduz para um ícone Lucide e desconhecidos caem em "Etiqueta".

## Regras e decisões

- Mensagens: duplicado → `Já existe uma categoria com este nome.` no campo Nome (diálogo continua aberto); `CATEGORY_TYPE_CONFLICT` → erro no campo Tipo; `CATEGORY_IN_USE` → toast `Categoria possui lançamentos vinculados e não pode ser excluída.`
- "Outros" aparece com cadeado e sem botão de remover (a proteção real é do servidor: `SUBCATEGORY_PROTECTED`).
- Remover subcategoria na edição sempre pede confirmação explicando que os lançamentos ficarão sem subcategoria (P-11); o frontend não sabe se há lançamentos, e a desassociação real entra com a F5.
- Excluir categoria com lançamentos e mudar o tipo com lançamentos incompatíveis passaram a ser recusados pelo servidor real na F5 (`CATEGORY_IN_USE`, `CATEGORY_TYPE_CONFLICT`); a página já tratava os dois.
- Utilizadores criados antes da F4 não têm categorias semeadas (a semente roda no cadastro); veem o estado vazio e criam as suas.
- `Modal` e `ConfirmDialog` são genéricos e ficam em `shared/ui`.

## Validação

- `data-access/categories.store.spec.ts`, `pages/categories-page.spec.ts` e `ui/category-dialog.spec.ts` (Vitest): ordenação, abas e `Ambos`, chips +N, vazio/erro/tentar novamente, exclusão com sucesso e bloqueada, duplicado no campo, conflito de tipo, "Outros" sem remoção, confirmação ao remover, limpeza no logout.
- Verificado em 2026-10-29 no navegador contra a API real com PostgreSQL: estado vazio, criação com subcategoria via Enter, erro de duplicado no campo, exclusão com confirmação e toasts.
- Pendente: verificação de teclado completa, 320 px e contraste nos dois temas.
