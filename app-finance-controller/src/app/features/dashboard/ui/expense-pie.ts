import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { formatMoney } from '../../../shared/money/format-money';
import { iconFor } from '../../categories/category-catalog';
import { formatPercent } from '../dashboard-format';
import { CategoryExpense, CurrencyCode } from '../data-access/overview-api.service';

/** Circunferência de um círculo de raio 15.9155 é 100: o comprimento do traço é o percentual da fatia. */
const RADIUS = 15.9155;

/**
 * Pizza de despesas por categoria com legenda (ícone, nome, percentual e valor). As fatias só repetem o que a legenda
 * diz por extenso, então o gráfico não depende da cor. Vazio: `Sem despesas no período.`
 */
@Component({
  selector: 'app-expense-pie',
  imports: [LucideAngularModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (items().length === 0) {
      <p class="py-8 text-center text-sm text-content-muted">Sem despesas no período.</p>
    } @else {
      <div class="flex flex-col items-center gap-4">
        <svg viewBox="0 0 42 42" class="size-40 shrink-0 -rotate-90" role="img" aria-label="Despesas por categoria (detalhes na lista ao lado)">
          <circle cx="21" cy="21" [attr.r]="radius" fill="none" class="stroke-surface-sunken" stroke-width="6" />
          @for (slice of slices(); track slice.id) {
            <circle
              cx="21"
              cy="21"
              [attr.r]="radius"
              fill="none"
              stroke-width="6"
              [attr.stroke]="slice.color"
              [attr.stroke-dasharray]="slice.length + ' ' + (100 - slice.length)"
              [attr.stroke-dashoffset]="-slice.offset"
            >
              <title>{{ slice.name }} · {{ slice.percent }}</title>
            </circle>
          }
        </svg>
        <ul class="flex w-full min-w-0 flex-col gap-2 text-sm" aria-label="Despesas por categoria">
          @for (slice of slices(); track slice.id) {
            <li class="flex items-center gap-2">
              <span
                class="flex size-7 shrink-0 items-center justify-center rounded-control"
                [style.background-color]="slice.color + '26'"
                [style.color]="slice.color"
                aria-hidden="true"
              >
                <lucide-icon [img]="slice.icon" [size]="16" />
              </span>
              <span class="min-w-0 flex-1 truncate text-content">{{ slice.name }}</span>
              <span class="text-content-muted">{{ slice.percent }}</span>
              <span class="whitespace-nowrap font-medium text-content">{{ slice.amount }}</span>
            </li>
          }
        </ul>
      </div>
    }
  `,
})
export class ExpensePie {
  readonly items = input.required<readonly CategoryExpense[]>();
  readonly currency = input.required<CurrencyCode>();

  protected readonly radius = RADIUS;

  protected readonly slices = computed(() => {
    let offset = 0;
    return this.items().map((item) => {
      // só geometria; o percentual exibido é o texto do servidor formatado
      const length = Math.max(0, Math.min(100, Number(item.percentage)));
      const slice = {
        id: item.category.id,
        name: item.category.name,
        color: item.category.color,
        icon: iconFor(item.category.icon),
        percent: formatPercent(item.percentage),
        amount: formatMoney(item.amount, this.currency()),
        length,
        offset,
      };
      offset += length;
      return slice;
    });
  });
}
