import { ChangeDetectionStrategy, Component, ElementRef, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { LucideAngularModule, Lock, Plus, X } from 'lucide-angular';
import { isApiError } from '../../../core/http/api-error';
import { applyServerErrors, focusFirstInvalid, maxLength, requiredText } from '../../../shared/forms/validators';
import { ButtonDirective } from '../../../shared/ui/button';
import { ConfirmDialog } from '../../../shared/ui/confirm-dialog';
import { FORM_FIELD_IMPORTS } from '../../../shared/ui/form-field';
import { Modal } from '../../../shared/ui/modal';
import { ToastService } from '../../../shared/ui/toast.service';
import { CATEGORY_COLORS, CATEGORY_ICONS, TYPE_LABELS } from '../category-catalog';
import { Category, CategoryType, Subcategory } from '../data-access/categories-api.service';
import { CategoriesStore } from '../data-access/categories.store';

const OTHERS = 'Outros';
const NAME_DUPLICATED = 'Já existe uma categoria com este nome.';
const TYPE_CONFLICT = 'Existem lançamentos incompatíveis com o novo tipo desta categoria.';
const SUBCATEGORY_DUPLICATED = 'Já existe uma subcategoria com este nome.';
const GENERIC_ERROR = 'Não foi possível salvar a categoria. Tente novamente.';

/**
 * Criação e edição de categoria. Na criação as subcategorias são montadas localmente e enviadas ao salvar; na edição
 * adicionar/remover grava imediatamente. Fica no DOM só enquanto aberto (`@if` do chamador).
 */
@Component({
  selector: 'app-category-dialog',
  imports: [ReactiveFormsModule, Modal, ConfirmDialog, ButtonDirective, LucideAngularModule, ...FORM_FIELD_IMPORTS],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-modal [title]="isEdit() ? 'Editar Categoria' : 'Nova Categoria'" (closed)="closed.emit()">
      <form class="flex flex-col gap-4" [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <app-form-field label="Nome" [required]="true" [control]="form.controls.name">
          <input appFieldControl data-autofocus type="text" formControlName="name" maxlength="60" autocomplete="off" />
        </app-form-field>

        <app-form-field label="Tipo" [required]="true" [control]="form.controls.type">
          <select appFieldControl formControlName="type">
            @for (option of typeOptions; track option.value) {
              <option [value]="option.value">{{ option.label }}</option>
            }
          </select>
        </app-form-field>

        <fieldset class="flex flex-col gap-1.5">
          <legend class="mb-1.5 text-sm font-medium text-content">Ícone</legend>
          <div role="radiogroup" aria-label="Ícone" class="grid grid-cols-6 gap-1.5 sm:grid-cols-8">
            @for (option of icons; track option.key) {
              <button
                type="button"
                role="radio"
                class="flex min-h-11 items-center justify-center rounded-control border text-content aria-checked:border-accent aria-checked:bg-accent/15 aria-checked:text-accent border-line-input hover:bg-surface-sunken"
                [attr.aria-checked]="form.controls.icon.value === option.key"
                [attr.aria-label]="option.label"
                (click)="form.controls.icon.setValue(option.key)"
              >
                <lucide-icon [img]="option.icon" [size]="20" aria-hidden="true" />
              </button>
            }
          </div>
        </fieldset>

        <fieldset class="flex flex-col gap-1.5">
          <legend class="mb-1.5 text-sm font-medium text-content">Cor</legend>
          <div role="radiogroup" aria-label="Cor" class="grid grid-cols-6 gap-2">
            @for (option of colors; track option.hex) {
              <button
                type="button"
                role="radio"
                class="flex min-h-11 items-center justify-center rounded-control border-2 border-transparent aria-checked:border-content"
                [attr.aria-checked]="form.controls.color.value === option.hex"
                [attr.aria-label]="option.label"
                (click)="form.controls.color.setValue(option.hex)"
              >
                <span class="block size-7 rounded-full" [style.background-color]="option.hex"></span>
              </button>
            }
          </div>
        </fieldset>

        <fieldset class="flex flex-col gap-2">
          <legend class="mb-1 text-sm font-medium text-content">Subcategorias</legend>
          <ul class="flex flex-wrap gap-1.5" aria-label="Subcategorias da categoria">
            @for (sub of subcategoryList(); track sub.id) {
              <li class="inline-flex items-center gap-1 rounded-full bg-surface-sunken py-0.5 pl-2.5 pr-1 text-xs text-content">
                {{ sub.name }}
                @if (sub.protected) {
                  <span class="inline-flex size-6 items-center justify-center text-content-muted" title="Protegida">
                    <lucide-icon [img]="lock" [size]="14" aria-hidden="true" />
                    <span class="sr-only">Protegida</span>
                  </span>
                } @else {
                  <button
                    type="button"
                    class="inline-flex size-6 items-center justify-center rounded-full text-content-muted hover:text-expense"
                    [attr.aria-label]="'Remover ' + sub.name"
                    (click)="askRemove(sub)"
                  >
                    <lucide-icon [img]="close" [size]="14" aria-hidden="true" />
                  </button>
                }
              </li>
            }
          </ul>
          <div class="flex gap-2">
            <input
              type="text"
              class="min-h-11 min-w-0 flex-1 rounded-control border border-line-input bg-surface-sunken px-3 text-content placeholder:text-content-muted"
              aria-label="Nova subcategoria"
              placeholder="Nova subcategoria"
              maxlength="60"
              autocomplete="off"
              [formControl]="newSubcategory"
              (keydown.enter)="$event.preventDefault(); addSubcategory()"
            />
            <button type="button" appButton variant="secondary" aria-label="Adicionar subcategoria" [disabled]="busy()" (click)="addSubcategory()">
              <lucide-icon [img]="plus" [size]="18" aria-hidden="true" />
            </button>
          </div>
          @if (subcategoryError(); as message) {
            <p role="alert" class="text-xs font-medium text-expense">{{ message }}</p>
          }
        </fieldset>

        @if (formError(); as message) {
          <p role="alert" class="rounded-control border border-expense bg-expense/10 px-3 py-2 text-sm font-medium text-expense">
            {{ message }}
          </p>
        }

        <div class="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <button type="button" appButton variant="secondary" (click)="closed.emit()">Cancelar</button>
          <button type="submit" appButton [disabled]="submitting()">{{ submitting() ? 'Salvando…' : 'Salvar' }}</button>
        </div>
      </form>
    </app-modal>

    @if (removing(); as sub) {
      <app-confirm-dialog
        title="Remover subcategoria?"
        [message]="'Os lançamentos que usam “' + sub.name + '” ficarão sem subcategoria.'"
        confirmLabel="Remover"
        [busy]="busy()"
        (confirmed)="removeConfirmed(sub)"
        (cancelled)="removing.set(null)"
      />
    }
  `,
})
export class CategoryDialog implements OnInit {
  /** Categoria em edição; ausente para criar. */
  readonly category = input<Category | null>(null);
  readonly closed = output<void>();

  private readonly store = inject(CategoriesStore);
  private readonly toast = inject(ToastService);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  protected readonly icons = CATEGORY_ICONS;
  protected readonly colors = CATEGORY_COLORS;
  protected readonly typeOptions = (Object.keys(TYPE_LABELS) as (keyof typeof TYPE_LABELS)[]).map((value) => ({
    value,
    label: TYPE_LABELS[value],
  }));
  protected readonly lock = Lock;
  protected readonly plus = Plus;
  protected readonly close = X;

  protected readonly submitting = signal(false);
  protected readonly busy = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly subcategoryError = signal<string | null>(null);
  protected readonly removing = signal<Subcategory | null>(null);
  private readonly pending = signal<readonly string[]>([]);

  protected readonly isEdit = computed(() => this.category() !== null);
  /** Edição: sempre a versão mais recente do store (adicionar/remover atualiza na hora). */
  private readonly current = computed(() => {
    const target = this.category();
    return target ? (this.store.byId(target.id) ?? target) : null;
  });
  protected readonly subcategoryList = computed<readonly Subcategory[]>(() => {
    const current = this.current();
    if (current) {
      return current.subcategories;
    }
    return [
      { id: 'others', name: OTHERS, protected: true },
      ...this.pending().map((name) => ({ id: `new-${name}`, name, protected: false })),
    ];
  });

  protected readonly newSubcategory = new FormControl('', { nonNullable: true });
  protected readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [requiredText, maxLength(60)] }),
    type: new FormControl<CategoryType>('EXPENSE', { nonNullable: true }),
    icon: new FormControl(CATEGORY_ICONS[0].key, { nonNullable: true }),
    color: new FormControl(CATEGORY_COLORS[0].hex, { nonNullable: true }),
  });

  ngOnInit(): void {
    const target = this.category();
    if (target) {
      this.form.setValue({ name: target.name, type: target.type, icon: target.icon, color: target.color });
    }
  }

  protected submit(): void {
    if (this.submitting()) {
      return;
    }
    this.formError.set(null);
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      focusFirstInvalid(this.host.nativeElement);
      return;
    }
    const raw = this.form.getRawValue();
    const base = { name: raw.name.trim(), type: raw.type, icon: raw.icon, color: raw.color };
    const target = this.category();
    this.submitting.set(true);
    const call = target
      ? this.store.update(target.id, base)
      : this.store.create({ ...base, subcategories: [...this.pending()] });
    call.subscribe({
      next: () => {
        this.submitting.set(false);
        this.toast.success('Categoria salva.');
        this.closed.emit();
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        this.handleSaveError(error);
      },
    });
  }

  protected addSubcategory(): void {
    const name = this.newSubcategory.value.trim();
    this.subcategoryError.set(null);
    if (name === '') {
      return;
    }
    const key = name.toLowerCase();
    if (this.subcategoryList().some((sub) => sub.name.trim().toLowerCase() === key)) {
      this.subcategoryError.set(SUBCATEGORY_DUPLICATED);
      return;
    }
    const target = this.category();
    if (!target) {
      this.pending.update((list) => [...list, name]);
      this.newSubcategory.reset('');
      return;
    }
    this.busy.set(true);
    this.store.addSubcategory(target.id, name).subscribe({
      next: () => {
        this.busy.set(false);
        this.newSubcategory.reset('');
      },
      error: (error: unknown) => {
        this.busy.set(false);
        this.subcategoryError.set(
          isApiError(error) && error.code === 'SUBCATEGORY_NAME_DUPLICATED'
            ? SUBCATEGORY_DUPLICATED
            : 'Não foi possível adicionar a subcategoria.',
        );
      },
    });
  }

  protected askRemove(sub: Subcategory): void {
    if (this.category() === null) {
      this.pending.update((list) => list.filter((name) => name !== sub.name));
      return;
    }
    this.removing.set(sub);
  }

  protected removeConfirmed(sub: Subcategory): void {
    const target = this.category();
    if (!target) {
      return;
    }
    this.busy.set(true);
    this.store.removeSubcategory(target.id, sub.id).subscribe({
      next: () => {
        this.busy.set(false);
        this.removing.set(null);
      },
      error: (error: unknown) => {
        this.busy.set(false);
        this.removing.set(null);
        this.toast.error(isApiError(error) ? error.message : 'Não foi possível remover a subcategoria.');
      },
    });
  }

  private handleSaveError(error: unknown): void {
    if (!isApiError(error)) {
      this.formError.set(GENERIC_ERROR);
      return;
    }
    switch (error.code) {
      case 'CATEGORY_NAME_DUPLICATED':
        this.form.controls.name.setErrors({ server: { message: NAME_DUPLICATED } });
        this.form.controls.name.markAsTouched();
        break;
      case 'CATEGORY_TYPE_CONFLICT':
        this.form.controls.type.setErrors({ server: { message: TYPE_CONFLICT } });
        this.form.controls.type.markAsTouched();
        break;
      case 'VALIDATION_FAILED': {
        const unmatched = applyServerErrors(this.form, error.fieldErrors);
        this.formError.set(unmatched[0]?.message ?? null);
        break;
      }
      default:
        this.formError.set(error.message || GENERIC_ERROR);
    }
    setTimeout(() => focusFirstInvalid(this.host.nativeElement));
  }
}
