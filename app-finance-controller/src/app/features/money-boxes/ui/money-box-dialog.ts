import { ChangeDetectionStrategy, Component, ElementRef, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { isApiError } from '../../../core/http/api-error';
import { CurrencyStore } from '../../../core/state/currency.store';
import { MoneyInputDirective } from '../../../shared/forms/money-input';
import { applyServerErrors, decimalValue, focusFirstInvalid, maxLength, requiredText } from '../../../shared/forms/validators';
import { ButtonDirective } from '../../../shared/ui/button';
import { FORM_FIELD_IMPORTS } from '../../../shared/ui/form-field';
import { Modal } from '../../../shared/ui/modal';
import { ToastService } from '../../../shared/ui/toast.service';
import { MONEY_BOX_COLORS, MONEY_BOX_ICONS } from '../money-box-catalog';
import { CurrencyCode, MoneyBox, MoneyBoxesApiService, SaveMoneyBoxRequest } from '../data-access/money-boxes-api.service';
import { MoneyBoxesStore } from '../data-access/money-boxes.store';

const CURRENCY_LOCKED = 'Não é possível alterar a moeda de um cofrinho com aportes.';
const GENERIC_ERROR = 'Não foi possível salvar o cofrinho. Tente novamente.';

/** Criação e edição de cofrinho. Na edição a moeda fica desabilitada se já há aportes. Fica no DOM só enquanto aberto. */
@Component({
  selector: 'app-money-box-dialog',
  imports: [ReactiveFormsModule, Modal, ButtonDirective, MoneyInputDirective, LucideAngularModule, ...FORM_FIELD_IMPORTS],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-modal [title]="isEdit() ? 'Editar Cofrinho' : 'Novo Cofrinho'" (closed)="closed.emit()">
      <form class="flex flex-col gap-4" [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <app-form-field label="Nome" [required]="true" [control]="form.controls.name">
          <input appFieldControl data-autofocus type="text" formControlName="name" maxlength="100" autocomplete="off" />
        </app-form-field>

        <app-form-field label="Onde está guardado (opcional)" [control]="form.controls.description">
          <input appFieldControl type="text" formControlName="description" maxlength="255" autocomplete="off" />
        </app-form-field>

        <div class="grid gap-4 sm:grid-cols-2">
          <app-form-field label="Objetivo (opcional)" [control]="form.controls.goal">
            <input appFieldControl appMoneyInput type="text" formControlName="goal" placeholder="0,00" />
          </app-form-field>
          <app-form-field
            label="Moeda"
            [required]="true"
            [help]="currencyLocked() ? lockedMessage : ''"
          >
            <select appFieldControl formControlName="currency">
              <option value="BRL">Real (BRL)</option>
              <option value="EUR">Euro (EUR)</option>
            </select>
          </app-form-field>
        </div>

        <fieldset class="flex flex-col gap-1.5">
          <legend class="mb-1.5 text-sm font-medium text-content">Ícone</legend>
          <div role="radiogroup" aria-label="Ícone" class="grid grid-cols-5 gap-1.5">
            @for (option of icons; track option.key) {
              <button
                type="button"
                role="radio"
                class="flex min-h-11 items-center justify-center rounded-control border border-line-input text-content hover:bg-surface-sunken aria-checked:border-accent aria-checked:bg-accent/15 aria-checked:text-accent"
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
export class MoneyBoxDialog implements OnInit {
  /** Cofrinho em edição; ausente para criar. */
  readonly box = input<MoneyBox | null>(null);
  readonly closed = output<void>();

  private readonly store = inject(MoneyBoxesStore);
  private readonly api = inject(MoneyBoxesApiService);
  private readonly currencyStore = inject(CurrencyStore);
  private readonly toast = inject(ToastService);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  protected readonly icons = MONEY_BOX_ICONS;
  protected readonly colors = MONEY_BOX_COLORS;
  protected readonly lockedMessage = CURRENCY_LOCKED;

  protected readonly submitting = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly isEdit = computed(() => this.box() !== null);
  /** Um cofrinho com aportes não pode mudar de moeda (RN-BOX-04). */
  protected readonly currencyLocked = signal(false);

  protected readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [requiredText, maxLength(100)] }),
    description: new FormControl('', { nonNullable: true, validators: [maxLength(255)] }),
    goal: new FormControl('', { nonNullable: true, validators: [decimalValue] }),
    currency: new FormControl<CurrencyCode>(this.currencyStore.selectedCurrency(), { nonNullable: true }),
    icon: new FormControl(MONEY_BOX_ICONS[0].key, { nonNullable: true }),
    color: new FormControl(MONEY_BOX_COLORS[0].hex, { nonNullable: true }),
  });

  ngOnInit(): void {
    const target = this.box();
    if (!target) {
      return;
    }
    this.form.patchValue({
      name: target.name,
      description: target.description ?? '',
      currency: target.currency,
      icon: target.icon,
      color: target.color,
    });
    this.form.controls.goal.setValue(target.goal ?? '');
    this.lockCurrencyWhileUnknown(target);
  }

  /** Enquanto não se sabe se há aportes a moeda fica travada; sem aportes ela é liberada. */
  private lockCurrencyWhileUnknown(target: MoneyBox): void {
    this.setCurrencyLocked(true);
    this.api.listMoneyBoxEntries(target.id).subscribe({
      next: (entries) => this.setCurrencyLocked(entries.length > 0),
      error: () => this.setCurrencyLocked(false),
    });
  }

  private setCurrencyLocked(locked: boolean): void {
    this.currencyLocked.set(locked);
    if (locked) {
      this.form.controls.currency.disable({ emitEvent: false });
    } else {
      this.form.controls.currency.enable({ emitEvent: false });
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
    const request: SaveMoneyBoxRequest = {
      name: raw.name.trim(),
      icon: raw.icon,
      color: raw.color,
      currency: raw.currency,
      ...(raw.description.trim() !== '' ? { description: raw.description.trim() } : {}),
      ...(raw.goal !== '' ? { goal: raw.goal } : {}),
    };
    const target = this.box();
    this.submitting.set(true);
    const call = target ? this.store.updateBox(target.id, request) : this.store.createBox(request);
    call.subscribe({
      next: (saved) => {
        this.submitting.set(false);
        this.toast.success('Cofrinho salvo.');
        // cofrinho em outra moeda: a interface passa a mostrar essa moeda
        if (saved.currency !== this.currencyStore.selectedCurrency()) {
          this.currencyStore.select(saved.currency);
        }
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
    if (error.code === 'CURRENCY_LOCKED') {
      this.formError.set(CURRENCY_LOCKED);
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
