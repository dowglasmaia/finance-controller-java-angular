import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { apiErrorInterceptor } from '../../../core/http/api-error.interceptor';
import { CurrencyStore } from '../../../core/state/currency.store';
import { ReferenceMonthStore } from '../../../core/state/reference-month.store';
import { Report } from '../data-access/reports-api.service';
import { changeText, monthName, rangeText } from '../reports-format';
import { ReportsPage } from './reports-page';

const FOOD = { id: 'food', name: 'Alimentação', color: '#4CAF50', icon: 'restaurant' };
const CAR = { id: 'car', name: 'Carro', color: '#FF9800', icon: 'directions_car' };

function report(over: Partial<Report> = {}): Report {
  return {
    period: 'MONTH',
    currency: 'BRL',
    range: { start: '2026-06-01', end: '2026-06-30' },
    summary: { income: '1250.50', expense: '300.00', balance: '950.50' },
    bars: [
      { label: 'S1', income: '1000.00', expense: '100.00' },
      { label: 'S2', income: '0.00', expense: '50.00' },
      { label: 'S3', income: '0.00', expense: '150.00' },
      { label: 'S4', income: '0.00', expense: '0.00' },
      { label: 'S5', income: '250.50', expense: '0.00' },
    ],
    expenseByCategory: [
      { category: FOOD, amount: '200.00', percentage: '66.6667' },
      { category: CAR, amount: '100.00', percentage: '33.3333' },
    ],
    ranking: [
      { category: FOOD, amount: '200.00', percentage: '66.6667' },
      { category: CAR, amount: '100.00', percentage: '33.3333' },
    ],
    ...over,
  };
}

async function setup(initial: Report = report()) {
  localStorage.clear();
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideHttpClient(withInterceptors([apiErrorInterceptor])),
      provideHttpClientTesting(),
    ],
  });
  const http = TestBed.inject(HttpTestingController);
  const fixture = TestBed.createComponent(ReportsPage);
  await fixture.whenStable();
  const first = expectReport(http);
  first.flush(initial);
  await fixture.whenStable();
  return { fixture, http, first, root: fixture.nativeElement as HTMLElement };
}

function expectReport(http: HttpTestingController): TestRequest {
  return http.expectOne((r) => r.method === 'GET' && r.url === '/api/v1/reports');
}

async function pick(fixture: ComponentFixture<unknown>, label: string): Promise<void> {
  const button = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('[role="radio"]')).find(
    (b) => b.textContent?.trim() === label,
  );
  if (!button) throw new Error('botão não encontrado: ' + label);
  button.click();
  await fixture.whenStable();
}

const text = (root: HTMLElement) => root.textContent?.replace(/\s+/g, ' ') ?? '';

