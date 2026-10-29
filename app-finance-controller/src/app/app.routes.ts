import { Routes } from '@angular/router';
import { AUTH_ROUTES } from './features/auth/auth.routes';
import { authGuard } from './features/auth/guards/auth.guard';

const comingSoon = () =>
  import('./features/coming-soon/coming-soon-page').then((module) => module.ComingSoonPage);

/** Fatia futura ainda sem página: cada fatia substitui a sua rota por `loadChildren`/`loadComponent` reais. */
function placeholder(path: string, title: string, showMonth: boolean) {
  return { path, title, data: { title, showMonth }, loadComponent: comingSoon };
}

export const routes: Routes = [
  ...AUTH_ROUTES,
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./core/layout/shell').then((module) => module.ShellComponent),
    children: [
      {
        path: '',
        pathMatch: 'full',
        loadChildren: () =>
          import('./features/dashboard/dashboard.routes').then((module) => module.DASHBOARD_ROUTES),
      },
      {
        path: 'lancamentos',
        loadChildren: () =>
          import('./features/transactions/transactions.routes').then((module) => module.TRANSACTIONS_ROUTES),
      },
      placeholder('relatorios', 'Relatórios', true),
      {
        path: 'categorias',
        loadChildren: () =>
          import('./features/categories/categories.routes').then((module) => module.CATEGORIES_ROUTES),
      },
      placeholder('cofrinhos', 'Cofrinhos', false),
      {
        path: 'metas',
        loadChildren: () => import('./features/budget/budget.routes').then((module) => module.BUDGET_ROUTES),
      },
      {
        path: '**',
        title: 'Página não encontrada',
        loadComponent: () =>
          import('./features/not-found/not-found-page').then((module) => module.NotFoundPage),
      },
    ],
  },
];
