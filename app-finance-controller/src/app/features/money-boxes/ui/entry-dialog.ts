import { ChangeDetectionStrategy, Component, ElementRef, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { startWith } from 'rxjs';
import { isApiError } from '../../../core/http/api-error';
import { MoneyInputDirective } from '../../../shared/forms/money-input';
import {
  applyServerErrors,
  decimalValue,
  focusFirstInvalid,
  maxLength,
  positiveAmount,
  requiredValue,
} from '../../../shared/forms/validators';
import { ButtonDirective } from '../../../shared/ui/button';
import { FORM_FIELD_IMPORTS } from '../../../shared/ui/form-field';
import { Modal } from '../../../shared/ui/modal';
import { ToastService } from '../../../shared/ui/toast.service';
import { EntryKind, MoneyBox, MoneyBoxEntry, SaveEntryRequest } from '../data-access/money-boxes-api.service';
import { MoneyBoxesStore } from '../data-access/money-boxes.store';

const MIN_DATE = '2000-01-01';
const MAX_DATE = '2100-12-31';
const INSUFFICIENT = 'Saldo insuficiente: o saque excede o saldo do cofrinho (ou o saldo é zero).';
const GENERIC_ERROR = 'Não foi possível salvar a movimentação. Tente novamente.';

/** Data local de hoje como `YYYY-MM-DD`, sem passar por UTC. */
function today(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
}

/**
 * Depósito ou saque de um cofrinho (criar ou editar). A moeda é a do cofrinho e não é pedida. O servidor decide se o
 * saldo aguenta o saque ou a edição; recusado (`INSUFFICIENT_BALANCE`), nada muda e a mensagem aparece aqui.
 */
@Component({
  selector: 'app-entry-dialog',
  imports: [ReactiveFormsModule, Modal, ButtonDirective, MoneyInputDirective, ...FORM_FIELD_IMPORTS],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-modal [title]="isEdit() ? 'Editar movimento' : 'Movimentar ' + box().name" (closed)="closed.emit()">
      <form class="flex flex-col gap-4" [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <fieldset>
          <legend class="mb-1.5 text-sm font-medium text-content">Tipo</legend>
          <div role="radiogroup" aria-label="Tipo" class="inline-flex overflow-hidden rounded-control border border-line-input">
            @for (option of kinds; track option.value) {
              <button
                type="button"
                role="radio"
                class="min-h-11 px-4 text-sm font-semibold text-content hover:bg-surface-sunken aria-checked:bg-accent aria-checked:text-on-accent"
                [attr.aria-checked]="kind() === option.value"
                (click)="form.controls.kind.setValue(option.value)"
              >
                {{ option.label }}
              </button>
            }
          </div>
        </fieldset>

        <app-form-field label="Valor" [required]="true" [control]="form.controls.amount">
          <input appFieldControl appMoneyInput data-autofocus type="text" formControlName="amount" placeholder="0,00" />
        </app-form-field>

        <app-form-field label="Data" [required]="true" [control]="form.controls.date">
          <input appFieldControl type="date" formControlName="date" [min]="minDate" [max]="maxDate" />
        </app-form-field>

        <app-form-field label="Observação" [control]="form.controls.note">
          <input appFieldControl type="text" formControlName="note" maxlength="255" autocomplete="off" />
        </app-form-field>

        @if (formError(); as message) {
          <p role="alert" class="rounded-control border border-expense bg-expense/10 px-3 py-2 text-sm font-medium text-expense">
            {{ message }}
          </p>
        }

        <div class="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <button type="button" appButton variant="secondary" (click)="closed.emit()">Cancelar</button>
          <button
            type="submit"
            class="inline-flex min-h-11 items-center justify-center rounded-control px-4 text-sm font-semibold text-surface disabled:cursor-not-allowed disabled:opacity-60"
            [class]="kind() === 'DEPOSIT' ? 'bg-income hover:bg-income/90' : 'bg-expense hover:bg-expense/90'"
            [disabled]="submitting()"
          >
            {{ submitting() ? 'Salvando…' : kind() === 'DEPOSIT' ? 'Depositar' : 'Sacar' }}
          </button>
        </div>
      </form>
    </app-modal>
  `,
})
export class EntryDialog implements OnInit {
  readonly box = input.required<MoneyBox>();
  /** Movimentação em edição; ausente para criar. */
  readonly entry = input<MoneyBoxEntry | null>(null);
  /** Tipo inicial ao criar (`Aporte` = depósito, `Saque` = saque). */
  readonly initialKind = input<EntryKind>('DEPOSIT');
  readonly closed = output<void>();

  private readonly store = inject(MoneyBoxesStore);
  private readonly toast = inject(ToastService);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  protected readonly minDate = MIN_DATE;
  protected readonly maxDate = MAX_DATE;
  protected readonly kinds: readonly { value: EntryKind; label: string }[] = [
    { value: 'DEPOSIT', label: 'Depósito' },
    { value: 'WITHDRAWAL', label: 'Saque' },
  ];

  protected readonly submitting = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly isEdit = computed(() => this.entry() !== null);

  protected readonly form = new FormGroup({
    kind: new FormControl<EntryKind>('DEPOSIT', { nonNullable: true }),
    amount: new FormControl('', { nonNullable: true, validators: [requiredValue, decimalValue, positiveAmount] }),
    date: new FormControl(today(), { nonNullable: true, validators: [requiredValue] }),
    note: new FormControl('', { nonNullable: true, validators: [maxLength(255)] }),
  });

  protected readonly kind = toSignal(this.form.controls.kind.valueChanges.pipe(startWith(this.form.controls.kind.value)), {
    requireSync: true,
  });

  ngOnInit(): void {
    const target = this.entry();
    if (target) {
      this.form.controls.kind.setValue(target.kind);
      this.form.controls.amount.setValue(target.amount);
      this.form.controls.date.setValue(target.date);
      this.form.controls.note.setValue(target.note ?? '');
    } else {
      this.form.controls.kind.setValue(this.initialKind());
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
    const request: SaveEntryRequest = {
      kind: raw.kind,
      amount: raw.amount,
      date: raw.date,
      ...(raw.note.trim() !== '' ? { note: raw.note.trim() } : {}),
    };
    const target = this.entry();
    const boxId = this.box().id;
    this.submitting.set(true);
    const call = target
      ? this.store.updateEntry(boxId, target.id, request)
      : this.store.createEntry(boxId, request);
    call.subscribe({
      next: () => {
        this.submitting.set(false);
        this.toast.success(target ? 'Movimentação atualizada.' : raw.kind === 'DEPOSIT' ? 'Aporte registrado.' : 'Saque registrado.');
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
    if (error.code === 'INSUFFICIENT_BALANCE') {
      this.formError.set(INSUFFICIENT);
      return;
    }
    if (error.code === 'VALIDATION_FAILED') {
      const unmatched = applyServerErrors(this.form, error.fieldErrors);
      this.formError.set(unmatched[0]?.message ?? null);
      setTimeout(() => focusFirstInvalid(this.host.nativeElement));
      return;
    }
    this.formError.set(error.message || GENERIC_ERROR);
  }
}
