import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, provideZonelessChangeDetection, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { apiErrorInterceptor } from '../../../core/http/api-error.interceptor';
import { ToastService } from '../../../shared/ui/toast.service';
import { Category } from '../data-access/categories-api.service';
import { CategoryDialog } from './category-dialog';

const EXISTING: Category = {
  id: 'c1',
  name: 'Casa',
  type: 'EXPENSE',
  icon: 'home',
  color: '#2196F3',
  subcategories: [
    { id: 'o1', name: 'Outros', protected: true },
    { id: 's1', name: 'Aluguel', protected: false },
  ],
};

@Component({
  imports: [CategoryDialog],
  template: `<app-category-dialog [category]="category()" (closed)="closedCount.set(closedCount() + 1)" />`,
})
class Host {
  readonly category = signal<Category | null>(null);
  readonly closedCount = signal(0);
}

async function setup(category: Category | null = null) {
  TestBed.configureTestingModule({
    providers: [provideZonelessChangeDetection(), provideHttpClient(withInterceptors([apiErrorInterceptor])), provideHttpClientTesting()],
  });
  const fixture = TestBed.createComponent(Host);
  fixture.componentInstance.category.set(category);
  await fixture.whenStable();
  return { fixture, host: fixture.componentInstance, http: TestBed.inject(HttpTestingController), root: fixture.nativeElement as HTMLElement };
}

async function type(fixture: ComponentFixture<unknown>, input: HTMLInputElement | null, value: string): Promise<void> {
  if (!input) {
    throw new Error('campo não encontrado');
  }
  input.value = value;
  input.dispatchEvent(new Event('input'));
  await fixture.whenStable();
}

function submit(root: HTMLElement): void {
  root.querySelector<HTMLFormElement>('form')?.dispatchEvent(new Event('submit'));
}

const nameInput = (root: HTMLElement) => root.querySelector<HTMLInputElement>('input[formcontrolname="name"]');
const subInput = (root: HTMLElement) => root.querySelector<HTMLInputElement>('input[aria-label="Nova subcategoria"]');

describe('CategoryDialog', () => {
  it('criar: exige o nome e não chama a API vazio', async () => {
    const { fixture, http, root } = await setup();

    submit(root);
    await fixture.whenStable();

    expect(root.textContent).toContain('Campo obrigatório');
    http.expectNone('/api/v1/categories');
  });

  it('criar: "Outros" aparece protegido e sem botão de remover; subcategorias vão junto ao salvar', async () => {
    const { fixture, host, http, root } = await setup();
    const toast = TestBed.inject(ToastService);

    expect(root.querySelector('[aria-label="Remover Outros"]')).toBeNull();
    await type(fixture, nameInput(root), 'Pets');
    await type(fixture, subInput(root), 'Ração');
    root.querySelector<HTMLButtonElement>('[aria-label="Adicionar subcategoria"]')?.click();
    await fixture.whenStable();
    await type(fixture, subInput(root), 'ração');
    root.querySelector<HTMLButtonElement>('[aria-label="Adicionar subcategoria"]')?.click();
    await fixture.whenStable();
    expect(root.textContent).toContain('Já existe uma subcategoria com este nome.');

    submit(root);
    await fixture.whenStable();
    const req = http.expectOne({ method: 'POST', url: '/api/v1/categories' });
    expect(req.request.body).toMatchObject({ name: 'Pets', type: 'EXPENSE', subcategories: ['Ração'] });
    req.flush({ ...EXISTING, id: 'c2', name: 'Pets' });
    await fixture.whenStable();

    expect(host.closedCount()).toBe(1);
    expect(toast.toasts().map((t) => t.message)).toContain('Categoria salva.');
  });

  it('nome duplicado: mostra a mensagem no campo e não fecha', async () => {
    const { fixture, host, http, root } = await setup();

    await type(fixture, nameInput(root), 'casa');
    submit(root);
    await fixture.whenStable();
    http
      .expectOne({ method: 'POST', url: '/api/v1/categories' })
      .flush({ code: 'CATEGORY_NAME_DUPLICATED', title: 'dup', status: 409 }, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();

    expect(root.querySelector('input[formcontrolname="name"]')?.getAttribute('aria-invalid')).toBe('true');
    expect(root.textContent).toContain('Já existe uma categoria com este nome.');
    expect(host.closedCount()).toBe(0);
  });

  it('editar: conflito de tipo aparece no campo Tipo', async () => {
    const { fixture, http, root } = await setup(EXISTING);

    const select = root.querySelector<HTMLSelectElement>('select[formcontrolname="type"]');
    if (!select) throw new Error('select');
    select.value = 'INCOME';
    select.dispatchEvent(new Event('change'));
    submit(root);
    await fixture.whenStable();
    http
      .expectOne({ method: 'PUT', url: '/api/v1/categories/c1' })
      .flush({ code: 'CATEGORY_TYPE_CONFLICT', title: 'c', status: 409 }, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();

    expect(select.getAttribute('aria-invalid')).toBe('true');
    expect(root.textContent).toContain('Existem lançamentos incompatíveis com o novo tipo desta categoria.');
  });

  it('editar: adicionar grava na hora e remover pede confirmação sobre os lançamentos', async () => {
    const { fixture, http, root } = await setup(EXISTING);
    expect(root.querySelector('[aria-label="Remover Outros"]')).toBeNull();

    await type(fixture, subInput(root), 'Luz');
    root.querySelector<HTMLButtonElement>('[aria-label="Adicionar subcategoria"]')?.click();
    await fixture.whenStable();
    http
      .expectOne({ method: 'POST', url: '/api/v1/categories/c1/subcategories' })
      .flush({ id: 's2', name: 'Luz', protected: false });
    await fixture.whenStable();

    root.querySelector<HTMLButtonElement>('[aria-label="Remover Aluguel"]')?.click();
    await fixture.whenStable();
    expect(root.textContent).toContain('ficarão sem subcategoria');
    http.expectNone('/api/v1/categories/c1/subcategories/s1');
  });
});
