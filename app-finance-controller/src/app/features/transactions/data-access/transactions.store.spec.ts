import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { CurrencyStore } from '../../../core/state/currency.store';
import { ReferenceMonthStore } from '../../../core/state/reference-month.store';
import { Transaction, TransactionPage } from './transactions-api.service';
import { TransactionsStore } from './transactions.store';

function item(id: string): Transaction {
  return {
    id,
    kind: 'EXPENSE',
    description: id,
    amount: '10.00',
    currency: 'BRL',
    date: '2026-10-05',
    category: { id: 'c', name: 'Casa', color: '#112233', icon: 'home' },
    createdAt: '2026-10-05T10:00:00Z',
    updatedAt: '2026-10-05T10:00:00Z',
  };
}

function page(items: Transaction[], p = 1, totalItems = items.length): TransactionPage {
  return {
    items,
    page: p,
    size: 15,
    totalItems,
    totalPages: Math.max(1, Math.ceil(totalItems / 15)),
    totals: { income: '0.00', expense: '10.00', balance: '-10.00' },
  };
}

function setup() {
  TestBed.configureTestingModule({
    providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting(), TransactionsStore],
  });
  localStorage.clear();
  return {
    store: TestBed.inject(TransactionsStore),
    http: TestBed.inject(HttpTestingController),
    month: TestBed.inject(ReferenceMonthStore),
    currency: TestBed.inject(CurrencyStore),
  };
}

const lastList = (http: HttpTestingController) =>
  http.expectOne((r) => r.url === '/api/v1/transactions' && r.method === 'GET');

describe('TransactionsStore', () => {
  it('carrega a lista do mês e moeda atuais com a primeira página', () => {
    const { store, http, month, currency } = setup();
    TestBed.tick();

    const req = lastList(http);
    expect(req.request.params.get('month')).toBe(month.monthKey());
    expect(req.request.params.get('currency')).toBe(currency.selectedCurrency());
    expect(req.request.params.get('type')).toBe('ALL');
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('size')).toBe('15');
    expect(store.status()).toBe('loading');

    req.flush(page([item('a')]));
    expect(store.status()).toBe('loaded');
    expect(store.items().map((i) => i.id)).toEqual(['a']);
    expect(store.range()).toEqual({ from: 1, to: 1, total: 1 });
  });

  it('mudar o filtro, o mês ou a moeda volta à primeira página e cancela a consulta anterior', () => {
    const { store, http, month, currency } = setup();
    TestBed.tick();
    lastList(http).flush(page([item('a')], 1, 40));
    store.goTo(3);
    TestBed.tick();
    expect(lastList(http).request.params.get('page')).toBe('3');

    store.setKind('INCOME');
    TestBed.tick();
    const filtered = lastList(http);
    expect(filtered.request.params.get('type')).toBe('INCOME');
    expect(filtered.request.params.get('page')).toBe('1');
    filtered.flush(page([], 1, 0));

    month.next();
    TestBed.tick();
    const nextMonth = lastList(http);
    expect(nextMonth.request.params.get('month')).toBe(month.monthKey());
    nextMonth.flush(page([]));

    currency.select('EUR');
    TestBed.tick();
    const eur = lastList(http);
    expect(eur.request.params.get('currency')).toBe('EUR');
    eur.flush(page([]));
  });

  it('erro mostra o estado de erro e refresh tenta de novo', () => {
    const { store, http } = setup();
    TestBed.tick();
    lastList(http).flush(null, { status: 500, statusText: 'x' });
    expect(store.status()).toBe('error');

    store.refresh();
    TestBed.tick();
    lastList(http).flush(page([item('a')]));
    expect(store.status()).toBe('loaded');
  });

  it('se a página atual deixou de existir, vai para a última', () => {
    const { store, http } = setup();
    TestBed.tick();
    lastList(http).flush(page([item('a')], 1, 16));
    store.goTo(2);
    TestBed.tick();
    lastList(http).flush(page([item('b')], 2, 16));

    // exclui o único item da página 2: o servidor devolve a página 2 vazia e totalPages 1
    store.refresh();
    TestBed.tick();
    lastList(http).flush({ ...page([], 2, 15), totalPages: 1 });
    TestBed.tick();

    const retry = lastList(http);
    expect(retry.request.params.get('page')).toBe('1');
    retry.flush(page([item('a')], 1, 15));
    expect(store.page()).toBe(1);
  });

  it('calcula o intervalo exibido (1–15 de 40, 31–40 de 40)', () => {
    const { store, http } = setup();
    TestBed.tick();
    lastList(http).flush(page(Array.from({ length: 15 }, (_, i) => item(String(i))), 1, 40));
    expect(store.range()).toEqual({ from: 1, to: 15, total: 40 });

    store.goTo(3);
    TestBed.tick();
    lastList(http).flush(page(Array.from({ length: 10 }, (_, i) => item(String(i))), 3, 40));
    expect(store.range()).toEqual({ from: 31, to: 40, total: 40 });
  });
});
