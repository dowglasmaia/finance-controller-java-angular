import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideAngularModule, Target } from 'lucide-angular';
import { CurrencyStore } from '../../../core/state/currency.store';
import { ReferenceMonthStore } from '../../../core/state/reference-month.store';
import { formatMoney } from '../../../shared/money/format-money';
import { AppPanel } from '../../../shared/ui/app-panel';
import { ButtonDirective } from '../../../shared/ui/button';
import { EmptyState } from '../../../shared/ui/empty-state';
import { KpiCard } from '../../../shared/ui/kpi-card';
import { PageHeader } from '../../../shared/ui/page-header';
import { BudgetProgressCard } from '../../budget/ui/budget-progress-card';
import { deltaFor, greetingFor } from '../dashboard-format';
import { OverviewStore } from '../data-access/overview.store';
import { ExpensePie } from '../ui/expense-pie';
import { IncomeExpenseBars } from '../ui/income-expense-bars';
import { LatestTransactions } from '../ui/latest-transactions';

/** `/`: resumo do mês e da moeda selecionados — KPIs, meta, barras, pizza por categoria e últimas transações. */
@Component({
  selector: 'app-dashboard-page',
  imports: [
    RouterLink,
    LucideAngularModule,
    PageHeader,
    AppPanel,
    ButtonDirective,
    EmptyState,
    KpiCard,
    BudgetProgressCard,
    IncomeExpenseBars,
    ExpensePie,
    LatestTransactions,
  ],
  providers: [OverviewStore],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-page-header [title]="greeting" [subtitle]="'Resumo de ' + month.label()" />

    @switch (store.status()) {
      @case ('error') {
        <app-panel>
          <app-empty-state message="Não foi possível carregar o resumo do mês.">
            <button type="button" appButton variant="secondary" (click)="store.refresh()">Tentar novamente</button>
          </app-empty-state>
        </app-panel>
      }
      @case ('loaded') {
        @if (store.data(); as overview) {
          <div class="flex flex-col gap-4">
            <section class="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4" aria-label="Indicadores do mês">
              <app-kpi-card label="Saldo" [value]="money(overview.summary.balance)" [tone]="overview.summary.balance.startsWith('-') ? 'negative' : 'positive'" />
              <app-kpi-card label="Entradas" [value]="money(overview.summary.income)" [delta]="incomeDelta()" />
              <app-kpi-card label="Saídas" [value]="money(overview.summary.expense)" [delta]="expenseDelta()" />
              <app-kpi-card label="Reservas" [value]="money(overview.reservesTotal)" />
            </section>

            @if (overview.budgetProgress.limit) {
              <app-budget-progress-card [entry]="overview.budgetProgress" [currency]="currency.selectedCurrency()" [monthLabel]="month.label()" />
            } @else {
              <app-panel>
                <div class="flex flex-wrap items-center justify-between gap-3">
                  <p class="inline-flex items-center gap-2 text-sm text-content-muted">
                    <lucide-icon [img]="target" [size]="18" aria-hidden="true" />
                    Sem meta definida para {{ month.label() }}. Defina um teto de gastos para acompanhar o mês.
                  </p>
                  <a routerLink="/metas" appButton variant="secondary">Definir meta</a>
                </div>
              </app-panel>
            }

            <div class="grid gap-4 lg:grid-cols-2">
              <app-panel title="Entradas × Saídas">
                <app-income-expense-bars [bars]="overview.bars" [currency]="currency.selectedCurrency()" />
              </app-panel>
              <app-panel title="Por Categoria">
                <app-expense-pie [items]="overview.expenseByCategory" [currency]="currency.selectedCurrency()" />
              </app-panel>
            </div>

            <app-panel title="Últimas Transações">
              <app-latest-transactions [items]="overview.latestTransactions" />
            </app-panel>
          </div>
        }
      }
      @default {
        <div class="flex flex-col gap-4" role="status" aria-busy="true" aria-label="Carregando o resumo do mês">
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
            @for (n of skeletons; track n) {
              <div class="h-24 animate-pulse rounded-card bg-surface-sunken"></div>
            }
          </div>
          <div class="grid gap-4 lg:grid-cols-2">
            <div class="h-56 animate-pulse rounded-card bg-surface-sunken"></div>
            <div class="h-56 animate-pulse rounded-card bg-surface-sunken"></div>
          </div>
          <span class="sr-only">Carregando…</span>
        </div>
      }
    }
  `,
})
export class DashboardPage {
  protected readonly store = inject(OverviewStore);
  protected readonly month = inject(ReferenceMonthStore);
  protected readonly currency = inject(CurrencyStore);

  protected readonly target = Target;
  protected readonly skeletons = [1, 2, 3, 4];
  protected readonly greeting = greetingFor(new Date().getHours());

  protected readonly incomeDelta = computed(() => deltaFor(this.store.data()?.incomeDeltaPercent, false));
  /** Para saídas, gastar menos é a boa notícia. */
  protected readonly expenseDelta = computed(() => deltaFor(this.store.data()?.expenseDeltaPercent, true));

  protected money(value: string): string {
    return formatMoney(value, this.currency.selectedCurrency());
  }
}
