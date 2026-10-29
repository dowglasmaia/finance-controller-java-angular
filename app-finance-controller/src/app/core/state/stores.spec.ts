import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { AppStateReset } from './app-state-reset';
import { CurrencyStore } from './currency.store';
import { ReferenceMonthStore } from './reference-month.store';
import { SessionStore } from './session.store';
import { ThemeStore } from './theme.store';

function setup(): void {
  TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] });
}

describe('ReferenceMonthStore', () => {
  beforeEach(setup);

  it('começa no 1.º dia do mês atual e navega entre meses', () => {
    const store = TestBed.inject(ReferenceMonthStore);
    const now = new Date();
    expect(store.referenceMonth().getDate()).toBe(1);
    expect(store.referenceMonth().getMonth()).toBe(now.getMonth());

    store.previous();
    expect(store.referenceMonth()).toEqual(new Date(now.getFullYear(), now.getMonth() - 1, 1));
    store.next();
    store.next();
    expect(store.referenceMonth()).toEqual(new Date(now.getFullYear(), now.getMonth() + 1, 1));
    store.reset();
    expect(store.referenceMonth()).toEqual(new Date(now.getFullYear(), now.getMonth(), 1));
  });

  it('formata o rótulo capitalizado em pt-BR', () => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date(2026, 5, 15));
    try {
      const store = TestBed.inject(ReferenceMonthStore);
      expect(store.label()).toBe('Junho 2026');
      expect(store.monthKey()).toBe('2026-06');
      store.next();
      expect(store.monthKey()).toBe('2026-07');
    } finally {
      vi.useRealTimers();
    }
  });
});

describe('CurrencyStore', () => {
  beforeEach(() => {
    localStorage.clear();
    setup();
  });
  afterEach(() => vi.restoreAllMocks());

  it('padrão BRL e persiste a seleção', () => {
    const store = TestBed.inject(CurrencyStore);
    expect(store.selectedCurrency()).toBe('BRL');
    store.select('EUR');
    expect(localStorage.getItem('fc.currency')).toBe('EUR');
  });

  it('restaura a moeda guardada', () => {
    localStorage.setItem('fc.currency', 'EUR');
    expect(TestBed.inject(CurrencyStore).selectedCurrency()).toBe('EUR');
  });

  it('ignora valor inválido guardado', () => {
    localStorage.setItem('fc.currency', 'USD');
    expect(TestBed.inject(CurrencyStore).selectedCurrency()).toBe('BRL');
  });

  it('funciona com localStorage indisponível', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('bloqueado');
    });
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('bloqueado');
    });
    const store = TestBed.inject(CurrencyStore);
    expect(store.selectedCurrency()).toBe('BRL');
    expect(() => store.select('EUR')).not.toThrow();
    expect(store.selectedCurrency()).toBe('EUR');
  });
});

describe('ThemeStore', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.classList.remove('dark');
    setup();
  });

  it('padrão é o tema escuro, aplicado a <html>, sem depender do sistema', () => {
    const store = TestBed.inject(ThemeStore);
    TestBed.tick();
    expect(store.themeMode()).toBe('DARK');
    expect(store.effectiveDark()).toBe(true);
    expect(document.documentElement.classList.contains('dark')).toBe(true);
  });

  it('respeita a escolha guardada (claro)', () => {
    localStorage.setItem('fc.theme', 'LIGHT');
    TestBed.resetTestingModule();
    setup();
    const store = TestBed.inject(ThemeStore);
    TestBed.tick();
    expect(store.themeMode()).toBe('LIGHT');
    expect(document.documentElement.classList.contains('dark')).toBe(false);
  });

  it('toggle alterna dark→light→dark, aplica a classe e persiste', () => {
    const store = TestBed.inject(ThemeStore);
    store.toggle();
    TestBed.tick();
    expect(store.themeMode()).toBe('LIGHT');
    expect(document.documentElement.classList.contains('dark')).toBe(false);
    expect(localStorage.getItem('fc.theme')).toBe('LIGHT');

    store.toggle();
    TestBed.tick();
    expect(store.themeMode()).toBe('DARK');
    expect(document.documentElement.classList.contains('dark')).toBe(true);
  });
});

describe('AppStateReset', () => {
  beforeEach(setup);

  it('limpa sessão e mês e executa callbacks registrados', () => {
    const session = TestBed.inject(SessionStore);
    const month = TestBed.inject(ReferenceMonthStore);
    const reset = TestBed.inject(AppStateReset);
    const callback = vi.fn();
    const unregister = reset.register(callback);

    session.set({ user: { id: 'x', email: 'a@b.com' }, accessToken: 't' });
    month.previous();
    reset.resetAll();

    expect(session.isAuthenticated()).toBe(false);
    expect(month.referenceMonth().getMonth()).toBe(new Date().getMonth());
    expect(callback).toHaveBeenCalledTimes(1);

    unregister();
    reset.resetAll();
    expect(callback).toHaveBeenCalledTimes(1);
  });
});
