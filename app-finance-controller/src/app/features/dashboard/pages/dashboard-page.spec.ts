import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { apiErrorInterceptor } from '../../../core/http/api-error.interceptor';
import { CurrencyStore } from '../../../core/state/currency.store';
import { ReferenceMonthStore } from '../../../core/state/reference-month.store';
import { MonthlyOverview, OverviewTransaction } from '../data-access/overview-api.service';
import { DashboardPage } from './dashboard-page';

const FOOD = { id: 'food', name: 'Alimentação', color: '#4CAF50', icon: 'restaurant' };
const CAR = { id: 'car', name: 'Carro', color: '#FF9800', icon: 'directions_car' };

function overview(over: Partial<MonthlyOverview> = {}): MonthlyOverview {
  return {
    month: '2026-10',
    currency: 'BRL',
    summary: { income: '1000.00', expense: '250.00', balance: '750.00' },
    previous: { income: '0.00', expense: '0.00', balance: '0.00' },
    reservesTotal: '0.00',
    bars: [
      { label: 'S1', income: '1000.00', expense: '100.00' },
      { label: 'S2', income: '0.00', expense: '150.00' },
      { label: 'S3', income: '0.00', expense: '0.00' },
      { label: 'S4', income: '0.00', expense: '0.00' },
      { label: 'S5', income: '0.00', expense: '0.00' },
    ],
    expenseByCategory: [
      { category: FOOD, amount: '150.00', percentage: '60.0000' },
      { category: CAR, amount: '100.00', percentage: '40.0000' },
    ],
    latestTransactions: [],
    budgetProgress: { spent: '250.00', status: 'NONE' },
    ...over,
  };
}

function txn(id: string, kind: 'INCOME' | 'EXPENSE', amount: string, withSub = false): OverviewTransaction {
  return {
    id,
    kind,
    description: `Item ${id}`,
    amount,
    currency: 'BRL',
    date: '2026-10-05',
    category: FOOD,
    ...(withSub ? { subcategory: { id: 's', name: 'Padaria' } } : {}),
  };
}

async function setup(initial: MonthlyOverview = overview()) {
  localStorage.clear();
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideRouter([]),
      provideHttpClient(withInterceptors([apiErrorInterceptor])),
      provideHttpClientTesting(),
    ],
  });
  const http = TestBed.inject(HttpTestingController);
  const fixture = TestBed.createComponent(DashboardPage);
  await fixture.whenStable();
  const root = fixture.nativeElement as HTMLElement;
  const first = http.expectOne((r) => r.url === '/api/v1/overview');
  first.flush(initial);
  await fixture.whenStable();
  return { fixture, http, root, first };
}

const overviewRequest = (http: HttpTestingController): TestRequest => http.expectOne((r) => r.url === '/api/v1/overview');
const text = (root: HTMLElement) => (root.textContent ?? '').replace(/\s+/g, ' ');

