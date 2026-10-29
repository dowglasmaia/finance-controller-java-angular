import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { formatMoney } from '../../../shared/money/format-money';
import { iconFor } from '../../categories/category-catalog';
import { formatPercent } from '../../dashboard/dashboard-format';
import { CategoryShare, CurrencyCode } from '../data-access/reports-api.service';

/**
 * Ranking de categorias de despesa: posição, ícone, nome, percentual, valor e barra. A ordem e os percentuais vêm
 * prontos do servidor; a largura da barra é só geometria. Vazio: `Sem despesas no período.`
 */
@Component({
  selector: 'app-ranking-list',
  imports: [LucideAngularModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (rows().length === 0) {
      <p class="py-8 text-center text-sm text-content-muted">Sem despesas no período.</p>
    } @else {
      <ol class="flex flex-col gap-3" aria-label="Ranking de categorias de despesa">
        @for (row of rows(); track row.id; let i = $index) {
          <li class="flex flex-col gap-1.5">
            <div class="flex items-center gap-2 text-sm">
              <span class="w-5 shrink-0 text-center text-xs font-semibold text-content-muted">{{ i + 1 }}º</span>
              <span
                class="flex size-7 shrink-0 items-center justify-center rounded-control"
                [style.background-color]="row.color + '26'"
                [style.color]="row.color"
                aria-hidden="true"
              >
                <lucide-icon [img]="row.icon" [size]="16" />
              </span>
              <span class="min-w-0 flex-1 truncate text-content">{{ row.name }}</span>
              <span class="text-content-muted">{{ row.percent }}</span>
              <span class="whitespace-nowrap font-medium text-content">{{ row.amount }}</span>
            </div>
            <div class="ml-7 h-1.5 overflow-hidden rounded-full bg-surface-sunken" aria-hidden="true">
              <div class="h-full rounded-full" [style.background-color]="row.color" [style.width.%]="row.width"></div>
            </div>
          </li>
        }
      </ol>
    }
  `,
})
export class RankingList {
  readonly items = input.required<readonly CategoryShare[]>();
  readonly currency = input.required<CurrencyCode>();

  protected readonly rows = computed(() =>
    this.items().map((item) => ({
      id: item.category.id,
      name: item.category.name,
      color: item.category.color,
      icon: iconFor(item.category.icon),
      percent: formatPercent(item.percentage),
      amount: formatMoney(item.amount, this.currency()),
      width: Math.max(0, Math.min(100, Number(item.percentage))),
    })),
  );
}
