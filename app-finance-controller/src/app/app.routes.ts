import { Routes } from '@angular/router';
import { AUTH_ROUTES } from './features/auth/auth.routes';
import { authGuard } from './features/auth/guards/auth.guard';

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
      {
        path: 'relatorios',
        loadChildren: () => import('./features/reports/reports.routes').then((module) => module.REPORTS_ROUTES),
      },
      {
        path: 'categorias',
        loadChildren: () =>
          import('./features/categories/categories.routes').then((module) => module.CATEGORIES_ROUTES),
      },
      {
        path: 'cofrinhos',
        loadChildren: () =>
          import('./features/money-boxes/money-boxes.routes').then((module) => module.MONEY_BOXES_ROUTES),
      },
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