describe('DashboardPage', () => {
  it('mostra esqueletos enquanto carrega, a saudação e o subtítulo do mês', async () => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    const fixture = TestBed.createComponent(DashboardPage);
    await fixture.whenStable();
    const root = fixture.nativeElement as HTMLElement;

    expect(root.querySelector('[aria-busy="true"]')).not.toBeNull();
    expect(root.querySelectorAll('.animate-pulse').length).toBeGreaterThan(0);
    expect(text(root)).toMatch(/Bom dia|Boa tarde|Boa noite/);
    expect(text(root)).toContain(`Resumo de ${TestBed.inject(ReferenceMonthStore).label()}`);
  });

  it('BRL +1000/−250: Saldo R$ 750,00 em verde e uma única chamada por mês e moeda', async () => {
    const { root, first } = await setup();

    expect(first.request.params.get('month')).toBe(TestBed.inject(ReferenceMonthStore).monthKey());
    expect(first.request.params.get('currency')).toBe('BRL');
    const balance = Array.from(root.querySelectorAll('app-kpi-card')).find((k) => k.textContent?.includes('Saldo'));
    expect(balance?.textContent).toMatch(/R\$\s750,00/);
    expect(balance?.querySelector('p.text-income')).not.toBeNull();
    expect(text(root)).toMatch(/Entradas.*R\$\s1\.000,00/);
    expect(text(root)).toMatch(/Saídas.*R\$\s250,00/);
  });

  it('saldo negativo fica coral', async () => {
    const { root } = await setup(overview({ summary: { income: '0.00', expense: '100.00', balance: '-100.00' } }));

    const balance = Array.from(root.querySelectorAll('app-kpi-card')).find((k) => k.textContent?.includes('Saldo'));
    expect(balance?.querySelector('p.text-expense')).not.toBeNull();
    expect(balance?.textContent).toMatch(/-R\$\s100,00/);
  });

  it('sem base não mostra variação; com base mostra sinal, ícone e texto, e saídas menores são boa notícia', async () => {
    const withoutBase = await setup();
    expect(withoutBase.root.textContent).not.toContain('sobre o mês anterior');
    TestBed.resetTestingModule();

    const { root } = await setup(overview({ incomeDeltaPercent: '25.0', expenseDeltaPercent: '-10.5' }));
    const income = Array.from(root.querySelectorAll('app-kpi-card')).find((k) => k.textContent?.includes('Entradas'));
    const expense = Array.from(root.querySelectorAll('app-kpi-card')).find((k) => k.textContent?.includes('Saídas'));
    expect(income?.textContent).toContain('+25,0%');
    expect(income?.textContent).toContain('aumento de 25,0% sobre o mês anterior');
    expect(expense?.textContent).toContain('-10,5%');
    expect(expense?.querySelector('p.text-income')).not.toBeNull();
  });

  it('variação "0" mostra 0%', async () => {
    const { root } = await setup(overview({ incomeDeltaPercent: '0' }));

    const income = Array.from(root.querySelectorAll('app-kpi-card')).find((k) => k.textContent?.includes('Entradas'));
    expect(income?.textContent).toContain('0%');
  });

  it('meta 1000 com gasto 850: Perto da meta, 85% e 150 restantes; sem meta mostra o convite com link', async () => {
    const withGoal = await setup(
      overview({ budgetProgress: { limit: '1000.00', spent: '850.00', fraction: '0.8500', remaining: '150.00', status: 'WARNING' } }),
    );
    const card = withGoal.root.querySelector('app-budget-progress-card');
    expect(card?.textContent).toContain('Perto da meta');
    expect(card?.textContent).toContain('85%');
    expect(card?.textContent).toMatch(/Restam R\$\s150,00/);
    TestBed.resetTestingModule();

    const { root } = await setup();
    expect(root.querySelector('app-budget-progress-card')).toBeNull();
    expect(text(root)).toContain('Sem meta definida para');
    expect(root.querySelector('a[href="/metas"]')?.textContent).toContain('Definir meta');
  });

  it('meta estourada mostra quanto excedeu', async () => {
    const { root } = await setup(
      overview({ budgetProgress: { limit: '1000.00', spent: '1200.00', fraction: '1.2000', remaining: '-200.00', status: 'EXCEEDED' } }),
    );

    const card = root.querySelector('app-budget-progress-card');
    expect(card?.textContent).toContain('Meta estourada');
    expect(card?.textContent).toMatch(/Excedeu R\$\s200,00/);
  });

  it('as barras têm alternativa textual e dica com o valor formatado', async () => {
    const { root } = await setup();

    const svg = root.querySelector('app-income-expense-bars svg');
    expect(svg?.getAttribute('role')).toBe('img');
    expect(svg?.getAttribute('aria-label')).toContain('Entradas e saídas');
    expect(root.querySelectorAll('app-income-expense-bars rect').length).toBe(10);
    expect(text(root.querySelector('app-income-expense-bars table') as HTMLElement)).toMatch(/S1R\$\s1\.000,00R\$\s100,00/);
    expect(root.querySelector('app-income-expense-bars title')?.textContent).toMatch(/S1 · Entradas: R\$\s1\.000,00/);
  });

  it('a pizza lista ícone, nome, percentual e valor; sem despesas mostra a mensagem', async () => {
    const { root } = await setup();
    const legend = root.querySelector('app-expense-pie ul');
    expect(legend?.textContent).toContain('Alimentação');
    expect(legend?.textContent).toContain('60,0%');
    expect(legend?.textContent).toMatch(/R\$\s150,00/);
    expect(root.querySelectorAll('app-expense-pie svg[role="img"] > circle').length).toBe(3);
    TestBed.resetTestingModule();

    const empty = await setup(overview({ expenseByCategory: [] }));
    expect(empty.root.querySelector('app-expense-pie')?.textContent).toContain('Sem despesas no período.');
  });

  it('a tabela mostra os lançamentos com tipo, valor com sinal e "—" sem subcategoria, e o link para Lançamentos', async () => {
    const { root } = await setup(
      overview({ latestTransactions: [txn('a', 'INCOME', '1000.00'), txn('b', 'EXPENSE', '50.00', true)] }),
    );

    const rows = root.querySelectorAll('app-latest-transactions table tbody tr');
    expect(rows).toHaveLength(2);
    expect(rows[0].textContent).toContain('Entrada');
    expect(rows[0].textContent).toMatch(/\+R\$\s1\.000,00/);
    expect(rows[0].textContent).toContain('—');
    expect(rows[1].textContent).toContain('Saída');
    expect(rows[1].textContent).toContain('Padaria');
    expect(root.querySelector('app-latest-transactions a[href="/lancamentos"]')?.textContent).toContain('Ver todas');
  });

  it('mês vazio: KPIs zerados e as mensagens de vazio', async () => {
    const { root } = await setup(
      overview({
        summary: { income: '0.00', expense: '0.00', balance: '0.00' },
        bars: [{ label: 'S1', income: '0.00', expense: '0.00' }],
        expenseByCategory: [],
        latestTransactions: [],
      }),
    );

    expect(text(root)).toContain('Nenhum lançamento no período.');
    expect(text(root)).toContain('Sem despesas no período.');
    expect(text(root)).toMatch(/SaldoR\$\s0,00/);
  });

  it('mudar o mês descarta o dado anterior e só mostra o do mês novo', async () => {
    const { fixture, http, root } = await setup();
    const month = TestBed.inject(ReferenceMonthStore);

    month.next();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(root.querySelector('[aria-busy="true"]')).not.toBeNull();
    expect(text(root)).not.toMatch(/R\$\s750,00/);

    const request = overviewRequest(http);
    expect(request.request.params.get('month')).toBe(month.monthKey());
    request.flush(overview({ summary: { income: '5.00', expense: '1.00', balance: '4.00' } }));
    await fixture.whenStable();
    expect(text(root)).toMatch(/R\$\s4,00/);
  });

  it('trocar a moeda consulta a outra moeda e mostra valores em EUR', async () => {
    const { fixture, http, root } = await setup();

    TestBed.inject(CurrencyStore).select('EUR');
    fixture.detectChanges();
    await fixture.whenStable();
    const request = overviewRequest(http);
    expect(request.request.params.get('currency')).toBe('EUR');
    request.flush(overview({ currency: 'EUR', summary: { income: '5000.00', expense: '0.00', balance: '5000.00' }, reservesTotal: '300.00' }));
    await fixture.whenStable();

    expect(text(root)).toMatch(/Saldo€\s5\.000,00/);
    expect(text(root)).not.toContain('R$');
  });

  it('erro mostra nova tentativa que consulta de novo', async () => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideHttpClient(withInterceptors([apiErrorInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(DashboardPage);
    await fixture.whenStable();
    overviewRequest(http).flush(null, { status: 500, statusText: 'x' });
    await fixture.whenStable();
    const root = fixture.nativeElement as HTMLElement;

    expect(text(root)).toContain('Não foi possível carregar o resumo do mês.');
    Array.from(root.querySelectorAll('button')).find((b) => b.textContent?.includes('Tentar novamente'))?.click();
    await fixture.whenStable();
    overviewRequest(http).flush(overview());
    await fixture.whenStable();
    expect(root.querySelector('app-kpi-card')).not.toBeNull();
  });
});
