import { ChangeDetectionStrategy, Component, ElementRef, OnInit, inject, input, output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { isApiError } from '../../../core/http/api-error';
import { MoneyInputDirective } from '../../../shared/forms/money-input';
import { applyServerErrors, decimalValue, focusFirstInvalid } from '../../../shared/forms/validators';
import { ButtonDirective } from '../../../shared/ui/button';
import { FORM_FIELD_IMPORTS } from '../../../shared/ui/form-field';
import { Modal } from '../../../shared/ui/modal';
import { ToastService } from '../../../shared/ui/toast.service';
import { MoneyBoxesStore } from '../data-access/money-boxes.store';

const GENERIC_ERROR = 'Não foi possível salvar a meta geral. Tente novamente.';

/** Meta geral de reservas da moeda selecionada; vazio (ou 0) remove a meta. */
@Component({
  selector: 'app-goal-dialog',
  imports: [ReactiveFormsModule, Modal, ButtonDirective, MoneyInputDirective, ...FORM_FIELD_IMPORTS],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-modal title="Meta geral de reservas" (closed)="closed.emit()">
      <form class="flex flex-col gap-4" [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <p class="text-sm text-content-muted">
          O alvo total que você quer guardar somando todos os cofrinhos. Deixe vazio para não usar meta geral.
        </p>

        <app-form-field label="Meta geral" [control]="form.controls.goal">
          <input appFieldControl appMoneyInput data-autofocus type="text" formControlName="goal" placeholder="0,00" />
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
export class GoalDialog implements OnInit {
  /** Meta atual (texto decimal); ausente sem meta. */
  readonly current = input<string | undefined>(undefined);
  readonly closed = output<void>();

  private readonly store = inject(MoneyBoxesStore);
  private readonly toast = inject(ToastService);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  protected readonly submitting = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly form = new FormGroup({
    goal: new FormControl('', { nonNullable: true, validators: [decimalValue] }),
  });

  ngOnInit(): void {
    this.form.controls.goal.setValue(this.current() ?? '');
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
    const amount = this.form.getRawValue().goal;
    this.submitting.set(true);
    this.store.saveGoal(amount === '' ? '0' : amount).subscribe({
      next: () => {
        this.submitting.set(false);
        this.toast.success('Meta geral salva.');
        this.closed.emit();
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        if (isApiError(error) && error.code === 'VALIDATION_FAILED') {
          const unmatched = applyServerErrors(this.form, error.fieldErrors);
          this.formError.set(unmatched[0]?.message ?? null);
          return;
        }
        this.formError.set(isApiError(error) ? error.message || GENERIC_ERROR : GENERIC_ERROR);
      },
    });
  }
}
