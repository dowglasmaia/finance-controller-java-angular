import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { SessionStore } from '../../../core/state/session.store';
import { AuthService } from './auth.service';

const USER = { id: '11111111-1111-1111-1111-111111111111', email: 'a@b.com' };

describe('AuthService', () => {
  let service: AuthService;
  let controller: HttpTestingController;
  let session: SessionStore;
  let navigate: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    controller = TestBed.inject(HttpTestingController);
    session = TestBed.inject(SessionStore);
    navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
  });

  afterEach(() => controller.verify());

  const nextTick = (): Promise<void> => new Promise((resolve) => setTimeout(resolve));

  it('login grava a sessão apenas em memória', () => {
    service.login('a@b.com', 'segredo123').subscribe();
    const request = controller.expectOne('/api/v1/auth/login');
    expect(request.request.withCredentials).toBe(true);
    request.flush({ user: USER, accessToken: 'tok-secreto' });

    expect(session.isAuthenticated()).toBe(true);
    expect(JSON.stringify({ ...localStorage })).not.toContain('tok-secreto');
    expect(JSON.stringify({ ...sessionStorage })).not.toContain('tok-secreto');
  });

  it('restoreSession com sucesso faz refresh e depois /auth/me', async () => {
    const promise = service.restoreSession();
    controller.expectOne('/api/v1/auth/refresh').flush({ accessToken: 'tok' });
    await nextTick();
    const me = controller.expectOne('/api/v1/auth/me');
    expect(me.request.withCredentials).toBe(false);
    me.flush(USER);

    expect(await promise).toBe(true);
    expect(session.user()).toEqual(USER);
    expect(session.initialized()).toBe(true);
  });

  it('restoreSession com falha resolve false, limpa e marca initialized', async () => {
    const promise = service.restoreSession();
    controller.expectOne('/api/v1/auth/refresh').flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(await promise).toBe(false);
    expect(session.isAuthenticated()).toBe(false);
    expect(session.initialized()).toBe(true);
  });

  it('refresh concorrente é compartilhado', () => {
    const tokens: string[] = [];
    service.refresh().subscribe((t) => tokens.push(t));
    service.refresh().subscribe((t) => tokens.push(t));
    controller.expectOne('/api/v1/auth/refresh').flush({ accessToken: 'novo' });
    expect(tokens).toEqual(['novo', 'novo']);
  });

  it('logout limpa a sessão e navega mesmo se a chamada falhar', async () => {
    session.set({ user: USER, accessToken: 'tok' });
    const promise = service.logout();
    controller.expectOne('/api/v1/auth/logout').flush({}, { status: 500, statusText: 'x' });
    await promise;

    expect(session.isAuthenticated()).toBe(false);
    expect(navigate).toHaveBeenCalledWith('/entrar');
  });
});
