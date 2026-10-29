import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { apiErrorInterceptor } from '../../../core/http/api-error.interceptor';
import { ToastService } from '../../../shared/ui/toast.service';
import { Transaction, TransactionPage } from '../data-access/transactions-api.service';
import { TransactionsPage } from './transactions-page';

function tx(id: string, kind: Transaction['kind'], amount: string, description = id): Transaction {
  return {
    id,
    kind,
    description,
    amount,
    currency: 'BRL',
    date: '2026-10-05',
    category: { id: 'c1', name: 'Casa', color: '#112233', icon: 'home' },
    createdAt: '2026-10-05T10:00:00Z',
    updatedAt: '2026-10-05T10:00:00Z',
  };
}

function pageOf(items: Transaction[], totalItems = items.length, p = 1): TransactionPage {
  return {
    items,
    page: p,
    size: 15,
    totalItems,
    totalPages: Math.max(1, Math.ceil(totalItems / 15)),
    totals: { income: '1000.00', expense: '1234.56', balance: '-234.56' },
  };
}

async function setup() {
  localStorage.clear();
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideHttpClient(withInterceptors([apiErrorInterceptor])),
      provideHttpClientTesting(),
    ],
  });
  const http = TestBed.inject(HttpTestingController);
  const fixture = TestBed.createComponent(TransactionsPage);
  await fixture.whenStable();
  return { fixture, http, root: fixture.nativeElement as HTMLElement };
}

const list = (http: HttpTestingController) => http.expectOne((r) => r.url === '/api/v1/transactions' && r.method === 'GET');

async function click(fixture: ComponentFixture<unknown>, element: Element | null | undefined): Promise<void> {
  (element as HTMLElement).click();
  await fixture.whenStable();
}

const button = (root: HTMLElement, text: string) =>
  Array.from(root.querySelectorAll('button')).find((b) => b.textContent?.includes(text));

describe('TransactionsPage', () => {
  it('mostra carregando e depois a lista com valor formatado, sinal, subtotais e intervalo', async () => {
    const { fixture, http, root } = await setup();
    expect(root.textContent).toContain('Carregando lançamentos');

    list(http).flush(pageOf([tx('a', 'EXPENSE', '1234.56', 'Mercado'), tx('b', 'INCOME', '1000.00', 'Salário')], 40));
    await fixture.whenStable();

    const text = root.textContent ?? '';
    expect(text).toContain('Mercado');
    expect(text).toContain('05/10/2026');
    expect(text).toMatch(/-R\$\s1\.234,56/);
    expect(text).toMatch(/\+R\$\s1\.000,00/);
    expect(root.querySelector('dl')?.textContent).toMatch(/Entradas.*R\$\s1\.000,00.*Saídas.*R\$\s1\.234,56.*Saldo.*-R\$\s234,56/s);
    expect(root.querySelector('nav[aria-label="Paginação"]')?.textContent).toContain('1–2 de 40');
    expect(root.querySelector('nav[aria-label="Paginação"]')?.textContent).toContain('1/3');
  });

  it('trocar o filtro consulta de novo com o tipo e volta à primeira página', async () => {
    const { fixture, http, root } = await setup();
    list(http).flush(pageOf([tx('a', 'EXPENSE', '10.00')], 40));
    await fixture.whenStable();

    await click(fixture, button(root, 'Entradas'));
    const req = list(http);

    expect(req.request.params.get('type')).toBe('INCOME');
    expect(req.request.params.get('page')).toBe('1');
    req.flush(pageOf([], 0));
  });

  it('estado vazio; repetir o mesmo filtro não consulta de novo', async () => {
    const { fixture, http, root } = await setup();
    list(http).flush(pageOf([], 0));
    await fixture.whenStable();
    expect(root.textContent).toContain('Nenhum lançamento no período.');

    await click(fixture, button(root, 'Todos'));
    // mesmo filtro: nada a consultar
    http.expectNone('/api/v1/transactions');
  });

  it('erro de carregamento oferece tentar novamente', async () => {
    const { fixture, http, root } = await setup();
    list(http).flush(null, { status: 500, statusText: 'x' });
    await fixture.whenStable();
    expect(root.textContent).toContain('Não foi possível carregar os lançamentos.');

    await click(fixture, button(root, 'Tentar novamente'));
    list(http).flush(pageOf([tx('a', 'EXPENSE', '10.00', 'Café')]));
    await fixture.whenStable();
    expect(root.textContent).toContain('Café');
  });

  it('excluir só depois de confirmar; recarrega a lista e avisa', async () => {
    const { fixture, http, root } = await setup();
    const toast = TestBed.inject(ToastService);
    list(http).flush(pageOf([tx('a', 'EXPENSE', '10.00', 'Café')]));
    await fixture.whenStable();

    await click(fixture, root.querySelector('table [aria-label="Excluir Café"]'));
    http.expectNone({ method: 'DELETE', url: '/api/v1/transactions/a' });
    expect(root.textContent).toContain('Excluir lançamento?');

    await click(fixture, root.querySelector('app-confirm-dialog button.\\!bg-expense'));
    http.expectOne({ method: 'DELETE', url: '/api/v1/transactions/a' }).flush(null, { status: 204, statusText: 'No Content' });
    await fixture.whenStable();
    list(http).flush(pageOf([], 0));
    await fixture.whenStable();

    expect(toast.toasts().map((t) => t.message)).toContain('Lançamento excluído.');
    expect(root.textContent).toContain('Nenhum lançamento no período.');
  });

  it('o cartão mostra descrição, data, categoria e valor em telas estreitas', async () => {
    const { fixture, http, root } = await setup();
    list(http).flush(pageOf([tx('a', 'EXPENSE', '10.00', 'Café')]));
    await fixture.whenStable();

    const card = root.querySelector('ul[aria-label="Lançamentos do período"] li');
    expect(card?.textContent).toContain('Café');
    expect(card?.textContent).toContain('05/10/2026 · Casa');
    expect(card?.textContent).toMatch(/-R\$\s10,00/);
  });
});
