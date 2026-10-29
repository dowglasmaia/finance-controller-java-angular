import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { LucideAngularModule, Target } from 'lucide-angular';
import { formatMoney } from '../../../shared/money/format-money';
import { STATUS_PRESENTATION, percentOf } from '../budget-status';
import { CurrencyCode, ProgressEntry } from '../data-access/budget-api.service';

/** Cartão de progresso do mês: `Meta de {Mês Ano}`, `N%`, barra (até 100%), `{gasto} de {limite}` e o estado. */
@Component({
  selector: 'app-budget-progress-card',
  imports: [LucideAngularModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="rounded-card border border-line bg-surface-raised p-4 shadow-card sm:p-6" aria-label="Progresso da meta do mês">
      <div class="flex items-center justify-between gap-3">
        <h2 class="flex items-center gap-2 text-lg font-semibold text-content">
          <lucide-icon [img]="target" [size]="20" aria-hidden="true" />
          Meta de {{ monthLabel() }}
        </h2>
        <p class="text-2xl font-semibold" [class]="presentation().textClass">{{ percent() }}%</p>
      </div>

      <div
        class="mt-3 h-3 overflow-hidden rounded-full bg-surface-sunken"
        role="progressbar"
        [attr.aria-valuenow]="barPercent()"
        aria-valuemin="0"
        aria-valuemax="100"
        [attr.aria-label]="'Gasto do mês: ' + percent() + '% da meta'"
      >
        <div class="h-full rounded-full transition-[width] duration-fast ease-standard" [class]="presentation().barClass" [style.width.%]="barPercent()"></div>
      </div>

      <div class="mt-3 flex flex-wrap items-center justify-between gap-2 text-sm">
        <p class="text-content">
          {{ spent() }} de {{ limit() }}
          @if (remainingText(); as remaining) {
            <span class="text-content-muted">· {{ remaining }}</span>
          }
        </p>
        <p class="inline-flex items-center gap-1.5 font-medium" [class]="presentation().textClass">
          <lucide-icon [img]="presentation().icon" [size]="16" aria-hidden="true" />
          {{ presentation().label }}
        </p>
      </div>
    </section>
  `,
})
export class BudgetProgressCard {
  readonly entry = input.required<ProgressEntry>();
  readonly currency = input.required<CurrencyCode>();
  readonly monthLabel = input.required<string>();

  protected readonly target = Target;
  protected readonly presentation = computed(() => STATUS_PRESENTATION[this.entry().status]);
  protected readonly percent = computed(() => percentOf(this.entry().fraction));
  protected readonly barPercent = computed(() => Math.min(100, this.percent()));
  protected readonly spent = computed(() => formatMoney(this.entry().spent, this.currency()));
  protected readonly limit = computed(() => formatMoney(this.entry().limit ?? '0', this.currency()));
  /** `Restam R$ 150,00` ou `Excedeu R$ 50,00` (o restante do servidor pode ser negativo). */
  protected readonly remainingText = computed(() => {
    const remaining = this.entry().remaining;
    if (remaining === undefined) {
      return null;
    }
    return remaining.startsWith('-')
      ? `Excedeu ${formatMoney(remaining.slice(1), this.currency())}`
      : `Restam ${formatMoney(remaining, this.currency())}`;
  });
}
