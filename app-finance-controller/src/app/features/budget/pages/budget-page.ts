import { ChangeDetectionStrategy, Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { FormControl, FormGroup, FormRecord, ReactiveFormsModule } from '@angular/forms';
import { isApiError } from '../../../core/http/api-error';
import { CurrencyStore } from '../../../core/state/currency.store';
import { ReferenceMonthStore } from '../../../core/state/reference-month.store';
import { decimalValue } from '../../../shared/forms/validators';
import { MoneyInputDirective } from '../../../shared/forms/money-input';
import { AppPanel } from '../../../shared/ui/app-panel';
import { ButtonDirective } from '../../../shared/ui/button';
import { EmptyState } from '../../../shared/ui/empty-state';
import { FORM_FIELD_IMPORTS } from '../../../shared/ui/form-field';
import { PageHeader } from '../../../shared/ui/page-header';
import { ToastService } from '../../../shared/ui/toast.service';
import { CategoriesStore } from '../../categories/data-access/categories.store';
import { SaveBudgetRequest } from '../data-access/budget-api.service';
import { BudgetStore } from '../data-access/budget.store';
import { BudgetProgressCard } from '../ui/budget-progress-card';
import { CategoryLimitRow } from '../ui/category-limit-row';

const GENERIC_ERROR = 'Não foi possível salvar a meta. Tente novamente.';

/** Texto do servidor (`1000.00`) → texto do campo; ausente → vazio. */
function asField(value: string | undefined): string {
  return value ?? '';
}

/**
 * `/metas`: progresso do mês, meta padrão recorrente, ajuste do mês (valor ou "sem meta neste mês") e metas por
 * categoria, gravados juntos. Campo vazio remove; mês ou moeda diferentes recarregam todos os campos.
 */
@Component({
  selector: 'app-budget-page',
  imports: [
    ReactiveFormsModule,
    PageHeader,
    AppPanel,
    EmptyState,
    ButtonDirective,
    BudgetProgressCard,
    CategoryLimitRow,
    MoneyInputDirective,
    ...FORM_FIELD_IMPORTS,
  ],
  providers: [BudgetStore],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-page-header title="Meta de Gastos" subtitle="Defina tetos mensais para controlar seus gastos" />

    @switch (store.status()) {
      @case ('error') {
        <app-panel>
          <app-empty-state message="Não foi possível carregar a meta.">
            <button type="button" appButton variant="secondary" (click)="store.refresh()">Tentar novamente</button>
          </app-empty-state>
        </app-panel>
      }
      @case ('loaded') {
        <div class="flex flex-col gap-4">
          @if (store.data(); as data) {
            @if (data.progress.global.limit) {
              <app-budget-progress-card [entry]="data.progress.global" [currency]="currency.selectedCurrency()" [monthLabel]="month.label()" />
            }
          }

          <form class="flex flex-col gap-4" [formGroup]="form" (ngSubmit)="save()" novalidate>
            <app-panel title="Meta mensal padrão">
              <app-form-field
                label="Meta padrão"
                help="Teto recorrente aplicado a todo mês. Deixe vazio para não usar meta global."
                [control]="form.controls.monthlyLimit"
              >
                <input appFieldControl appMoneyInput type="text" formControlName="monthlyLimit" placeholder="Sem meta" />
              </app-form-field>
            </app-panel>

            <app-panel [title]="'Ajuste para ' + month.label()">
              <div class="flex flex-col gap-3">
                <app-form-field
                  label="Meta deste mês"
                  help="Opcional: um teto específico para este mês (sobrepõe a meta padrão)."
                  [control]="form.controls.overrideLimit"
                >
                  <input appFieldControl appMoneyInput type="text" formControlName="overrideLimit" placeholder="Usar a meta padrão" />
                </app-form-field>
                <label class="inline-flex min-h-11 items-center gap-2 text-sm text-content">
                  <input type="checkbox" class="size-5 accent-[var(--color-accent)]" formControlName="overrideDisabled" />
                  Sem meta neste mês
                </label>
              </div>
            </app-panel>

            <app-panel title="Metas por categoria">
              <p class="mb-2 text-sm text-content-muted">
                Opcional: teto recorrente por categoria. Gasto exibido é o do mês selecionado.
              </p>
              <ul class="divide-y divide-line">
                @for (row of rows(); track row.id) {
                  <li>
                    <app-category-limit-row
                      [name]="row.name"
                      [iconKey]="row.icon"
                      [color]="row.color"
                      [entry]="row.entry"
                      [currency]="currency.selectedCurrency()"
                      [control]="row.control"
                    />
                  </li>
                } @empty {
                  <li class="py-4 text-sm text-content-muted">Nenhuma categoria de despesa cadastrada.</li>
                }
              </ul>
            </app-panel>

            <div class="flex justify-end">
              <button type="submit" appButton [disabled]="saving()">{{ saving() ? 'Salvando…' : 'Salvar meta' }}</button>
            </div>
          </form>
        </div>
      }
      @default {
        <p role="status" aria-busy="true" class="py-10 text-center text-sm text-content-muted">Carregando meta…</p>
      }
    }
  `,
})
export class BudgetPage {
  protected readonly store = inject(BudgetStore);
  protected readonly month = inject(ReferenceMonthStore);
  protected readonly currency = inject(CurrencyStore);
  private readonly categories = inject(CategoriesStore);
  private readonly toast = inject(ToastService);

  protected readonly saving = signal(false);

  /** Um controle por categoria de despesa, indexado pelo id (recriado a cada carga). */
  protected readonly categoryControls = new FormRecord<FormControl<string>>({});
  protected readonly form = new FormGroup({
    monthlyLimit: new FormControl('', { nonNullable: true, validators: [decimalValue] }),
    overrideLimit: new FormControl('', { nonNullable: true, validators: [decimalValue] }),
    overrideDisabled: new FormControl(false, { nonNullable: true }),
  });

  protected readonly rows = computed(() => {
    const progress = this.store.data()?.progress.categories ?? [];
    return progress.flatMap((entry) => {
      const id = entry.categoryId;
      const category = id ? this.categories.byId(id) : undefined;
      const control = id ? this.categoryControls.controls[id] : undefined;
      return id && category && control
        ? [{ id, name: category.name, icon: category.icon, color: category.color, entry, control }]
        : [];
    });
  });

  constructor() {
    this.categories.ensureLoaded();
    // cada carga (mês, moeda ou depois de gravar) preenche todos os campos com o que o servidor tem
    effect(() => {
      const data = this.store.data();
      if (!data) {
        return;
      }
      untracked(() => this.fill(data));
    });
    // "sem meta neste mês" desativa o campo do ajuste
    this.form.controls.overrideDisabled.valueChanges.subscribe((disabled) => {
      const control = this.form.controls.overrideLimit;
      if (disabled) {
        control.disable({ emitEvent: false });
      } else {
        control.enable({ emitEvent: false });
      }
    });
  }

  protected save(): void {
    if (this.saving()) {
      return;
    }
    this.form.markAllAsTouched();
    this.categoryControls.markAllAsTouched();
    if (this.form.invalid || this.categoryControls.invalid) {
      return;
    }
    const raw = this.form.getRawValue();
    const request: SaveBudgetRequest = {
      month: this.month.monthKey(),
      categoryLimits: Object.entries(this.categoryControls.controls)
        .filter(([, control]) => control.value !== '')
        .map(([categoryId, control]) => ({ categoryId, limit: control.value })),
      ...(raw.monthlyLimit !== '' ? { monthlyLimit: raw.monthlyLimit } : {}),
      ...(raw.overrideDisabled
        ? { monthOverride: { disabled: true } }
        : raw.overrideLimit !== ''
          ? { monthOverride: { limit: raw.overrideLimit } }
          : {}),
    };
    this.saving.set(true);
    this.store.save(request).subscribe({
      next: () => {
        this.saving.set(false);
        this.toast.success('Meta salva.');
      },
      error: (error: unknown) => {
        // nada foi gravado e os valores digitados continuam na tela
        this.saving.set(false);
        this.handleError(error);
      },
    });
  }

  private fill(data: NonNullable<ReturnType<BudgetStore['data']>>): void {
    const { budget, progress } = data;
    for (const key of Object.keys(this.categoryControls.controls)) {
      this.categoryControls.removeControl(key);
    }
    for (const entry of progress.categories) {
      if (entry.categoryId) {
        const saved = budget.categoryLimits.find((limit) => limit.categoryId === entry.categoryId);
        this.categoryControls.addControl(
          entry.categoryId,
          new FormControl(asField(saved?.limit), { nonNullable: true, validators: [decimalValue] }),
        );
      }
    }
    const disabled = budget.monthOverride?.disabled === true;
    this.form.reset(
      {
        monthlyLimit: asField(budget.monthlyLimit),
        overrideLimit: asField(budget.monthOverride?.limit),
        overrideDisabled: disabled,
      },
      { emitEvent: false },
    );
    if (disabled) {
      this.form.controls.overrideLimit.disable({ emitEvent: false });
    } else {
      this.form.controls.overrideLimit.enable({ emitEvent: false });
    }
  }

  private handleError(error: unknown): void {
    if (!isApiError(error)) {
      this.toast.error(GENERIC_ERROR);
      return;
    }
    if (error.code === 'RESOURCE_NOT_FOUND') {
      // categoria removida em outra aba: recarrega os seletores
      this.categories.load();
      this.toast.error('Uma categoria não existe mais. Revise as metas e tente novamente.');
      return;
    }
    this.toast.error(error.fieldErrors[0]?.message ?? error.message ?? GENERIC_ERROR);
  }
}
