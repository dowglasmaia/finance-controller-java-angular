import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { apiErrorInterceptor } from '../../../core/http/api-error.interceptor';
import { CurrencyStore } from '../../../core/state/currency.store';
import { ToastService } from '../../../shared/ui/toast.service';
import { MoneyBox, MoneyBoxEntry, ReservesSummary } from '../data-access/money-boxes-api.service';
import { MoneyBoxesPage } from './money-boxes-page';

function box(over: Partial<MoneyBox> = {}): MoneyBox {
  return {
    id: 'box-1',
    name: 'Reserva',
    icon: 'savings',
    color: '#4CAF50',
    currency: 'BRL',
    balance: '1200.00',
    progress: '0.0000',
    createdAt: '2026-10-01T10:00:00Z',
    ...over,
  };
}

function summary(over: Partial<ReservesSummary> = {}): ReservesSummary {
  return { currency: 'BRL', total: '1200.00', progress: '0.0000', boxCount: 1, ...over };
}

function entry(over: Partial<MoneyBoxEntry> = {}): MoneyBoxEntry {
  return {
    id: 'e-1',
    moneyBoxId: 'box-1',
    kind: 'DEPOSIT',
    amount: '1000.00',
    currency: 'BRL',
    date: '2026-10-01',
    createdAt: '2026-10-01T10:00:00Z',
    ...over,
  };
}

async function setup(boxes: MoneyBox[] = [box()], sum: ReservesSummary = summary()) {
  localStorage.clear();
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideHttpClient(withInterceptors([apiErrorInterceptor])),
      provideHttpClientTesting(),
    ],
  });
  const http = TestBed.inject(HttpTestingController);
  const fixture = TestBed.createComponent(MoneyBoxesPage);
  await fixture.whenStable();
  flushLoad(http, boxes, sum);
  await fixture.whenStable();
  return { fixture, http, root: fixture.nativeElement as HTMLElement };
}

function flushLoad(http: HttpTestingController, boxes: MoneyBox[], sum: ReservesSummary, currency = 'BRL'): void {
  http.expectOne((r) => r.method === 'GET' && r.url === '/api/v1/money-boxes' && r.params.get('currency') === currency).flush(boxes);
  http.expectOne({ method: 'GET', url: `/api/v1/reserves/${currency}/summary` }).flush(sum);
}

async function click(fixture: ComponentFixture<unknown>, element: Element | null | undefined): Promise<void> {
  if (!element) throw new Error('elemento não encontrado');
  (element as HTMLElement).click();
  await fixture.whenStable();
}

const byLabel = (root: HTMLElement, label: string) => root.querySelector<HTMLButtonElement>(`button[aria-label="${label}"]`);
const byText = (root: HTMLElement, text: string) =>
  Array.from(root.querySelectorAll<HTMLButtonElement>('button')).find((b) => b.textContent?.trim() === text);

async function type(fixture: ComponentFixture<unknown>, input: HTMLInputElement | null | undefined, value: string): Promise<void> {
  if (!input) throw new Error('campo não encontrado');
  input.value = value;
  input.dispatchEvent(new Event('input'));
  await fixture.whenStable();
}

const field = (root: HTMLElement, name: string) => root.querySelector<HTMLInputElement>(`[formcontrolname="${name}"]`);
const submit = (root: HTMLElement) => root.querySelector<HTMLFormElement>('app-modal form')?.dispatchEvent(new Event('submit'));
const toasts = () => TestBed.inject(ToastService).toasts().map((t) => t.message);

