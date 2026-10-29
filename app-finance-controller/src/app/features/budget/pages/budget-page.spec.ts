import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { apiErrorInterceptor } from '../../../core/http/api-error.interceptor';
import { CurrencyStore } from '../../../core/state/currency.store';
import { ReferenceMonthStore } from '../../../core/state/reference-month.store';
import { ToastService } from '../../../shared/ui/toast.service';
import { Category } from '../../categories/data-access/categories-api.service';
import { Budget, BudgetProgress } from '../data-access/budget-api.service';
import { BudgetPage } from './budget-page';

const FOOD = 'food-id';
const CAR = 'car-id';

const CATEGORIES: Category[] = [
  { id: FOOD, name: 'Alimentação', type: 'EXPENSE', icon: 'restaurant', color: '#4CAF50', subcategories: [] },
  { id: CAR, name: 'Carro', type: 'EXPENSE', icon: 'directions_car', color: '#FF9800', subcategories: [] },
];

function budget(over: Partial<Budget> = {}): Budget {
  return { currency: 'BRL', month: '2026-10', categoryLimits: [], ...over };
}

function progress(over: Partial<BudgetProgress> = {}): BudgetProgress {
  return {
    currency: 'BRL',
    month: '2026-10',
    global: { spent: '0.00', status: 'NONE' },
    categories: [
      { categoryId: FOOD, spent: '0.00', status: 'NONE' },
      { categoryId: CAR, spent: '30.25', status: 'NONE' },
    ],
    ...over,
  };
}

async function setup(initialBudget: Budget = budget(), initialProgress: BudgetProgress = progress()) {
  localStorage.clear();
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideHttpClient(withInterceptors([apiErrorInterceptor])),
      provideHttpClientTesting(),
    ],
  });
  const http = TestBed.inject(HttpTestingController);
  const fixture = TestBed.createComponent(BudgetPage);
  await fixture.whenStable();
  http.expectOne('/api/v1/categories').flush(CATEGORIES);
  flushLoad(http, initialBudget, initialProgress);
  await fixture.whenStable();
  return { fixture, http, root: fixture.nativeElement as HTMLElement };
}

function flushLoad(http: HttpTestingController, b: Budget, p: BudgetProgress): { budget: TestRequest; progress: TestRequest } {
  const budgetReq = http.expectOne((r) => /^\/api\/v1\/budgets\/(BRL|EUR)$/.test(r.url) && r.method === 'GET');
  const progressReq = http.expectOne((r) => /^\/api\/v1\/budgets\/(BRL|EUR)\/progress$/.test(r.url));
  budgetReq.flush(b);
  progressReq.flush(p);
  return { budget: budgetReq, progress: progressReq };
}

async function type(fixture: ComponentFixture<unknown>, input: HTMLInputElement | null, value: string): Promise<void> {
  if (!input) throw new Error('campo não encontrado');
  input.value = value;
  input.dispatchEvent(new Event('input'));
  await fixture.whenStable();
}

const field = (root: HTMLElement, name: string) => root.querySelector<HTMLInputElement>(`[formcontrolname="${name}"]`);
const categoryField = (root: HTMLElement, name: string) => root.querySelector<HTMLInputElement>(`input[aria-label="Meta de ${name}"]`);
const submit = (root: HTMLElement) => root.querySelector<HTMLFormElement>('form')?.dispatchEvent(new Event('submit'));

