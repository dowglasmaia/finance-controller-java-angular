import { Routes } from '@angular/router';

/** Rotas de `/relatorios` (dentro do shell autenticado); mostra o seletor de mês; página carregada sob demanda. */
export const REPORTS_ROUTES: Routes = [
  {
    path: '',
    title: 'Relatórios',
    data: { title: 'Relatórios', showMonth: true },
    loadComponent: () => import('./pages/reports-page').then((module) => module.ReportsPage),
  },
];