describe('ReportsPage', () => {
  it('abre em Mês, pede o relatório com a âncora no dia 15 do mês de referência e mostra os KPIs', async () => {
    const { root, first } = await setup();

    const month = TestBed.inject(ReferenceMonthStore).monthKey();
    expect(first.request.params.get('period')).toBe('MONTH');
    expect(first.request.params.get('anchorDate')).toBe(`${month}-15`);
    expect(first.request.params.get('currency')).toBe('BRL');
    expect(text(root)).toContain('Relatórios');
    expect(text(root)).toContain('Análise financeira do período');
    expect(root.querySelector('[role="radio"][aria-checked="true"]')?.textContent?.trim()).toBe('Mês');
    expect(text(root)).toMatch(/Entradas\s*R\$\s1\.250,50/);
    expect(text(root)).toMatch(/Saídas\s*R\$\s300,00/);
    expect(text(root)).toMatch(/Saldo\s*R\$\s950,50/);
    expect(text(root)).toContain('Período: 01/06/2026 a 30/06/2026');
    const cards = root.querySelectorAll('app-kpi-card');
    expect(cards[0]?.querySelector('p.text-2xl.text-income')).not.toBeNull();
    expect(cards[1]?.querySelector('p.text-2xl.text-expense')).not.toBeNull();
    expect(cards[2]?.querySelector('p.text-2xl.text-success')).not.toBeNull();
  });

  it('saldo negativo fica em coral', async () => {
    const { root } = await setup(report({ summary: { income: '0.00', expense: '10.00', balance: '-10.00' } }));

    const cards = root.querySelectorAll('app-kpi-card');
    expect(cards[2]?.querySelector('p.text-expense')).not.toBeNull();
  });

  it('em Mês a soma das barras de entradas é igual ao KPI Entradas (tabela para leitor de tela)', async () => {
    const { root } = await setup();

    const rows = Array.from(root.querySelectorAll('app-income-expense-bars table tbody tr'));
    expect(rows.length).toBe(5);
    expect(rows[0]?.textContent).toMatch(/S1.*R\$\s1\.000,00.*R\$\s100,00/);
    const income = report().bars.reduce((sum, bar) => sum + Math.round(Number(bar.income) * 100), 0);
    expect(income / 100).toBe(Number(report().summary.income));
    expect(root.querySelector('app-income-expense-bars svg')?.getAttribute('aria-label')).toBe(
      'Entradas e saídas por faixa de 7 dias do mês',
    );
  });

  it('mostra a pizza e o ranking só com despesas, com percentual, valor e posição', async () => {
    const { root } = await setup();

    const ranking = root.querySelector('app-ranking-list');
    expect(ranking?.textContent).toContain('1º');
    expect(ranking?.textContent).toContain('Alimentação');
    expect(ranking?.textContent).toContain('66,7%');
    expect(ranking?.textContent).toMatch(/R\$\s200,00/);
    expect(ranking?.textContent).toContain('2º');
    expect(root.querySelector('app-expense-pie')?.textContent).toContain('Carro');
    expect(root.querySelector('app-monthly-summary-table')).toBeNull();
  });

  it('sem despesas mostra "Sem despesas no período." na pizza e no ranking', async () => {
    const { root } = await setup(
      report({ summary: { income: '5.00', expense: '0.00', balance: '5.00' }, expenseByCategory: [], ranking: [] }),
    );

    expect(root.querySelector('app-expense-pie')?.textContent).toContain('Sem despesas no período.');
    expect(root.querySelector('app-ranking-list')?.textContent).toContain('Sem despesas no período.');
  });

  it('Semana pede WEEK e mostra 7 barras Dom…Sáb, sem resumo mensal', async () => {
    const { fixture, http, root } = await setup();

    await pick(fixture, 'Semana');
    const request = expectReport(http);
    expect(request.request.params.get('period')).toBe('WEEK');
    request.flush(
      report({
        period: 'WEEK',
        range: { start: '2026-06-21', end: '2026-06-27' },
        bars: ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'].map((label) => ({ label, income: '0.00', expense: '0.00' })),
      }),
    );
    await fixture.whenStable();

    const labels = Array.from(root.querySelectorAll('app-income-expense-bars table tbody th')).map((th) => th.textContent?.trim());
    expect(labels).toEqual(['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb']);
    expect(root.querySelector('app-income-expense-bars svg')?.getAttribute('aria-label')).toBe('Entradas e saídas por dia da semana');
    expect(text(root)).toContain('Período: 21/06/2026 a 27/06/2026');
    expect(root.querySelector('app-monthly-summary-table')).toBeNull();
  });

  it('Ano mostra 12 barras e o Resumo Mensal com "—" na primeira linha e com base zero', async () => {
    const { fixture, http, root } = await setup();

    await pick(fixture, 'Ano');
    const request = expectReport(http);
    expect(request.request.params.get('period')).toBe('YEAR');
    const months = ['2026-01', '2026-02', '2026-03', '2026-04', '2026-05', '2026-06', '2026-07', '2026-08', '2026-09', '2026-10', '2026-11', '2026-12'];
    request.flush(
      report({
        period: 'YEAR',
        range: { start: '2026-01-01', end: '2026-12-31' },
        bars: months.map((m) => ({ label: monthName(m).slice(0, 3), income: '0.00', expense: '0.00' })),
        monthlySummary: months.map((month, i) => ({
          month,
          income: i === 0 ? '100.00' : i === 1 ? '150.00' : '0.00',
          expense: '0.00',
          balance: i === 0 ? '100.00' : i === 1 ? '150.00' : '0.00',
          // só fevereiro (+50%) e março (-100%) têm base; janeiro e abril em diante não
          ...(i === 1 ? { changePercent: '50.0' } : i === 2 ? { changePercent: '-100.0' } : {}),
        })),
      }),
    );
    await fixture.whenStable();

    expect(root.querySelectorAll('app-income-expense-bars table tbody tr').length).toBe(12);
    const rows = Array.from(root.querySelectorAll('app-monthly-summary-table tbody tr'));
    expect(rows.length).toBe(12);
    expect(rows[0]?.textContent).toContain('Jan/2026');
    expect(rows[0]?.querySelector('td:last-child')?.textContent?.trim()).toBe('—');
    expect(rows[1]?.querySelector('td:last-child')?.textContent?.trim()).toBe('+50,0%');
    expect(rows[1]?.querySelector('td:last-child.text-success')).not.toBeNull();
    expect(rows[2]?.querySelector('td:last-child')?.textContent?.trim()).toBe('-100,0%');
    expect(rows[2]?.querySelector('td:last-child.text-expense')).not.toBeNull();
    expect(rows[3]?.querySelector('td:last-child')?.textContent?.trim()).toBe('—');
    expect(text(root)).toContain('Resumo Mensal');
  });

  it('trocar o mês de referência, a moeda ou o período faz nova consulta e a resposta antiga não sobrescreve a nova', async () => {
    const { fixture, http, root } = await setup();

    const month = TestBed.inject(ReferenceMonthStore);
    month.previous();
    await fixture.whenStable();
    const stale = expectReport(http);
    expect(stale.request.params.get('anchorDate')).toBe(`${month.monthKey()}-15`);

    TestBed.inject(CurrencyStore).select('EUR');
    await fixture.whenStable();
    const fresh = expectReport(http);
    expect(fresh.request.params.get('currency')).toBe('EUR');
    expect(stale.cancelled).toBe(true);
    expect(text(root)).toContain('Carregando relatório');

    fresh.flush(report({ currency: 'EUR', summary: { income: '1.00', expense: '0.00', balance: '1.00' } }));
    await fixture.whenStable();
    expect(text(root)).toMatch(/Entradas\s*€\s1,00/);
  });

  it('erro de carregamento mostra a mensagem e tenta novamente', async () => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(withInterceptors([apiErrorInterceptor])), provideHttpClientTesting()],
    });
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(ReportsPage);
    const root = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
    expect(text(root)).toContain('Carregando relatório');

    expectReport(http).flush({ status: 500 }, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();
    expect(text(root)).toContain('Não foi possível carregar o relatório.');

    Array.from(root.querySelectorAll<HTMLButtonElement>('button')).find((b) => b.textContent?.includes('Tentar novamente'))?.click();
    await fixture.whenStable();
    expectReport(http).flush(report());
    await fixture.whenStable();
    expect(text(root)).toMatch(/Saldo\s*R\$\s950,50/);
  });
});

describe('reports-format', () => {
  it('formata mês, intervalo e variação', () => {
    expect(monthName('2026-06')).toBe('Jun/2026');
    expect(rangeText({ start: '2026-06-21', end: '2026-06-27' })).toBe('21/06/2026 a 27/06/2026');
    expect(changeText(undefined)).toBe('—');
    expect(changeText('50.0')).toBe('+50,0%');
    expect(changeText('-100.0')).toBe('-100,0%');
    expect(changeText('0.0')).toBe('0,0%');
  });
});
