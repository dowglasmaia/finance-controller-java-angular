import { Routes } from '@angular/router';

/** Rotas de `/cofrinhos` (dentro do shell autenticado); não depende de mês; página carregada sob demanda. */
export const MONEY_BOXES_ROUTES: Routes = [
  {
    path: '',
    title: 'Cofrinhos',
    data: { title: 'Cofrinhos', showMonth: false },
    loadComponent: () => import('./pages/money-boxes-page').then((module) => module.MoneyBoxesPage),
  },
];
