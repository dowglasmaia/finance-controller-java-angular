# Exemplo de módulo: categorias

Este exemplo mostra o padrão esperado para um módulo que consulta e altera categorias. Os nomes de operações são ilustrativos: o módulo real deve usar somente os `operationId`s do OpenAPI aprovado.

## Estrutura

```text
features/categorias/
├── categorias.routes.ts
├── doc.md
├── data-access/
│   ├── categorias-api.service.ts
│   └── categoria.mapper.ts
├── pages/
│   ├── categorias-lista.page.ts
│   └── categoria-formulario.page.ts
└── ui/
    └── categoria-form.component.ts
```

O módulo contém `data-access/` porque consome a API, `pages/` porque tem duas jornadas de rota e `ui/` porque o formulário é reutilizado pelas jornadas de criação e edição. O mapper só existe se os tipos da API não atenderem diretamente às necessidades da interface.

## Rotas

```ts
export const CATEGORIAS_ROUTES: Routes = [
  {path: '', component: CategoriasListaPage},
  {path: 'nova', component: CategoriaFormularioPage},
  {path: ':id/editar', component: CategoriaFormularioPage},
];
```

Os nomes concretos, lazy loading e parâmetros dependem da convenção já existente e do contrato aprovado. Não é obrigatório repetir este código literalmente.

## Exemplo de doc.md

```md
# Categorias

## Propósito

Permitir organizar lançamentos em categorias e subcategorias.

## Jornadas e rotas

| Jornada | Rota | Resultado esperado |
|---|---|---|
| Listar categorias | `/categorias` | Exibe categorias, estado vazio e erro de consulta. |
| Criar categoria | `/categorias/nova` | Valida dados e cria uma categoria pelo contrato aprovado. |
| Editar categoria | `/categorias/:id/editar` | Carrega e atualiza a categoria selecionada. |

## Contrato consumido

| Operação | Finalidade | Estados tratados |
|---|---|---|
| `listCategories` | Consultar categorias. | Carregamento, sucesso, vazio e falha. |
| `createCategory` | Criar categoria. | Validação, sucesso, conflito e falha. |
| `updateCategory` | Atualizar categoria. | Carregamento, validação, sucesso, conflito e falha. |

## Estado e responsabilidades

- A página de lista é responsável pela consulta e pela apresentação dos estados.
- O formulário é responsável por validação de experiência e emissão dos dados.
- O serviço de acesso adapta operações OpenAPI; regras de unicidade permanecem no backend.

## Validação

- Testes da lista, do formulário e dos erros de contrato relevantes.
- Verificação de rotas e de compilação de templates.
```

O `doc.md` deve ser atualizado ao alterar as jornadas, operações, proprietário do estado ou validação. Não copiar schemas ou respostas completas do OpenAPI para o documento.

