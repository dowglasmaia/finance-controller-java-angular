import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  CanActivateFn,
  GuardResult,
  MaybeAsync,
  Router,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { SessionStore } from '../../../core/state/session.store';
import { authGuard, guestGuard, safeReturnUrl } from './auth.guard';

const USER = { id: '11111111-1111-1111-1111-111111111111', email: 'a@b.com' };

async function run(guard: CanActivateFn, url: string): Promise<GuardResult> {
  const result: MaybeAsync<GuardResult> = TestBed.runInInjectionContext(() =>
    guard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot),
  );
  return (await Promise.resolve(result)) as GuardResult;
}

describe('guards', () => {
  let session: SessionStore;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    session = TestBed.inject(SessionStore);
    router = TestBed.inject(Router);
    session.markInitialized();
  });

  it('authGuard sem sessão redireciona para /entrar com returnUrl só com o caminho', async () => {
    const result = await run(authGuard, '/lancamentos?mes=2026-06#x');
    expect(result).toBeInstanceOf(UrlTree);
    expect(router.serializeUrl(result as UrlTree)).toBe('/entrar?returnUrl=%2Flancamentos');
  });

  it('authGuard sem sessão na raiz não leva returnUrl', async () => {
    const result = await run(authGuard, '/');
    expect(router.serializeUrl(result as UrlTree)).toBe('/entrar');
  });

  it('authGuard com sessão permite', async () => {
    session.set({ user: USER, accessToken: 't' });
    expect(await run(authGuard, '/lancamentos')).toBe(true);
  });

  it('guestGuard permite sem sessão e redireciona para / com sessão', async () => {
    expect(await run(guestGuard, '/entrar')).toBe(true);
    session.set({ user: USER, accessToken: 't' });
    const result = await run(guestGuard, '/entrar');
    expect(router.serializeUrl(result as UrlTree)).toBe('/');
  });
});

describe('safeReturnUrl', () => {
  it('aceita caminhos internos', () => {
    expect(safeReturnUrl('/lancamentos')).toBe('/lancamentos');
    expect(safeReturnUrl('/a/b?x=1')).toBe('/a/b?x=1');
  });

  it('rejeita open redirect e valores inválidos', () => {
    for (const raw of ['//evil.com', '/\\evil.com', 'https://evil.com', 'evil', '', null, undefined, 42, '/a\nb']) {
      expect(safeReturnUrl(raw)).toBe('/');
    }
    expect(safeReturnUrl('//x', '/inicio')).toBe('/inicio');
  });
});
