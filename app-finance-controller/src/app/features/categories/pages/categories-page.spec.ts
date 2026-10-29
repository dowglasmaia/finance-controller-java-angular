import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { apiErrorInterceptor } from '../../../core/http/api-error.interceptor';
import { ToastService } from '../../../shared/ui/toast.service';
import { Category } from '../data-access/categories-api.service';
import { CategoriesPage } from './categories-page';

function category(id: string, name: string, type: Category['type'], subs: string[] = []): Category {
  return {
    id,
    name,
    type,
    icon: 'home',
    color: '#112233',
    subcategories: [
      { id: `${id}-o`, name: 'Outros', protected: true },
      ...subs.map((s, i) => ({ id: `${id}-${i}`, name: s, protected: false })),
    ],
  };
}

const LIST: Category[] = [
  category('1', 'Casa', 'EXPENSE', ['Aluguel', 'Luz', 'Água']),
  category('2', 'Salário', 'INCOME'),
  category('3', 'Extras', 'BOTH'),
];

async function setup(list: Category[] = LIST) {
  TestBed.configureTestingModule({
    providers: [provideZonelessChangeDetection(), provideHttpClient(withInterceptors([apiErrorInterceptor])), provideHttpClientTesting()],
  });
  const http = TestBed.inject(HttpTestingController);
  const fixture = TestBed.createComponent(CategoriesPage);
  await fixture.whenStable();
  http.expectOne('/api/v1/categories').flush(list);
  await fixture.whenStable();
  return { fixture, http, root: fixture.nativeElement as HTMLElement };
}

async function click(fixture: ComponentFixture<unknown>, element: Element | null): Promise<void> {
  (element as HTMLElement).click();
  await fixture.whenStable();
}

const names = (root: HTMLElement) => Array.from(root.querySelectorAll('app-category-card h3')).map((h) => h.textContent?.trim());

describe('CategoriesPage', () => {
  it('mostra carregando e depois as despesas; Ambos aparece nas duas abas', async () => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(withInterceptors([apiErrorInterceptor])), provideHttpClientTesting()],
    });
    const fixture = TestBed.createComponent(CategoriesPage);
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Carregando categorias');
    TestBed.inject(HttpTestingController).expectOne('/api/v1/categories').flush(LIST);
    await fixture.whenStable();
    const root = fixture.nativeElement as HTMLElement;

    expect(names(root)).toEqual(['Casa', 'Extras']);

    await click(fixture, root.querySelectorAll('[role="tab"]')[1]);
    expect(names(root)).toEqual(['Extras', 'Salário']);
  });

  it('limita a 3 chips e indica +N', async () => {
    const { root } = await setup([category('1', 'Casa', 'EXPENSE', ['Aluguel', 'Luz', 'Água'])]);

    const chips = Array.from(root.querySelectorAll('app-category-card li')).map((li) => li.textContent?.trim());
    expect(chips).toEqual(['Outros', 'Aluguel', 'Luz', '+1']);
  });

  it('estado vazio', async () => {
    const { root } = await setup([]);

    expect(root.textContent).toContain('Nenhuma categoria cadastrada.');
  });

  it('erro de carregamento oferece nova tentativa', async () => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(withInterceptors([apiErrorInterceptor])), provideHttpClientTesting()],
    });
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(CategoriesPage);
    await fixture.whenStable();
    http.expectOne('/api/v1/categories').flush(null, { status: 500, statusText: 'x' });
    await fixture.whenStable();
    const root = fixture.nativeElement as HTMLElement;

    expect(root.textContent).toContain('Não foi possível carregar as categorias.');
    const retry = Array.from(root.querySelectorAll('button')).find((b) => b.textContent?.includes('Tentar novamente'));
    await click(fixture, retry ?? null);
    http.expectOne('/api/v1/categories').flush(LIST);
    await fixture.whenStable();
    expect(names(root)).toEqual(['Casa', 'Extras']);
  });

  it('excluir com lançamentos vinculados mostra a mensagem e nada muda', async () => {
    const { fixture, http, root } = await setup();
    const toast = TestBed.inject(ToastService);

    await click(fixture, root.querySelector('[aria-label="Excluir Casa"]'));
    expect(root.textContent).toContain('Excluir categoria?');
    const confirm = Array.from(root.querySelectorAll('app-confirm-dialog button')).find((b) => b.textContent?.includes('Excluir'));
    await click(fixture, confirm ?? null);
    http
      .expectOne({ method: 'DELETE', url: '/api/v1/categories/1' })
      .flush({ code: 'CATEGORY_IN_USE', title: 'x', status: 409 }, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();

    expect(toast.toasts().map((t) => t.message)).toContain(
      'Categoria possui lançamentos vinculados e não pode ser excluída.',
    );
    expect(names(root)).toEqual(['Casa', 'Extras']);
  });

  it('excluir com sucesso remove o cartão e avisa', async () => {
    const { fixture, http, root } = await setup();
    const toast = TestBed.inject(ToastService);

    await click(fixture, root.querySelector('[aria-label="Excluir Casa"]'));
    const confirm = Array.from(root.querySelectorAll('app-confirm-dialog button')).find((b) => b.textContent?.includes('Excluir'));
    await click(fixture, confirm ?? null);
    http.expectOne({ method: 'DELETE', url: '/api/v1/categories/1' }).flush(null, { status: 204, statusText: 'No Content' });
    await fixture.whenStable();

    expect(names(root)).toEqual(['Extras']);
    expect(toast.toasts().map((t) => t.message)).toContain('Categoria excluída.');
  });
});
