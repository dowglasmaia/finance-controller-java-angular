import { ChangeDetectionStrategy, Component, ElementRef, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { map, startWith } from 'rxjs';
import { isApiError } from '../../../core/http/api-error';
import { CurrencyStore } from '../../../core/state/currency.store';
import { ReferenceMonthStore } from '../../../core/state/reference-month.store';
import {
  applyServerErrors,
  decimalValue,
  focusFirstInvalid,
  maxLength,
  positiveAmount,
  requiredCategory,
  requiredText,
  requiredValue,
} from '../../../shared/forms/validators';
import { MoneyInputDirective } from '../../../shared/forms/money-input';
import { ButtonDirective } from '../../../shared/ui/button';
import { FORM_FIELD_IMPORTS } from '../../../shared/ui/form-field';
import { Modal } from '../../../shared/ui/modal';
import { ToastService } from '../../../shared/ui/toast.service';
import { CategoriesStore } from '../../categories/data-access/categories.store';
import { budgetAlertToast } from '../budget-alert-message';
import {
  CurrencyCode,
  Transaction,
  TransactionKind,
  TransactionRequest,
  TransactionsApiService,
} from '../data-access/transactions-api.service';

const MIN_DATE = '2000-01-01';
const MAX_DATE = '2100-12-31';
const TYPE_MISMATCH = 'A categoria não aceita este tipo de lançamento.';
const SUBCATEGORY_MISMATCH = 'A subcategoria não pertence à categoria escolhida.';
const GENERIC_ERROR = 'Não foi possível salvar o lançamento. Tente novamente.';

/** Data local de hoje como `YYYY-MM-DD`, sem passar por UTC. */
function today(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
}

/** Criação e edição de lançamento. Fica no DOM só enquanto aberto (`@if` do chamador). */
@Component({
  selector: 'app-transaction-dialog',
  imports: [ReactiveFormsModule, Modal, ButtonDirective, MoneyInputDirective, ...FORM_FIELD_IMPORTS],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-modal [title]="isEdit() ? 'Editar Lançamento' : 'Novo Lançamento'" (closed)="closed.emit()">
      <form class="flex flex-col gap-4" [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <fieldset>
          <legend class="mb-1.5 text-sm font-medium text-content">Tipo</legend>
          <div role="radiogroup" aria-label="Tipo" class="inline-flex overflow-hidden rounded-control border border-line-input">
            @for (option of kinds; track option.value) {
              <button
                type="button"
                role="radio"
                class="min-h-11 px-4 text-sm font-semibold text-content hover:bg-surface-sunken aria-checked:bg-accent aria-checked:text-on-accent"
                [attr.aria-checked]="form.controls.kind.value === option.value"
                (click)="form.controls.kind.setValue(option.value)"
              >
                {{ option.label }}
              </button>
            }
          </div>
        </fieldset>

        <app-form-field label="Descrição" [required]="true" [control]="form.controls.description">
          <input appFieldControl data-autofocus type="text" formControlName="description" maxlength="120" autocomplete="off" />
        </app-form-field>

        <div class="grid gap-4 sm:grid-cols-2">
          <app-form-field label="Valor" [required]="true" [control]="form.controls.amount">
            <input appFieldControl appMoneyInput type="text" formControlName="amount" placeholder="0,00" />
          </app-form-field>
          <app-form-field label="Moeda" [required]="true">
            <select appFieldControl formControlName="currency">
              <option value="BRL">Real (BRL)</option>
              <option value="EUR">Euro (EUR)</option>
            </select>
          </app-form-field>
        </div>

        <app-form-field label="Data" [required]="true" [control]="form.controls.date">
          <input appFieldControl type="date" formControlName="date" [min]="minDate" [max]="maxDate" />
        </app-form-field>

        <div class="grid gap-4 sm:grid-cols-2">
          <app-form-field label="Categoria" [required]="true" [control]="form.controls.categoryId">
            <select appFieldControl formControlName="categoryId">
              <option value="">Selecione…</option>
              @for (category of categoryOptions(); track category.id) {
                <option [value]="category.id">{{ category.name }}</option>
              }
            </select>
          </app-form-field>
          <app-form-field label="Subcategoria" [control]="form.controls.subcategoryId">
            <select appFieldControl formControlName="subcategoryId">
              <option value="">Nenhuma</option>
              @for (sub of subcategoryOptions(); track sub.id) {
                <option [value]="sub.id">{{ sub.name }}</option>
              }
            </select>
          </app-form-field>
        </div>

        <app-form-field label="Observações" [control]="form.controls.notes">
          <textarea appFieldControl formControlName="notes" rows="2" maxlength="500" class="py-2"></textarea>
        </app-form-field>

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
  `,
})
export class TransactionDialog implements OnInit {
  /** Lançamento em edição; ausente para criar. */
  readonly transaction = input<Transaction | null>(null);
  /** Emitido depois de salvar (a lista se recarrega). */
  readonly saved = output<void>();
  readonly closed = output<void>();

  private readonly api = inject(TransactionsApiService);
  private readonly categories = inject(CategoriesStore);
  private readonly month = inject(ReferenceMonthStore);
  private readonly currency = inject(CurrencyStore);
  private readonly toast = inject(ToastService);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  protected readonly minDate = MIN_DATE;
  protected readonly maxDate = MAX_DATE;
  protected readonly kinds: readonly { value: TransactionKind; label: string }[] = [
    { value: 'EXPENSE', label: 'Saída' },
    { value: 'INCOME', label: 'Entrada' },
  ];

  protected readonly submitting = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly isEdit = computed(() => this.transaction() !== null);

  protected readonly form = new FormGroup({
    kind: new FormControl<TransactionKind>('EXPENSE', { nonNullable: true }),
    description: new FormControl('', { nonNullable: true, validators: [requiredText, maxLength(120)] }),
    amount: new FormControl('', { nonNullable: true, validators: [requiredValue, decimalValue, positiveAmount] }),
    currency: new FormControl<CurrencyCode>(this.currency.selectedCurrency(), { nonNullable: true }),
    date: new FormControl(today(), { nonNullable: true, validators: [requiredValue] }),
    categoryId: new FormControl('', { nonNullable: true, validators: [requiredCategory] }),
    subcategoryId: new FormControl('', { nonNullable: true }),
    notes: new FormControl('', { nonNullable: true, validators: [maxLength(500)] }),
  });

  private readonly kindValue = toSignal(
    this.form.controls.kind.valueChanges.pipe(startWith(this.form.controls.kind.value)),
    { requireSync: true },
  );
  private readonly categoryValue = toSignal(
    this.form.controls.categoryId.valueChanges.pipe(startWith(this.form.controls.categoryId.value)),
    { requireSync: true },
  );

  /** Categorias que aceitam o tipo escolhido (`Ambos` aparece nos dois). */
  protected readonly categoryOptions = computed(() =>
    this.kindValue() === 'EXPENSE' ? this.categories.expenseCategories() : this.categories.incomeCategories(),
  );
  protected readonly subcategoryOptions = computed(
    () => this.categories.byId(this.categoryValue())?.subcategories ?? [],
  );

  constructor() {
    this.categories.ensureLoaded();
    // trocar o tipo ou a categoria invalida as escolhas dependentes
    this.form.controls.kind.valueChanges.pipe(map(() => this.categoryOptions())).subscribe((options) => {
      if (!options.some((option) => option.id === this.form.controls.categoryId.value)) {
        this.form.controls.categoryId.setValue('');
      }
    });
    this.form.controls.categoryId.valueChanges.subscribe(() => this.form.controls.subcategoryId.setValue(''));
  }

  ngOnInit(): void {
    const target = this.transaction();
    if (target) {
      this.form.patchValue({ kind: target.kind, currency: target.currency, date: target.date, description: target.description });
      this.form.controls.amount.setValue(target.amount);
      this.form.controls.categoryId.setValue(target.category.id);
      this.form.controls.subcategoryId.setValue(target.subcategory?.id ?? '');
      this.form.controls.notes.setValue(target.notes ?? '');
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
    const request: TransactionRequest = {
      kind: raw.kind,
      description: raw.description.trim(),
      amount: raw.amount,
      currency: raw.currency,
      date: raw.date,
      categoryId: raw.categoryId,
      referenceMonth: this.month.monthKey(),
      ...(raw.subcategoryId !== '' ? { subcategoryId: raw.subcategoryId } : {}),
      ...(raw.notes.trim() !== '' ? { notes: raw.notes.trim() } : {}),
    };
    const target = this.transaction();
    this.submitting.set(true);
    const call = target ? this.api.updateTransaction(target.id, request) : this.api.createTransaction(request);
    call.subscribe({
      next: (result) => {
        this.submitting.set(false);
        this.toast.success('Lançamento salvo.');
        if (result.budgetAlert) {
          const alert = budgetAlertToast(result.budgetAlert);
          this.toast.show(alert.kind, alert.message);
        }
        // lançamento noutra moeda: a interface passa a mostrar essa moeda (P-15)
        if (result.transaction.currency !== this.currency.selectedCurrency()) {
          this.currency.select(result.transaction.currency);
        }
        this.saved.emit();
        this.closed.emit();
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        this.handleError(error);
      },
    });
  }

  private handleError(error: unknown): void {
    if (!isApiError(error)) {
      this.formError.set(GENERIC_ERROR);
      return;
    }
    switch (error.code) {
      case 'CATEGORY_TYPE_MISMATCH':
        this.form.controls.categoryId.setErrors({ server: { message: TYPE_MISMATCH } });
        this.form.controls.categoryId.markAsTouched();
        break;
      case 'SUBCATEGORY_NOT_IN_CATEGORY':
        this.form.controls.subcategoryId.setErrors({ server: { message: SUBCATEGORY_MISMATCH } });
        this.form.controls.subcategoryId.markAsTouched();
        break;
      case 'RESOURCE_NOT_FOUND':
        // categoria removida em outra aba: recarrega os seletores
        this.categories.load();
        this.form.controls.categoryId.setErrors({ server: { message: 'Selecione uma categoria' } });
        this.form.controls.categoryId.markAsTouched();
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
