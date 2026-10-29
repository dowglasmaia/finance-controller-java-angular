import { Routes } from '@angular/router';

/** Rotas de `/metas` (dentro do shell autenticado); mostra o seletor de mês; página carregada sob demanda. */
export const BUDGET_ROUTES: Routes = [
  {
    path: '',
    title: 'Meta de Gastos',
    data: { title: 'Meta de Gastos', showMonth: true },
    loadComponent: () => import('./pages/budget-page').then((module) => module.BudgetPage),
  },
];
