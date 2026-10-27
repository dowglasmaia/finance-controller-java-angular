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
      { ...placeholder('', 'Dashboard', true), pathMatch: 'full' },
      placeholder('lancamentos', 'Lançamentos', true),
      placeholder('relatorios', 'Relatórios', true),
      placeholder('categorias', 'Categorias', false),
      placeholder('cofrinhos', 'Cofrinhos', false),
      placeholder('metas', 'Meta', true),
      {
        path: '**',
        title: 'Página não encontrada',
        loadComponent: () =>
          import('./features/not-found/not-found-page').then((module) => module.NotFoundPage),
      },
    ],
  },
];
