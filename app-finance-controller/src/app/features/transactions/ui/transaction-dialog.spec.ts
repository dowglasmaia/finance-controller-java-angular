import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, provideZonelessChangeDetection, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { apiErrorInterceptor } from '../../../core/http/api-error.interceptor';
import { CurrencyStore } from '../../../core/state/currency.store';
import { ToastService } from '../../../shared/ui/toast.service';
import { Category } from '../../categories/data-access/categories-api.service';
import { Transaction } from '../data-access/transactions-api.service';
import { TransactionDialog } from './transaction-dialog';

const CATEGORIES: Category[] = [
  { id: 'food', name: 'Alimentação', type: 'EXPENSE', icon: 'restaurant', color: '#4CAF50', subcategories: [{ id: 'food-o', name: 'Outros', protected: true }, { id: 'food-m', name: 'Mercado', protected: false }] },
  { id: 'pay', name: 'Salário', type: 'INCOME', icon: 'work', color: '#009688', subcategories: [{ id: 'pay-o', name: 'Outros', protected: true }] },
  { id: 'both', name: 'Extras', type: 'BOTH', icon: 'sell', color: '#FFC107', subcategories: [{ id: 'both-o', name: 'Outros', protected: true }] },
];

const EXISTING: Transaction = {
  id: 't1',
  kind: 'EXPENSE',
  description: 'Feira',
  amount: '45.90',
  currency: 'EUR',
  date: '2026-09-12',
  category: { id: 'food', name: 'Alimentação', color: '#4CAF50', icon: 'restaurant' },
  subcategory: { id: 'food-m', name: 'Mercado' },
  notes: 'domingo',
  createdAt: '2026-09-12T10:00:00Z',
  updatedAt: '2026-09-12T10:00:00Z',
};

@Component({
  imports: [TransactionDialog],
  template: `<app-transaction-dialog [transaction]="transaction()" (saved)="saved.set(saved() + 1)" (closed)="closedCount.set(closedCount() + 1)" />`,
})
class Host {
  readonly transaction = signal<Transaction | null>(null);
  readonly saved = signal(0);
  readonly closedCount = signal(0);
}

async function setup(transaction: Transaction | null = null) {
  localStorage.clear();
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideHttpClient(withInterceptors([apiErrorInterceptor])),
      provideHttpClientTesting(),
    ],
  });
  const http = TestBed.inject(HttpTestingController);
  const fixture = TestBed.createComponent(Host);
  fixture.componentInstance.transaction.set(transaction);
  await fixture.whenStable();
  http.expectOne('/api/v1/categories').flush(CATEGORIES);
  await fixture.whenStable();
  return { fixture, host: fixture.componentInstance, http, root: fixture.nativeElement as HTMLElement };
}

async function type(fixture: ComponentFixture<unknown>, el: HTMLInputElement | HTMLTextAreaElement | null, value: string): Promise<void> {
  if (!el) throw new Error('campo não encontrado');
  el.value = value;
  el.dispatchEvent(new Event('input'));
  await fixture.whenStable();
}

async function choose(fixture: ComponentFixture<unknown>, el: HTMLSelectElement | null, value: string): Promise<void> {
  if (!el) throw new Error('select não encontrado');
  el.value = value;
  el.dispatchEvent(new Event('change'));
  await fixture.whenStable();
}

const field = <T extends HTMLElement>(root: HTMLElement, name: string) => root.querySelector<T>(`[formcontrolname="${name}"]`);
const submit = (root: HTMLElement) => root.querySelector<HTMLFormElement>('form')?.dispatchEvent(new Event('submit'));
const optionLabels = (select: HTMLSelectElement | null) => Array.from(select?.options ?? []).map((o) => o.textContent?.trim());

