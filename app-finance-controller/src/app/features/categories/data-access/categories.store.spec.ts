import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { AppStateReset } from '../../../core/state/app-state-reset';
import { Category } from './categories-api.service';
import { CategoriesStore } from './categories.store';

function category(id: string, name: string, type: Category['type'] = 'EXPENSE'): Category {
  return {
    id,
    name,
    type,
    icon: 'home',
    color: '#112233',
    subcategories: [{ id: `${id}-o`, name: 'Outros', protected: true }],
  };
}

function setup() {
  TestBed.configureTestingModule({
    providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting()],
  });
  return { store: TestBed.inject(CategoriesStore), http: TestBed.inject(HttpTestingController) };
}

describe('CategoriesStore', () => {
  it('carrega, ordena sem distinguir caixa e separa despesas de receitas (Ambos nas duas)', () => {
    const { store, http } = setup();

    store.ensureLoaded();
    expect(store.status()).toBe('loading');
    http
      .expectOne('/api/v1/categories')
      .flush([category('1', 'zebra'), category('2', 'Bônus', 'INCOME'), category('3', 'água', 'BOTH'), category('4', 'Abacaxi')]);

    expect(store.status()).toBe('loaded');
    expect(store.categories().map((c) => c.name)).toEqual(['Abacaxi', 'água', 'Bônus', 'zebra']);
    expect(store.expenseCategories().map((c) => c.name)).toEqual(['Abacaxi', 'água', 'zebra']);
    expect(store.incomeCategories().map((c) => c.name)).toEqual(['água', 'Bônus']);
  });

  it('ensureLoaded não repete a chamada quando já carregou, mas tenta de novo após erro', () => {
    const { store, http } = setup();

    store.ensureLoaded();
    http.expectOne('/api/v1/categories').flush(null, { status: 500, statusText: 'x' });
    expect(store.status()).toBe('error');

    store.ensureLoaded();
    http.expectOne('/api/v1/categories').flush([]);
    expect(store.status()).toBe('loaded');

    store.ensureLoaded();
    http.expectNone('/api/v1/categories');
  });

  it('criar, atualizar e excluir refletem no estado', () => {
    const { store, http } = setup();
    store.load();
    http.expectOne('/api/v1/categories').flush([category('1', 'Casa')]);

    store.create({ name: 'Pets', type: 'EXPENSE', icon: 'pets', color: '#112233', subcategories: ['Ração'] }).subscribe();
    const created = http.expectOne({ method: 'POST', url: '/api/v1/categories' });
    expect(created.request.body.subcategories).toEqual(['Ração']);
    created.flush(category('2', 'Pets'));
    expect(store.categories().map((c) => c.name)).toEqual(['Casa', 'Pets']);

    store.update('2', { name: 'Animais', type: 'BOTH', icon: 'pets', color: '#112233' }).subscribe();
    http.expectOne({ method: 'PUT', url: '/api/v1/categories/2' }).flush({ ...category('2', 'Animais', 'BOTH') });
    expect(store.byId('2')?.name).toBe('Animais');
    expect(store.incomeCategories().map((c) => c.name)).toEqual(['Animais']);

    store.remove('1').subscribe();
    http.expectOne({ method: 'DELETE', url: '/api/v1/categories/1' }).flush(null, { status: 204, statusText: 'No Content' });
    expect(store.categories().map((c) => c.id)).toEqual(['2']);
  });

  it('falha ao excluir não altera o estado', () => {
    const { store, http } = setup();
    store.load();
    http.expectOne('/api/v1/categories').flush([category('1', 'Casa')]);

    let failed = false;
    store.remove('1').subscribe({ error: () => (failed = true) });
    http
      .expectOne('/api/v1/categories/1')
      .flush({ code: 'CATEGORY_IN_USE', title: 'em uso', status: 409 }, { status: 409, statusText: 'Conflict' });

    expect(failed).toBe(true);
    expect(store.categories()).toHaveLength(1);
  });

  it('adiciona e remove subcategorias na categoria certa', () => {
    const { store, http } = setup();
    store.load();
    http.expectOne('/api/v1/categories').flush([category('1', 'Casa'), category('2', 'Carro')]);

    store.addSubcategory('1', 'Aluguel').subscribe();
    http
      .expectOne({ method: 'POST', url: '/api/v1/categories/1/subcategories' })
      .flush({ id: 's1', name: 'Aluguel', protected: false });
    expect(store.byId('1')?.subcategories.map((s) => s.name)).toEqual(['Outros', 'Aluguel']);
    expect(store.byId('2')?.subcategories).toHaveLength(1);

    store.removeSubcategory('1', 's1').subscribe();
    http
      .expectOne({ method: 'DELETE', url: '/api/v1/categories/1/subcategories/s1' })
      .flush(null, { status: 204, statusText: 'No Content' });
    expect(store.byId('1')?.subcategories.map((s) => s.name)).toEqual(['Outros']);
  });

  it('é limpo no logout (AppStateReset)', () => {
    const { store, http } = setup();
    store.load();
    http.expectOne('/api/v1/categories').flush([category('1', 'Casa')]);

    TestBed.inject(AppStateReset).resetAll();

    expect(store.categories()).toEqual([]);
    expect(store.status()).toBe('idle');
  });
});
