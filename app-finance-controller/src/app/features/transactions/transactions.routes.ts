import { Routes } from '@angular/router';

/** Rotas de `/lancamentos` (dentro do shell autenticado); mostra o seletor de mês; página carregada sob demanda. */
export const TRANSACTIONS_ROUTES: Routes = [
  {
    path: '',
    title: 'Lançamentos',
    data: { title: 'Lançamentos', showMonth: true },
    loadComponent: () => import('./pages/transactions-page').then((module) => module.TransactionsPage),
  },
];
