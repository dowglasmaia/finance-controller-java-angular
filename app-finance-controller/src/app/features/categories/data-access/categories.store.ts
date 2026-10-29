import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { AppStateReset } from '../../../core/state/app-state-reset';
import {
  CategoriesApiService,
  Category,
  CategoryRequest,
  CreateCategoryRequest,
  Subcategory,
} from './categories-api.service';

export type CategoriesStatus = 'idle' | 'loading' | 'loaded' | 'error';

function byName(a: Category, b: Category): number {
  return a.name.localeCompare(b.name, 'pt-BR', { sensitivity: 'base' });
}

/**
 * Dono do estado das categorias do utilizador. Lançamentos e Metas leem `categories()` nos seus seletores, então
 * qualquer mutação aqui já se reflete neles. A ordem (nome, sem distinguir caixa) é a do servidor e é mantida.
 * Limpo no logout.
 */
@Injectable({ providedIn: 'root' })
export class CategoriesStore {
  private readonly api = inject(CategoriesApiService);

  private readonly items = signal<readonly Category[]>([]);
  private readonly state = signal<CategoriesStatus>('idle');

  readonly categories = this.items.asReadonly();
  readonly status = this.state.asReadonly();
  readonly expenseCategories = computed(() => this.items().filter((c) => c.type !== 'INCOME'));
  readonly incomeCategories = computed(() => this.items().filter((c) => c.type !== 'EXPENSE'));

  constructor() {
    inject(AppStateReset).register(() => this.clear());
  }

  /** Carrega do servidor (também usado para "Tentar novamente"). */
  load(): void {
    this.state.set('loading');
    this.api.listCategories().subscribe({
      next: (list) => {
        this.items.set([...list].sort(byName));
        this.state.set('loaded');
      },
      error: () => this.state.set('error'),
    });
  }

  /** Carrega apenas se ainda não houver dados (ou se a última tentativa falhou). */
  ensureLoaded(): void {
    if (this.state() === 'idle' || this.state() === 'error') {
      this.load();
    }
  }

  byId(id: string): Category | undefined {
    return this.items().find((c) => c.id === id);
  }

  create(request: CreateCategoryRequest): Observable<Category> {
    return this.api.createCategory(request).pipe(tap((created) => this.upsert(created)));
  }

  update(id: string, request: CategoryRequest): Observable<Category> {
    return this.api.updateCategory(id, request).pipe(tap((updated) => this.upsert(updated)));
  }

  remove(id: string): Observable<void> {
    return this.api.deleteCategory(id).pipe(tap(() => this.items.update((list) => list.filter((c) => c.id !== id))));
  }

  addSubcategory(categoryId: string, name: string): Observable<Subcategory> {
    return this.api.addSubcategory(categoryId, name).pipe(
      tap((created) => this.patch(categoryId, (c) => ({ ...c, subcategories: [...c.subcategories, created] }))),
    );
  }

  removeSubcategory(categoryId: string, subcategoryId: string): Observable<void> {
    return this.api.deleteSubcategory(categoryId, subcategoryId).pipe(
      tap(() =>
        this.patch(categoryId, (c) => ({ ...c, subcategories: c.subcategories.filter((s) => s.id !== subcategoryId) })),
      ),
    );
  }

  clear(): void {
    this.items.set([]);
    this.state.set('idle');
  }

  private upsert(category: Category): void {
    this.items.update((list) => [...list.filter((c) => c.id !== category.id), category].sort(byName));
  }

  private patch(id: string, change: (category: Category) => Category): void {
    this.items.update((list) => list.map((c) => (c.id === id ? change(c) : c)));
  }
}