describe('TransactionDialog', () => {
  it('não chama a API vazio e mostra as mensagens exatas', async () => {
    const { fixture, http, root } = await setup();

    submit(root);
    await fixture.whenStable();

    expect(root.textContent).toContain('Campo obrigatório');
    expect(root.textContent).toContain('Informe um valor');
    expect(root.textContent).toContain('Selecione uma categoria');
    http.expectNone('/api/v1/transactions');
  });

  it('valor zero mostra "O valor deve ser maior que zero"', async () => {
    const { fixture, root } = await setup();

    await type(fixture, field(root, 'amount'), '0');
    submit(root);
    await fixture.whenStable();

    expect(root.textContent).toContain('O valor deve ser maior que zero');
  });

  it('as categorias seguem o tipo (Ambos nos dois) e trocar o tipo limpa uma escolha incompatível', async () => {
    const { fixture, root } = await setup();
    const category = field<HTMLSelectElement>(root, 'categoryId');
    expect(optionLabels(category)).toEqual(['Selecione…', 'Alimentação', 'Extras']);

    await choose(fixture, category, 'food');
    const income = Array.from(root.querySelectorAll<HTMLButtonElement>('[role="radio"]')).find((b) => b.textContent?.includes('Entrada'));
    income?.click();
    await fixture.whenStable();

    expect(optionLabels(category)).toEqual(['Selecione…', 'Extras', 'Salário']);
    expect(category?.value).toBe('');
  });

  it('cria uma despesa com o valor decimal exato, o mês de referência e a moeda do lançamento', async () => {
    const { fixture, host, http, root } = await setup();
    const toast = TestBed.inject(ToastService);
    const currency = TestBed.inject(CurrencyStore);

    await type(fixture, field(root, 'description'), '  Mercado  ');
    await type(fixture, field(root, 'amount'), '1234,5');
    await choose(fixture, field(root, 'currency'), 'EUR');
    await choose(fixture, field(root, 'categoryId'), 'food');
    await choose(fixture, field(root, 'subcategoryId'), 'food-m');
    submit(root);
    await fixture.whenStable();

    const req = http.expectOne({ method: 'POST', url: '/api/v1/transactions' });
    expect(req.request.body).toMatchObject({
      kind: 'EXPENSE',
      description: 'Mercado',
      amount: '1234.5',
      currency: 'EUR',
      categoryId: 'food',
      subcategoryId: 'food-m',
    });
    expect(req.request.body.referenceMonth).toMatch(/^\d{4}-\d{2}$/);
    expect(req.request.body.notes).toBeUndefined();
    req.flush({ transaction: { ...EXISTING, currency: 'EUR' }, budgetAlert: { scope: 'CATEGORY', status: 'EXCEEDED', categoryName: 'Alimentação' } });
    await fixture.whenStable();

    const messages = toast.toasts().map((t) => t.message);
    expect(messages).toEqual(['Lançamento salvo.', 'Meta de Alimentação estourada!']);
    expect(currency.selectedCurrency()).toBe('EUR');
    expect(host.saved()).toBe(1);
    expect(host.closedCount()).toBe(1);
  });

  it('receita não gera alerta e subcategoria acompanha a categoria', async () => {
    const { fixture, http, root } = await setup();
    const toast = TestBed.inject(ToastService);
    const income = Array.from(root.querySelectorAll<HTMLButtonElement>('[role="radio"]')).find((b) => b.textContent?.includes('Entrada'));
    income?.click();
    await fixture.whenStable();

    await type(fixture, field(root, 'description'), 'Salário');
    await type(fixture, field(root, 'amount'), '5000');
    await choose(fixture, field(root, 'categoryId'), 'pay');
    expect(optionLabels(field(root, 'subcategoryId'))).toEqual(['Nenhuma', 'Outros']);
    submit(root);
    await fixture.whenStable();

    http.expectOne({ method: 'POST', url: '/api/v1/transactions' }).flush({ transaction: { ...EXISTING, kind: 'INCOME', currency: 'BRL' } });
    await fixture.whenStable();

    expect(toast.toasts().map((t) => t.message)).toEqual(['Lançamento salvo.']);
  });

  it('erro do servidor de tipo incompatível aparece no campo Categoria e não fecha', async () => {
    const { fixture, host, http, root } = await setup();

    await type(fixture, field(root, 'description'), 'x');
    await type(fixture, field(root, 'amount'), '10');
    await choose(fixture, field(root, 'categoryId'), 'both');
    submit(root);
    await fixture.whenStable();
    http
      .expectOne({ method: 'POST', url: '/api/v1/transactions' })
      .flush({ code: 'CATEGORY_TYPE_MISMATCH', title: 't', status: 409 }, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();

    expect(field(root, 'categoryId')?.getAttribute('aria-invalid')).toBe('true');
    expect(root.textContent).toContain('A categoria não aceita este tipo de lançamento.');
    expect(host.closedCount()).toBe(0);
  });

  it('erro de validação do servidor aparece no campo do valor', async () => {
    const { fixture, http, root } = await setup();

    await type(fixture, field(root, 'description'), 'x');
    await type(fixture, field(root, 'amount'), '10');
    await choose(fixture, field(root, 'categoryId'), 'food');
    submit(root);
    await fixture.whenStable();
    http.expectOne({ method: 'POST', url: '/api/v1/transactions' }).flush(
      { code: 'VALIDATION_FAILED', title: 't', status: 400, errors: [{ field: 'amount', message: 'O valor excede o máximo permitido' }] },
      { status: 400, statusText: 'Bad Request' },
    );
    await fixture.whenStable();

    expect(field(root, 'amount')?.getAttribute('aria-invalid')).toBe('true');
    expect(root.textContent).toContain('O valor excede o máximo permitido');
  });

  it('editar abre preenchido, preserva moeda e data e envia PUT', async () => {
    const { fixture, http, root } = await setup(EXISTING);

    expect(field<HTMLInputElement>(root, 'description')?.value).toBe('Feira');
    expect(field<HTMLInputElement>(root, 'amount')?.value).toBe('45,90');
    expect(field<HTMLSelectElement>(root, 'currency')?.value).toBe('EUR');
    expect(field<HTMLInputElement>(root, 'date')?.value).toBe('2026-09-12');
    expect(field<HTMLSelectElement>(root, 'categoryId')?.value).toBe('food');
    expect(field<HTMLSelectElement>(root, 'subcategoryId')?.value).toBe('food-m');
    expect(field<HTMLTextAreaElement>(root, 'notes')?.value).toBe('domingo');
    expect(root.textContent).toContain('Editar Lançamento');

    submit(root);
    await fixture.whenStable();
    const req = http.expectOne({ method: 'PUT', url: '/api/v1/transactions/t1' });
    expect(req.request.body).toMatchObject({ amount: '45.90', currency: 'EUR', date: '2026-09-12', notes: 'domingo' });
    req.flush({ transaction: EXISTING });
  });
});