describe('BudgetPage', () => {
  it('mostra carregando e depois os campos preenchidos com o que o servidor tem', async () => {
    const { root } = await setup(
      budget({ monthlyLimit: '1000.00', monthOverride: { limit: '1500.00' }, effectiveLimit: '1500.00', categoryLimits: [{ categoryId: FOOD, limit: '200.50' }] }),
      progress({ global: { limit: '1500.00', spent: '80.25', fraction: '0.0535', remaining: '1419.75', status: 'OK' } }),
    );

    expect(root.textContent).toContain('Meta de Gastos');
    expect(field(root, 'monthlyLimit')?.value).toBe('1000,00');
    expect(field(root, 'overrideLimit')?.value).toBe('1500,00');
    expect(categoryField(root, 'Alimentação')?.value).toBe('200,50');
    expect(categoryField(root, 'Carro')?.value).toBe('');
    expect(root.textContent).toContain('Gasto: R$');
  });

  it.each([
    ['OK', '0.7000', '700.00', 'Dentro da meta', 70],
    ['WARNING', '0.8000', '800.00', 'Perto da meta', 80],
    ['WARNING', '0.9500', '950.00', 'Perto da meta', 95],
    ['EXCEEDED', '1.0000', '1000.00', 'Meta estourada', 100],
    ['EXCEEDED', '1.2000', '1200.00', 'Meta estourada', 120],
  ] as const)('estado %s com gasto %s mostra "%s" com texto, ícone e %d%%', async (status, fraction, spent, label, percent) => {
    const { root } = await setup(
      budget({ monthlyLimit: '1000.00', effectiveLimit: '1000.00' }),
      progress({ global: { limit: '1000.00', spent, fraction, remaining: '0.00', status } }),
    );

    const card = root.querySelector('app-budget-progress-card');
    expect(card?.textContent).toContain(label);
    expect(card?.textContent).toContain(`${percent}%`);
    expect(card?.querySelector('svg, lucide-icon')).not.toBeNull();
    expect(card?.querySelector('[role="progressbar"]')?.getAttribute('aria-valuenow')).toBe(String(Math.min(100, percent)));
  });

  it('sem meta global não mostra o cartão de progresso', async () => {
    const { root } = await setup();

    expect(root.querySelector('app-budget-progress-card')).toBeNull();
  });

  it('a linha da categoria mostra "gasto de limite" colorido pelo estado e o rótulo para leitor de tela', async () => {
    const { root } = await setup(
      budget({ categoryLimits: [{ categoryId: FOOD, limit: '200.00' }] }),
      progress({ categories: [{ categoryId: FOOD, limit: '200.00', spent: '170.00', fraction: '0.8500', remaining: '30.00', status: 'WARNING' }, { categoryId: CAR, spent: '0.00', status: 'NONE' }] }),
    );

    const row = root.querySelector('app-category-limit-row');
    expect(row?.textContent).toMatch(/R\$\s170,00 de R\$\s200,00/);
    expect(row?.textContent).toContain('Perto da meta');
    expect(row?.querySelector('p.text-warning')).not.toBeNull();
  });

  it('grava tudo de uma vez: valores exatos, vazio omite, ajuste do mês e mês exibido', async () => {
    const { fixture, http, root } = await setup();
    const toast = TestBed.inject(ToastService);
    const month = TestBed.inject(ReferenceMonthStore).monthKey();

    await type(fixture, field(root, 'monthlyLimit'), '1000,5');
    await type(fixture, field(root, 'overrideLimit'), '1500');
    await type(fixture, categoryField(root, 'Alimentação'), '200');
    submit(root);
    await fixture.whenStable();

    const req = http.expectOne({ method: 'PUT', url: '/api/v1/budgets/BRL' });
    expect(req.request.body).toEqual({
      month,
      monthlyLimit: '1000.5',
      monthOverride: { limit: '1500' },
      categoryLimits: [{ categoryId: FOOD, limit: '200' }],
    });
    req.flush(budget());
    await fixture.whenStable();
    flushLoad(http, budget({ monthlyLimit: '1000.50' }), progress());
    await fixture.whenStable();

    expect(toast.toasts().map((t) => t.message)).toContain('Meta salva.');
    expect(field(root, 'monthlyLimit')?.value).toBe('1000,50');
  });

  it('campos vazios removem: nada de limite e nenhuma categoria no pedido', async () => {
    const { fixture, http, root } = await setup(
      budget({ monthlyLimit: '1000.00', categoryLimits: [{ categoryId: FOOD, limit: '200.00' }] }),
    );

    await type(fixture, field(root, 'monthlyLimit'), '');
    await type(fixture, categoryField(root, 'Alimentação'), '');
    submit(root);
    await fixture.whenStable();

    const body = http.expectOne({ method: 'PUT', url: '/api/v1/budgets/BRL' }).request.body;
    expect(body.monthlyLimit).toBeUndefined();
    expect(body.monthOverride).toBeUndefined();
    expect(body.categoryLimits).toEqual([]);
  });

  it('"Sem meta neste mês" envia disabled e desativa o campo do ajuste', async () => {
    const { fixture, http, root } = await setup(budget({ monthlyLimit: '1000.00' }));

    const checkbox = field(root, 'overrideDisabled');
    checkbox?.click();
    await fixture.whenStable();
    expect(field(root, 'overrideLimit')?.disabled).toBe(true);

    submit(root);
    await fixture.whenStable();
    const body = http.expectOne({ method: 'PUT', url: '/api/v1/budgets/BRL' }).request.body;
    expect(body.monthOverride).toEqual({ disabled: true });
    expect(body.monthlyLimit).toBe('1000.00');
  });

  it('mês com "sem meta" vindo do servidor marca a caixa e mostra o campo desativado', async () => {
    const { root } = await setup(budget({ monthlyLimit: '1000.00', monthOverride: { disabled: true } }));

    expect(field(root, 'overrideDisabled')?.checked).toBe(true);
    expect(field(root, 'overrideLimit')?.disabled).toBe(true);
  });

  it('falha ao gravar mantém o que foi digitado e mostra o erro; nada recarrega', async () => {
    const { fixture, http, root } = await setup();
    const toast = TestBed.inject(ToastService);

    await type(fixture, field(root, 'monthlyLimit'), '900');
    await type(fixture, categoryField(root, 'Carro'), '50');
    submit(root);
    await fixture.whenStable();
    http
      .expectOne({ method: 'PUT', url: '/api/v1/budgets/BRL' })
      .flush({ code: 'CATEGORY_TYPE_MISMATCH', title: 'A categoria não aceita este tipo de lançamento.', status: 409 }, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();

    expect(toast.toasts().map((t) => t.message)).toContain('A categoria não aceita este tipo de lançamento.');
    expect(field(root, 'monthlyLimit')?.value).toBe('900');
    expect(categoryField(root, 'Carro')?.value).toBe('50');
    http.expectNone((r) => r.method === 'GET' && r.url.startsWith('/api/v1/budgets'));
  });

  it('trocar o mês recarrega os campos daquele mês; trocar a moeda, a configuração da outra moeda', async () => {
    const { fixture, http, root } = await setup(budget({ monthlyLimit: '1000.00' }));
    const month = TestBed.inject(ReferenceMonthStore);
    await type(fixture, field(root, 'monthlyLimit'), '1');

    month.next();
    fixture.detectChanges();
    await fixture.whenStable();
    const next = flushLoad(http, budget({ monthlyLimit: '1000.00', monthOverride: { limit: '1500.00' } }), progress());
    expect(next.budget.request.params.get('month')).toBe(month.monthKey());
    await fixture.whenStable();
    expect(field(root, 'overrideLimit')?.value).toBe('1500,00');
    expect(field(root, 'monthlyLimit')?.value).toBe('1000,00');

    TestBed.inject(CurrencyStore).select('EUR');
    fixture.detectChanges();
    await fixture.whenStable();
    const eur = flushLoad(http, budget({ currency: 'EUR', monthlyLimit: '50.00' }), progress({ currency: 'EUR' }));
    expect(eur.budget.request.url).toBe('/api/v1/budgets/EUR');
    await fixture.whenStable();
    expect(field(root, 'monthlyLimit')?.value).toBe('50,00');
    expect(field(root, 'overrideLimit')?.value).toBe('');
  });

  it('erro de carregamento oferece tentar novamente', async () => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(withInterceptors([apiErrorInterceptor])), provideHttpClientTesting()],
    });
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(BudgetPage);
    await fixture.whenStable();
    http.expectOne('/api/v1/categories').flush(CATEGORIES);
    http.expectOne((r) => r.url === '/api/v1/budgets/BRL').flush(null, { status: 500, statusText: 'x' });
    http.match((r) => r.url === '/api/v1/budgets/BRL/progress');
    await fixture.whenStable();
    const root = fixture.nativeElement as HTMLElement;

    expect(root.textContent).toContain('Não foi possível carregar a meta.');
    Array.from(root.querySelectorAll('button')).find((b) => b.textContent?.includes('Tentar novamente'))?.click();
    await fixture.whenStable();
    flushLoad(http, budget(), progress());
    await fixture.whenStable();
    expect(root.querySelector('form')).not.toBeNull();
  });
});
