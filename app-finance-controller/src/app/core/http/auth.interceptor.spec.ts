import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { SessionStore } from '../state/session.store';
import { isApiError } from './api-error';
import { apiErrorInterceptor } from './api-error.interceptor';
import { authInterceptor } from './auth.interceptor';

const USER = { id: '11111111-1111-1111-1111-111111111111', email: 'a@b.com' };
const UNAUTHORIZED = { status: 401, statusText: 'Unauthorized' };
const PROBLEM = { code: 'UNAUTHENTICATED', title: 'Não autenticado', status: 401 };

describe('authInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;
  let session: SessionStore;
  let navigate: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor, apiErrorInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
    session = TestBed.inject(SessionStore);
    navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    session.set({ user: USER, accessToken: 'old' });
  });

  afterEach(() => controller.verify());

  it('anexa o bearer nas chamadas da API', () => {
    http.get('/api/v1/accounts').subscribe();
    const request = controller.expectOne('/api/v1/accounts');
    expect(request.request.headers.get('Authorization')).toBe('Bearer old');
    request.flush([]);
  });

  it('não anexa bearer a endpoints de auth nem a URLs externas', () => {
    http.post('/api/v1/auth/login', {}).subscribe();
    const login = controller.expectOne('/api/v1/auth/login');
    expect(login.request.headers.has('Authorization')).toBe(false);
    login.flush({});

    http.get('https://outro.exemplo/x').subscribe();
    const external = controller.expectOne('https://outro.exemplo/x');
    expect(external.request.headers.has('Authorization')).toBe(false);
    external.flush({});
  });

  it('em 401 renova uma vez e repete a chamada com o novo token', () => {
    let result: unknown;
    http.get('/api/v1/accounts').subscribe((value) => (result = value));
    controller.expectOne('/api/v1/accounts').flush(PROBLEM, UNAUTHORIZED);

    controller.expectOne('/api/v1/auth/refresh').flush({ accessToken: 'new' });
    const retry = controller.expectOne('/api/v1/accounts');
    expect(retry.request.headers.get('Authorization')).toBe('Bearer new');
    retry.flush(['ok']);

    expect(result).toEqual(['ok']);
    expect(session.accessToken()).toBe('new');
  });

  it('chamadas concorrentes com 401 compartilham uma única renovação', () => {
    const results: unknown[] = [];
    http.get('/api/v1/a').subscribe((value) => results.push(value));
    http.get('/api/v1/b').subscribe((value) => results.push(value));
    controller.expectOne('/api/v1/a').flush(PROBLEM, UNAUTHORIZED);
    controller.expectOne('/api/v1/b').flush(PROBLEM, UNAUTHORIZED);

    controller.expectOne('/api/v1/auth/refresh').flush({ accessToken: 'new' });
    controller.expectOne('/api/v1/a').flush('A');
    controller.expectOne('/api/v1/b').flush('B');

    expect(results).toEqual(['A', 'B']);
  });

  it('se a renovação falha limpa a sessão, navega para /entrar e propaga o erro', () => {
    let error: unknown;
    http.get('/api/v1/accounts').subscribe({ error: (e: unknown) => (error = e) });
    controller.expectOne('/api/v1/accounts').flush(PROBLEM, UNAUTHORIZED);
    controller.expectOne('/api/v1/auth/refresh').flush(PROBLEM, UNAUTHORIZED);

    expect(session.isAuthenticated()).toBe(false);
    expect(session.accessToken()).toBeNull();
    expect(navigate).toHaveBeenCalledWith('/entrar');
    expect(isApiError(error)).toBe(true);
  });

  it('não renova em erros diferentes de 401', () => {
    let error: unknown;
    http.get('/api/v1/accounts').subscribe({ error: (e: unknown) => (error = e) });
    controller.expectOne('/api/v1/accounts').flush({ code: 'X', title: 't' }, { status: 500, statusText: 'x' });
    expect(isApiError(error)).toBe(true);
    expect(navigate).not.toHaveBeenCalled();
  });
});
