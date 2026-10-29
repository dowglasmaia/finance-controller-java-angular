import { Routes } from '@angular/router';
import { guestGuard } from './guards/auth.guard';

/** Rotas públicas de autenticação, fora do shell (recuperação de senha sem guarda: o link funciona com ou sem sessão). Cada página é carregada sob demanda. */
export const AUTH_ROUTES: Routes = [
  {
    path: 'entrar',
    title: 'Entrar',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/login/login-page').then((module) => module.LoginPage),
  },
  {
    path: 'esqueci-senha',
    title: 'Esqueci minha senha',
    loadComponent: () =>
      import('./pages/forgot-password/forgot-password-page').then((module) => module.ForgotPasswordPage),
  },
  {
    path: 'redefinir-senha',
    title: 'Redefinir senha',
    loadComponent: () =>
      import('./pages/reset-password/reset-password-page').then((module) => module.ResetPasswordPage),
  },
  {
    path: 'cadastrar',
    title: 'Cadastrar',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/register/register-page').then((module) => module.RegisterPage),
  },
];
