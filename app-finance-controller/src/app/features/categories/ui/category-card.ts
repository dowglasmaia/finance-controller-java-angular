import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { LucideAngularModule, Pencil, Trash2 } from 'lucide-angular';
import { Category } from '../data-access/categories-api.service';
import { iconFor } from '../category-catalog';

const VISIBLE_CHIPS = 3;

/** Cartão de uma categoria: ícone na cor da categoria, nome, até 3 subcategorias (+N) e ações. */
@Component({
  selector: 'app-category-card',
  imports: [LucideAngularModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <article class="flex h-full flex-col gap-3 rounded-card border border-line bg-surface-raised p-4 shadow-card">
      <div class="flex items-center gap-3">
        <span
          class="flex size-11 shrink-0 items-center justify-center rounded-control"
          [style.background-color]="category().color + '26'"
          [style.color]="category().color"
          aria-hidden="true"
        >
          <lucide-icon [img]="icon()" [size]="22" />
        </span>
        <h3 class="min-w-0 flex-1 break-words text-base font-semibold text-content">{{ category().name }}</h3>
      </div>

      <ul class="flex flex-wrap gap-1.5" aria-label="Subcategorias">
        @for (sub of visible(); track sub.id) {
          <li class="rounded-full bg-surface-sunken px-2.5 py-0.5 text-xs text-content-muted">{{ sub.name }}</li>
        }
        @if (hidden() > 0) {
          <li class="rounded-full bg-surface-sunken px-2.5 py-0.5 text-xs font-medium text-content">+{{ hidden() }}</li>
        }
      </ul>

      <div class="mt-auto flex justify-end gap-1">
        <button
          type="button"
          class="inline-flex min-h-11 min-w-11 items-center justify-center rounded-control text-content-muted hover:bg-surface-sunken hover:text-content"
          [attr.aria-label]="'Editar ' + category().name"
          (click)="edit.emit(category())"
        >
          <lucide-icon [img]="pencil" [size]="18" aria-hidden="true" />
        </button>
        <button
          type="button"
          class="inline-flex min-h-11 min-w-11 items-center justify-center rounded-control text-content-muted hover:bg-surface-sunken hover:text-expense"
          [attr.aria-label]="'Excluir ' + category().name"
          (click)="remove.emit(category())"
        >
          <lucide-icon [img]="trash" [size]="18" aria-hidden="true" />
        </button>
      </div>
    </article>
  `,
})
export class CategoryCard {
  readonly category = input.required<Category>();
  readonly edit = output<Category>();
  readonly remove = output<Category>();

  protected readonly pencil = Pencil;
  protected readonly trash = Trash2;
  protected readonly icon = computed(() => iconFor(this.category().icon));
  protected readonly visible = computed(() => this.category().subcategories.slice(0, VISIBLE_CHIPS));
  protected readonly hidden = computed(() => Math.max(0, this.category().subcategories.length - VISIBLE_CHIPS));
}