describe('MoneyBoxesPage', () => {
  it('mostra os KPIs, a meta geral e os cartões só da moeda selecionada', async () => {
    const { root } = await setup(
      [box({ goal: '2000.00', progress: '0.6000', description: 'Conta remunerada' })],
      summary({ goal: '4000.00', progress: '0.3000' }),
    );

    expect(root.textContent).toContain('Cofrinhos');
    expect(root.textContent).toContain('Suas reservas e o progresso rumo às metas');
    expect(root.querySelector('app-kpi-card')?.textContent).toMatch(/Saldo Atual\s*R\$\s1\.200,00/);
    const kpis = root.querySelectorAll('app-kpi-card');
    expect(kpis[1]?.textContent).toMatch(/Meta Total\s*R\$\s4\.000,00/);
    expect(kpis[2]?.textContent).toContain('30%');
    expect(kpis[3]?.textContent).toMatch(/Reservas\s*1/);
    expect(root.textContent).toMatch(/30% concluído — R\$\s1\.200,00 de R\$\s4\.000,00/);
    const card = root.querySelector('app-money-box-card');
    expect(card?.textContent).toMatch(/R\$\s1\.200,00/);
    expect(card?.textContent).toContain('Conta remunerada');
    expect(card?.textContent).toMatch(/60% de R\$\s2\.000,00/);
    expect(root.textContent).toContain('Editar meta geral');
  });

  it('sem meta mostra "—" e o convite para definir a meta geral', async () => {
    const { root } = await setup();

    const kpis = root.querySelectorAll('app-kpi-card');
    expect(kpis[1]?.textContent).toContain('—');
    expect(kpis[2]?.textContent).toContain('—');
    expect(root.textContent).toContain('Definir meta geral');
    expect(root.textContent).not.toContain('concluído');
  });

  it('sem cofrinhos mostra o estado vazio', async () => {
    const { root } = await setup([], summary({ total: '0.00', boxCount: 0 }));

    expect(root.textContent).toContain('Nenhum cofrinho ainda. Crie sua primeira reserva.');
  });

  it('com saldo 0 o botão Saque fica desabilitado com a explicação "Saldo insuficiente"', async () => {
    const { root } = await setup([box({ balance: '0.00' })], summary({ total: '0.00' }));

    const withdraw = byLabel(root, 'Saque de Reserva');
    expect(withdraw?.disabled).toBe(true);
    expect(withdraw?.getAttribute('title')).toBe('Saldo insuficiente');
    expect(root.textContent).toContain('Saldo insuficiente para saque.');
    expect(byLabel(root, 'Aporte em Reserva')?.disabled).toBe(false);
  });

  it('cria um cofrinho com os valores exatos e recarrega a lista e o resumo', async () => {
    const { fixture, http, root } = await setup([], summary({ total: '0.00', boxCount: 0 }));

    await click(fixture, byText(root, 'Novo Cofrinho'));
    await type(fixture, field(root, 'name'), '  Viagem ');
    await type(fixture, field(root, 'goal'), '5000,5');
    submit(root);
    await fixture.whenStable();

    const req = http.expectOne({ method: 'POST', url: '/api/v1/money-boxes' });
    expect(req.request.body).toEqual({
      name: 'Viagem',
      icon: 'savings',
      color: '#FF9800',
      currency: 'BRL',
      goal: '5000.5',
    });
    req.flush(box({ name: 'Viagem' }));
    await fixture.whenStable();
    flushLoad(http, [box({ name: 'Viagem', balance: '0.00' })], summary({ total: '0.00' }));
    await fixture.whenStable();

    expect(toasts()).toContain('Cofrinho salvo.');
    expect(root.querySelector('app-modal')).toBeNull();
    expect(root.textContent).toContain('Viagem');
  });

  it('o aporte envia o tipo, o valor exato e a data; sem moeda, que é a do cofrinho', async () => {
    const { fixture, http, root } = await setup();

    await click(fixture, byLabel(root, 'Aporte em Reserva'));
    expect(root.textContent).toContain('Movimentar Reserva');
    expect(byText(root, 'Depositar')).toBeDefined();
    await type(fixture, field(root, 'amount'), '1000');
    await type(fixture, field(root, 'note'), 'Aporte inicial');
    submit(root);
    await fixture.whenStable();

    const req = http.expectOne({ method: 'POST', url: '/api/v1/money-boxes/box-1/entries' });
    expect(req.request.body.kind).toBe('DEPOSIT');
    expect(req.request.body.amount).toBe('1000');
    expect(req.request.body.note).toBe('Aporte inicial');
    expect(req.request.body.currency).toBeUndefined();
    expect(req.request.body.date).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    req.flush(entry());
    await fixture.whenStable();
    flushLoad(http, [box({ balance: '2200.00' })], summary({ total: '2200.00' }));
    await fixture.whenStable();

    expect(toasts()).toContain('Aporte registrado.');
    expect(root.textContent).toMatch(/R\$\s2\.200,00/);
  });

  it('saque acima do saldo é recusado: mensagem no diálogo, nada fecha e o saldo continua', async () => {
    const { fixture, http, root } = await setup();

    await click(fixture, byLabel(root, 'Saque de Reserva'));
    expect(byText(root, 'Sacar')).toBeDefined();
    await type(fixture, field(root, 'amount'), '1300');
    submit(root);
    await fixture.whenStable();

    http.expectOne({ method: 'POST', url: '/api/v1/money-boxes/box-1/entries' }).flush(
      { status: 409, code: 'INSUFFICIENT_BALANCE', title: 'Conflito', detail: 'Saldo insuficiente' },
      { status: 409, statusText: 'Conflict' },
    );
    await fixture.whenStable();

    expect(root.querySelector('app-modal [role="alert"]')?.textContent).toContain('Saldo insuficiente');
    expect(root.querySelector('app-modal')).not.toBeNull();
    expect(toasts()).not.toContain('Saque registrado.');
    expect(root.textContent).toMatch(/R\$\s1\.200,00/);
  });

  it('o detalhe lista os aportes com sinal, nota padrão e data; excluir o que negativaria o saldo é recusado', async () => {
    const { fixture, http, root } = await setup();

    await click(fixture, byText(root, 'Reserva (ver aportes)') ?? root.querySelector('h3 button'));
    http.expectOne({ method: 'GET', url: '/api/v1/money-boxes/box-1/entries' }).flush([
      entry({ id: 'e-2', kind: 'WITHDRAWAL', amount: '300.00', date: '2026-10-03', note: 'Mercado' }),
      entry({ id: 'e-1', kind: 'DEPOSIT', amount: '1500.00', date: '2026-10-01' }),
    ]);
    await fixture.whenStable();

    const detail = root.querySelector('app-money-box-detail');
    expect(detail?.textContent).toContain('Aportes');
    expect(detail?.textContent).toContain('Mercado');
    expect(detail?.textContent).toContain('Depósito');
    expect(detail?.textContent).toMatch(/−\s*Saque de\s*R\$\s300,00/);
    expect(detail?.textContent).toMatch(/\+\s*Depósito de\s*R\$\s1\.500,00/);
    expect(detail?.textContent).toContain('03/10/2026');

    await click(fixture, root.querySelector('button[aria-label^="Excluir depósito de"]'));
    expect(root.textContent).toContain('Excluir movimentação?');
    await click(fixture, byText(root, 'Excluir'));
    http.expectOne({ method: 'DELETE', url: '/api/v1/money-boxes/box-1/entries/e-1' }).flush(
      { status: 409, code: 'INSUFFICIENT_BALANCE', title: 'Conflito', detail: 'Saldo insuficiente' },
      { status: 409, statusText: 'Conflict' },
    );
    await fixture.whenStable();

    expect(toasts().some((m) => m.includes('deixaria o saldo negativo'))).toBe(true);
    expect(detail?.textContent).toContain('1.500,00');
  });

  it('detalhe sem aportes mostra "Nenhum aporte ainda."', async () => {
    const { fixture, http, root } = await setup();

    await click(fixture, root.querySelector('h3 button'));
    http.expectOne({ method: 'GET', url: '/api/v1/money-boxes/box-1/entries' }).flush([]);
    await fixture.whenStable();

    expect(root.textContent).toContain('Nenhum aporte ainda.');
  });

  it('excluir o cofrinho pede confirmação com o nome e remove só depois do sim', async () => {
    const { fixture, http, root } = await setup();

    await click(fixture, byLabel(root, 'Excluir Reserva'));
    expect(root.textContent).toContain('“Reserva”');
    await click(fixture, byText(root, 'Excluir'));

    http.expectOne({ method: 'DELETE', url: '/api/v1/money-boxes/box-1' }).flush(null, { status: 204, statusText: 'No Content' });
    await fixture.whenStable();
    flushLoad(http, [], summary({ total: '0.00', boxCount: 0 }));
    await fixture.whenStable();

    expect(toasts()).toContain('Cofrinho excluído.');
    expect(root.textContent).toContain('Nenhum cofrinho ainda.');
  });

  it('a meta geral: salva o valor exato; vazio manda 0 e remove', async () => {
    const { fixture, http, root } = await setup();

    await click(fixture, byText(root, 'Definir meta geral'));
    expect(root.textContent).toContain('O alvo total que você quer guardar somando todos os cofrinhos.');
    await type(fixture, field(root, 'goal'), '10000');
    submit(root);
    await fixture.whenStable();
    const set = http.expectOne({ method: 'PUT', url: '/api/v1/reserves/BRL/goal' });
    expect(set.request.body).toEqual({ amount: '10000' });
    set.flush(summary({ goal: '10000.00', progress: '0.1200' }));
    await fixture.whenStable();

    expect(toasts()).toContain('Meta geral salva.');
    expect(root.textContent).toContain('Editar meta geral');
    expect(root.textContent).toMatch(/12% concluído/);

    await click(fixture, byText(root, 'Editar meta geral'));
    expect(field(root, 'goal')?.value).toBe('10000,00');
    await type(fixture, field(root, 'goal'), '');
    submit(root);
    await fixture.whenStable();
    const clear = http.expectOne({ method: 'PUT', url: '/api/v1/reserves/BRL/goal' });
    expect(clear.request.body).toEqual({ amount: '0' });
    clear.flush(summary());
    await fixture.whenStable();

    expect(root.textContent).toContain('Definir meta geral');
  });

  it('em cofrinho com aportes o campo Moeda fica desabilitado com a mensagem de ajuda', async () => {
    const { fixture, http, root } = await setup();

    await click(fixture, byLabel(root, 'Editar Reserva'));
    http.expectOne({ method: 'GET', url: '/api/v1/money-boxes/box-1/entries' }).flush([entry()]);
    await fixture.whenStable();

    expect(field(root, 'name')?.value).toBe('Reserva');
    expect(root.querySelector<HTMLSelectElement>('select[formcontrolname="currency"]')?.disabled).toBe(true);
    expect(root.textContent).toContain('Não é possível alterar a moeda de um cofrinho com aportes.');
  });

  it('sem aportes o campo Moeda fica habilitado', async () => {
    const { fixture, http, root } = await setup();

    await click(fixture, byLabel(root, 'Editar Reserva'));
    http.expectOne({ method: 'GET', url: '/api/v1/money-boxes/box-1/entries' }).flush([]);
    await fixture.whenStable();

    expect(root.querySelector<HTMLSelectElement>('select[formcontrolname="currency"]')?.disabled).toBe(false);
  });

  it('trocar a moeda recarrega a lista e o resumo daquela moeda', async () => {
    const { fixture, http, root } = await setup();

    TestBed.inject(CurrencyStore).select('EUR');
    await fixture.whenStable();
    flushLoad(http, [box({ id: 'eur-1', name: 'Viagem', currency: 'EUR', balance: '400.00' })], summary({ currency: 'EUR', total: '400.00' }), 'EUR');
    await fixture.whenStable();

    expect(root.textContent).toContain('Viagem');
    expect(root.textContent).not.toContain('Reserva (ver aportes)');
    expect(root.querySelector('app-money-box-card')?.textContent).toMatch(/400,00/);
  });

  it('erro de carregamento mostra a mensagem e tenta novamente', async () => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(withInterceptors([apiErrorInterceptor])), provideHttpClientTesting()],
    });
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(MoneyBoxesPage);
    const root = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
    expect(root.textContent).toContain('Carregando cofrinhos');

    http.expectOne((r) => r.url === '/api/v1/money-boxes').flush({ status: 500 }, { status: 500, statusText: 'Server Error' });
    // o forkJoin cancela a segunda chamada quando a primeira falha
    http.match((r) => r.url === '/api/v1/reserves/BRL/summary');
    await fixture.whenStable();
    expect(root.textContent).toContain('Não foi possível carregar os cofrinhos.');

    await click(fixture, byText(root, 'Tentar novamente'));
    flushLoad(http, [box()], summary());
    await fixture.whenStable();
    expect(root.textContent).toMatch(/R\$\s1\.200,00/);
  });
});
