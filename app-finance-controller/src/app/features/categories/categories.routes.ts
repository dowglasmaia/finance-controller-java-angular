import { Routes } from '@angular/router';

/** Rotas de `/categorias` (dentro do shell autenticado); a página é carregada sob demanda. */
export const CATEGORIES_ROUTES: Routes = [
  {
    path: '',
    title: 'Categorias',
    data: { title: 'Categorias', showMonth: false },
    loadComponent: () => import('./pages/categories-page').then((module) => module.CategoriesPage),
  },
];
