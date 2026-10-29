import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ToastService } from '../../shared/ui/toast.service';
import { AppStateReset } from './app-state-reset';
import { CurrencyStore } from './currency.store';
import { PreferencesSync } from './preferences-sync';
import { SessionStore } from './session.store';
import { ThemeStore } from './theme.store';

const USER = { id: '11111111-1111-1111-1111-111111111111', email: 'a@b.com' };

function setup() {
  localStorage.clear();
  TestBed.configureTestingModule({
    providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting()],
  });
  const sync = TestBed.inject(PreferencesSync);
  return {
    sync,
    http: TestBed.inject(HttpTestingController),
    session: TestBed.inject(SessionStore),
    currency: TestBed.inject(CurrencyStore),
    theme: TestBed.inject(ThemeStore),
    toast: TestBed.inject(ToastService),
  };
}

describe('PreferencesSync', () => {
  afterEach(() => TestBed.inject(HttpTestingController).verify());

  it('depois de entrar as preferências do servidor prevalecem e não são regravadas', () => {
    const { sync, http, session, currency, theme } = setup();
    currency.select('BRL');
    theme.setMode('DARK');
    session.set({ user: USER, accessToken: 't' });

    sync.load().subscribe();
    http.expectOne({ method: 'GET', url: '/api/v1/preferences' }).flush({ currency: 'EUR', theme: 'LIGHT' });
    TestBed.tick();

    expect(currency.selectedCurrency()).toBe('EUR');
    expect(theme.themeMode()).toBe('LIGHT');
    http.expectNone({ method: 'PUT', url: '/api/v1/preferences' });
  });

  it('trocar a moeda ou o tema grava as duas preferências no servidor', () => {
    const { sync, http, session, currency, theme } = setup();
    session.set({ user: USER, accessToken: 't' });
    sync.load().subscribe();
    http.expectOne('/api/v1/preferences').flush({ currency: 'BRL', theme: 'DARK' });
    TestBed.tick();

    currency.select('EUR');
    TestBed.tick();
    const first = http.expectOne({ method: 'PUT', url: '/api/v1/preferences' });
    expect(first.request.body).toEqual({ currency: 'EUR', theme: 'DARK' });
    first.flush({ currency: 'EUR', theme: 'DARK' });

    theme.toggle();
    TestBed.tick();
    const second = http.expectOne({ method: 'PUT', url: '/api/v1/preferences' });
    expect(second.request.body).toEqual({ currency: 'EUR', theme: 'LIGHT' });
    second.flush({ currency: 'EUR', theme: 'LIGHT' });
  });

  it('sem sessão não há chamada nenhuma', () => {
    const { currency, theme } = setup();

    currency.select('EUR');
    theme.setMode('LIGHT');
    TestBed.tick();

    TestBed.inject(HttpTestingController).expectNone('/api/v1/preferences');
  });

  it('falha ao gravar avisa, não lança e a escolha continua no dispositivo', () => {
    const { sync, http, session, currency, toast } = setup();
    session.set({ user: USER, accessToken: 't' });
    sync.load().subscribe();
    http.expectOne('/api/v1/preferences').flush({ currency: 'BRL', theme: 'DARK' });
    TestBed.tick();

    currency.select('EUR');
    TestBed.tick();
    http.expectOne({ method: 'PUT', url: '/api/v1/preferences' }).flush({}, { status: 500, statusText: 'x' });

    expect(toast.toasts().map((t) => t.message)).toContain('Não foi possível salvar suas preferências. Elas continuam neste dispositivo.');
    expect(currency.selectedCurrency()).toBe('EUR');
    expect(localStorage.getItem('fc.currency')).toBe('EUR');
  });

  it('falha ao ler mantém as preferências do dispositivo e as mudanças seguintes são gravadas', () => {
    const { sync, http, session, currency } = setup();
    currency.select('EUR');
    session.set({ user: USER, accessToken: 't' });

    sync.load().subscribe();
    http.expectOne('/api/v1/preferences').flush({}, { status: 500, statusText: 'x' });
    TestBed.tick();
    expect(currency.selectedCurrency()).toBe('EUR');
    http.expectNone({ method: 'PUT', url: '/api/v1/preferences' });

    currency.select('BRL');
    TestBed.tick();
    expect(http.expectOne({ method: 'PUT', url: '/api/v1/preferences' }).request.body.currency).toBe('BRL');
  });

  it('ao sair moeda e tema voltam ao padrão, saem do dispositivo e nada é gravado no servidor', () => {
    const { sync, http, session, currency, theme } = setup();
    session.set({ user: USER, accessToken: 't' });
    sync.load().subscribe();
    http.expectOne('/api/v1/preferences').flush({ currency: 'EUR', theme: 'LIGHT' });
    TestBed.tick();
    expect(localStorage.getItem('fc.currency')).toBe('EUR');

    TestBed.inject(AppStateReset).resetAll();
    TestBed.tick();

    expect(currency.selectedCurrency()).toBe('BRL');
    expect(theme.themeMode()).toBe('DARK');
    expect(localStorage.getItem('fc.currency')).toBeNull();
    expect(localStorage.getItem('fc.theme')).toBeNull();
    http.expectNone({ method: 'PUT', url: '/api/v1/preferences' });
  });
});
