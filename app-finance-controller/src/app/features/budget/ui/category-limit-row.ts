import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { MoneyInputDirective } from '../../../shared/forms/money-input';
import { formatMoney } from '../../../shared/money/format-money';
import { iconFor } from '../../categories/category-catalog';
import { STATUS_PRESENTATION } from '../budget-status';
import { CurrencyCode, ProgressEntry } from '../data-access/budget-api.service';

/** Linha de uma categoria: ícone, nome, `{gasto} de {limite}` colorido pelo estado (ou `Gasto: {gasto}`) e o campo da meta. */
@Component({
  selector: 'app-category-limit-row',
  imports: [ReactiveFormsModule, LucideAngularModule, MoneyInputDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex flex-col gap-3 py-3 sm:flex-row sm:items-center sm:justify-between">
      <div class="flex min-w-0 items-center gap-3">
        <span
          class="flex size-10 shrink-0 items-center justify-center rounded-control"
          [style.background-color]="color() + '26'"
          [style.color]="color()"
          aria-hidden="true"
        >
          <lucide-icon [img]="icon()" [size]="20" />
        </span>
        <div class="min-w-0">
          <p class="break-words font-medium text-content">{{ name() }}</p>
          @if (entry().limit) {
            <p class="inline-flex items-center gap-1.5 text-sm" [class]="presentation().textClass">
              <lucide-icon [img]="presentation().icon" [size]="14" aria-hidden="true" />
              {{ spent() }} de {{ limit() }}
              <span class="sr-only">— {{ presentation().label }}</span>
            </p>
          } @else {
            <p class="text-sm text-content-muted">Gasto: {{ spent() }}</p>
          }
        </div>
      </div>
      <input
        appMoneyInput
        type="text"
        class="min-h-11 w-full rounded-control border border-line-input bg-surface-sunken px-3 text-content placeholder:text-content-muted sm:w-40"
        placeholder="Sem meta"
        [attr.aria-label]="'Meta de ' + name()"
        [formControl]="control()"
      />
    </div>
  `,
})
export class CategoryLimitRow {
  readonly name = input.required<string>();
  readonly iconKey = input.required<string>();
  readonly color = input.required<string>();
  readonly entry = input.required<ProgressEntry>();
  readonly currency = input.required<CurrencyCode>();
  readonly control = input.required<FormControl<string>>();

  protected readonly icon = computed(() => iconFor(this.iconKey()));
  protected readonly presentation = computed(() => STATUS_PRESENTATION[this.entry().status]);
  protected readonly spent = computed(() => formatMoney(this.entry().spent, this.currency()));
  protected readonly limit = computed(() => formatMoney(this.entry().limit ?? '0', this.currency()));
}
