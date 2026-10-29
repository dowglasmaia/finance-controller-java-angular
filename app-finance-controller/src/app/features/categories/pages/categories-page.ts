import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { LucideAngularModule, Plus, Tags } from 'lucide-angular';
import { isApiError } from '../../../core/http/api-error';
import { AppPanel } from '../../../shared/ui/app-panel';
import { ButtonDirective } from '../../../shared/ui/button';
import { ConfirmDialog } from '../../../shared/ui/confirm-dialog';
import { EmptyState } from '../../../shared/ui/empty-state';
import { PageHeader } from '../../../shared/ui/page-header';
import { ToastService } from '../../../shared/ui/toast.service';
import { Category } from '../data-access/categories-api.service';
import { CategoriesStore } from '../data-access/categories.store';
import { CategoryCard } from '../ui/category-card';
import { CategoryDialog } from '../ui/category-dialog';

type Tab = 'EXPENSE' | 'INCOME';

const IN_USE_MESSAGE = 'Categoria possui lançamentos vinculados e não pode ser excluída.';

/** `/categorias`: abas Despesas/Receitas (uma categoria `Ambos` aparece nas duas), criação, edição e exclusão. */
@Component({
  selector: 'app-categories-page',
  imports: [
    PageHeader,
    AppPanel,
    EmptyState,
    ButtonDirective,
    CategoryCard,
    CategoryDialog,
    ConfirmDialog,
    LucideAngularModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-page-header title="Categorias" subtitle="Organize suas despesas e receitas">
      <button actions type="button" appButton (click)="openCreate()">
        <lucide-icon [img]="plus" [size]="18" aria-hidden="true" />
        Nova Categoria
      </button>
    </app-page-header>

    <div role="tablist" aria-label="Tipo de categoria" class="mb-4 inline-flex rounded-control border border-line bg-surface-sunken p-1">
      @for (option of tabs; track option.value) {
        <button
          type="button"
          role="tab"
          class="min-h-11 rounded-control px-4 text-sm font-semibold text-content-muted aria-selected:bg-accent aria-selected:text-on-accent"
          [attr.aria-selected]="tab() === option.value"
          (click)="tab.set(option.value)"
          (keydown.arrowright)="tab.set('INCOME')"
          (keydown.arrowleft)="tab.set('EXPENSE')"
        >
          {{ option.label }}
        </button>
      }
    </div>

    @switch (store.status()) {
      @case ('error') {
        <app-panel>
          <app-empty-state message="Não foi possível carregar as categorias.">
            <button type="button" appButton variant="secondary" (click)="store.load()">Tentar novamente</button>
          </app-empty-state>
        </app-panel>
      }
      @case ('loaded') {
        @if (visible().length === 0) {
          <app-panel>
            <app-empty-state [icon]="tagsIcon" message="Nenhuma categoria cadastrada." />
          </app-panel>
        } @else {
          <div role="tabpanel" [attr.aria-label]="tab() === 'EXPENSE' ? 'Despesas' : 'Receitas'">
            <ul class="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
              @for (category of visible(); track category.id) {
                <li>
                  <app-category-card [category]="category" (edit)="openEdit($event)" (remove)="deleteTarget.set($event)" />
                </li>
              }
            </ul>
          </div>
        }
      }
      @default {
        <p role="status" aria-busy="true" class="py-10 text-center text-sm text-content-muted">Carregando categorias…</p>
      }
    }

    @if (dialogOpen()) {
      <app-category-dialog [category]="editTarget()" (closed)="closeDialog()" />
    }

    @if (deleteTarget(); as target) {
      <app-confirm-dialog
        title="Excluir categoria?"
        [message]="'A categoria “' + target.name + '” e suas subcategorias serão removidas.'"
        confirmLabel="Excluir"
        [busy]="deleting()"
        (confirmed)="confirmDelete(target)"
        (cancelled)="deleteTarget.set(null)"
      />
    }
  `,
})
export class CategoriesPage {
  protected readonly store = inject(CategoriesStore);
  private readonly toast = inject(ToastService);

  protected readonly plus = Plus;
  protected readonly tagsIcon = Tags;
  protected readonly tabs: readonly { value: Tab; label: string }[] = [
    { value: 'EXPENSE', label: 'Despesas' },
    { value: 'INCOME', label: 'Receitas' },
  ];

  protected readonly tab = signal<Tab>('EXPENSE');
  protected readonly dialogOpen = signal(false);
  protected readonly editTarget = signal<Category | null>(null);
  protected readonly deleteTarget = signal<Category | null>(null);
  protected readonly deleting = signal(false);

  protected readonly visible = computed(() =>
    this.tab() === 'EXPENSE' ? this.store.expenseCategories() : this.store.incomeCategories(),
  );

  constructor() {
    this.store.ensureLoaded();
  }

  protected openCreate(): void {
    this.editTarget.set(null);
    this.dialogOpen.set(true);
  }

  protected openEdit(category: Category): void {
    this.editTarget.set(category);
    this.dialogOpen.set(true);
  }

  protected closeDialog(): void {
    this.dialogOpen.set(false);
    this.editTarget.set(null);
  }

  protected confirmDelete(category: Category): void {
    this.deleting.set(true);
    this.store.remove(category.id).subscribe({
      next: () => {
        this.deleting.set(false);
        this.deleteTarget.set(null);
        this.toast.success('Categoria excluída.');
      },
      error: (error: unknown) => {
        this.deleting.set(false);
        this.deleteTarget.set(null);
        this.toast.error(
          isApiError(error) && error.code === 'CATEGORY_IN_USE'
            ? IN_USE_MESSAGE
            : isApiError(error)
              ? error.message
              : 'Não foi possível excluir a categoria.',
        );
      },
    });
  }
}
