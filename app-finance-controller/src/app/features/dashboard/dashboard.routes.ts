import { Routes } from '@angular/router';

/** Rotas de `/` (dentro do shell autenticado); mostra o seletor de mês; página carregada sob demanda. */
export const DASHBOARD_ROUTES: Routes = [
  {
    path: '',
    title: 'Dashboard',
    data: { title: 'Dashboard', showMonth: true },
    loadComponent: () => import('./pages/dashboard-page').then((module) => module.DashboardPage),
  },
];
