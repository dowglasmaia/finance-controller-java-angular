import { Routes } from '@angular/router';
import { guestGuard } from './guards/auth.guard';

/** Rotas públicas de autenticação, fora do shell. Cada página é carregada sob demanda. */
export const AUTH_ROUTES: Routes = [
  {
    path: 'entrar',
    title: 'Entrar',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/login/login-page').then((module) => module.LoginPage),
  },
  {
    path: 'cadastrar',
    title: 'Cadastrar',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/register/register-page').then((module) => module.RegisterPage),
  },
];
