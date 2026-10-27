import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../../features/auth/data-access/auth.service';
import { AppStateReset } from '../state/app-state-reset';
import { SessionStore } from '../state/session.store';
import { API_BASE_URL } from './api-base-url.token';
import { isApiError } from './api-error';

const PUBLIC_AUTH_PATHS: readonly string[] = ['/auth/login', '/auth/register', '/auth/refresh', '/auth/logout'];
const GUEST_ROUTES: readonly string[] = ['/entrar', '/cadastrar'];

function pathAfterBase(url: string, baseUrl: string): string | null {
  if (!url.startsWith(baseUrl)) {
    return null;
  }
  return url.slice(baseUrl.length).split(/[?#]/)[0] ?? '';
}

function statusOf(error: unknown): number | null {
  if (error instanceof HttpErrorResponse) {
    return error.status;
  }
  return isApiError(error) ? error.status : null;
}

function withBearer(request: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return request.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}

/**
 * Anexa o bearer e, em 401 de chamada protegida, faz UMA renovação (compartilhada) e repete a chamada.
 * Aceita o erro como `HttpErrorResponse` ou já como `ApiError` (ordem recomendada: [auth, apiError]).
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const path = pathAfterBase(request.url, inject(API_BASE_URL));
  if (path === null || PUBLIC_AUTH_PATHS.includes(path)) {
    return next(request);
  }

  const session = inject(SessionStore);
  const auth = inject(AuthService);
  const reset = inject(AppStateReset);
  const router = inject(Router);

  const sentToken = session.accessToken();
  const outgoing = sentToken ? withBearer(request, sentToken) : request;

  return next(outgoing).pipe(
    catchError((error: unknown) => {
      if (statusOf(error) !== 401) {
        return throwError(() => error);
      }
      // Outra chamada já renovou o token enquanto esta estava em voo: só repete.
      const current = session.accessToken();
      if (current && current !== sentToken) {
        return next(withBearer(request, current));
      }
      return auth.refresh().pipe(
        catchError(() => {
          reset.resetAll();
          if (!GUEST_ROUTES.some((route) => router.url.startsWith(route))) {
            void router.navigateByUrl('/entrar');
          }
          return throwError(() => error);
        }),
        switchMap((token) => next(withBearer(request, token))),
      );
    }),
  );
};
