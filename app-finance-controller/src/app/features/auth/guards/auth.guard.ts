import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SessionStore } from '../../../core/state/session.store';
import { AuthService } from '../data-access/auth.service';

/** Aceita apenas caminhos internos ("/x"); recusa "//x", "/\x" e qualquer outra coisa. */
export function safeReturnUrl(raw: unknown, fallback = '/'): string {
  if (typeof raw !== 'string' || !raw.startsWith('/') || raw.startsWith('//') || raw.startsWith('/\\')) {
    return fallback;
  }
  // eslint-disable-next-line no-control-regex
  return /[\u0000-\u001f\u007f\\]/.test(raw) ? fallback : raw;
}

export const authGuard: CanActivateFn = async (_route, state) => {
  const auth = inject(AuthService);
  const session = inject(SessionStore);
  const router = inject(Router);

  await auth.ensureInitialized();
  if (session.isAuthenticated()) {
    return true;
  }
  // Apenas o caminho: query e fragmento podem conter dados financeiros.
  const path = safeReturnUrl(state.url.split(/[?#]/)[0]);
  return router.createUrlTree(['/entrar'], path === '/' ? {} : { queryParams: { returnUrl: path } });
};

export const guestGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const session = inject(SessionStore);
  const router = inject(Router);

  await auth.ensureInitialized();
  return session.isAuthenticated() ? router.parseUrl('/') : true;
};
