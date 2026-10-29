# features/coming-soon

Página provisória genérica "Em breve" (`ComingSoonPage`), carregada por `loadComponent` em `app.routes.ts`.

- O título vem de `route.data['title']` (padrão "Em breve") e é exibido no `h1` por `app-page-header`.
- Usada pelas rotas Dashboard, Lançamentos, Relatórios, Cofrinhos e Meta (Categorias saiu na F4) enquanto as fatias não existem.
- **Cada fatia futura substitui a sua rota** em `app.routes.ts` (por `loadChildren` do seu `<modulo>.routes.ts`, mantendo `title` e `data.showMonth`) e deixa de usar esta página. Quando nenhuma rota a usar mais, o feature pode ser removido.
- Não consome API.

# features/not-found

`NotFoundPage` (rota `**` dentro do shell): título "Página não encontrada" e link para o Dashboard. Sem estado nem API.
