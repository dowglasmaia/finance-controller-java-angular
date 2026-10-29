import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { CurrencyStore } from '../../../core/state/currency.store';
import { formatMoney } from '../../../shared/money/format-money';
import { AppPanel } from '../../../shared/ui/app-panel';
import { ButtonDirective } from '../../../shared/ui/button';
import { EmptyState } from '../../../shared/ui/empty-state';
import { KpiCard } from '../../../shared/ui/kpi-card';
import { PageHeader } from '../../../shared/ui/page-header';
import { ExpensePie } from '../../dashboard/ui/expense-pie';
import { IncomeExpenseBars } from '../../dashboard/ui/income-expense-bars';
import { ReportPeriod } from '../data-access/reports-api.service';
import { ReportsStore } from '../data-access/reports.store';
import { rangeText } from '../reports-format';
import { MonthlySummaryTable } from '../ui/monthly-summary-table';
import { RankingList } from '../ui/ranking-list';

const BARS_LABEL: Record<ReportPeriod, { aria: string; caption: string; column: string }> = {
  WEEK: { aria: 'Entradas e saídas por dia da semana', caption: 'Entradas e saídas por dia da semana', column: 'Dia' },
  MONTH: { aria: 'Entradas e saídas por faixa de 7 dias do mês', caption: 'Entradas e saídas por faixa de 7 dias', column: 'Faixa' },
  YEAR: { aria: 'Entradas e saídas por mês do ano', caption: 'Entradas e saídas por mês', column: 'Mês' },
};

/** `/relatorios`: KPIs, evolução de entradas e saídas, despesas por categoria, ranking e, no ano, o resumo mensal. */
@Component({
  selector: 'app-reports-page',
  imports: [PageHeader, AppPanel, EmptyState, ButtonDirective, KpiCard, IncomeExpenseBars, ExpensePie, RankingList, MonthlySummaryTable],
  providers: [ReportsStore],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-page-header title="Relatórios" subtitle="Análise financeira do período" />

    <div role="radiogroup" aria-label="Período" class="mb-4 inline-flex rounded-control border border-line bg-surface-sunken p-1">
      @for (option of periods; track option.value) {
        <button
          type="button"
          role="radio"
          class="min-h-11 rounded-control px-4 text-sm font-semibold text-content-muted aria-checked:bg-accent aria-checked:text-on-accent"
          [attr.aria-checked]="store.period() === option.value"
          (click)="store.selectPeriod(option.value)"
        >
          {{ option.label }}
        </button>
      }
    </div>

    @switch (store.status()) {
      @case ('error') {
        <app-panel>
          <app-empty-state message="Não foi possível carregar o relatório.">
            <button type="button" appButton variant="secondary" (click)="store.refresh()">Tentar novamente</button>
          </app-empty-state>
        </app-panel>
      }
      @case ('loaded') {
        @if (store.data(); as report) {
          <div class="flex flex-col gap-4">
            <p class="text-sm text-content-muted" aria-live="polite">Período: {{ range() }}</p>

            <section class="grid grid-cols-1 gap-4 sm:grid-cols-3" aria-label="Indicadores do período">
              <app-kpi-card label="Entradas" tone="income" [value]="money(report.summary.income)" />
              <app-kpi-card label="Saídas" tone="expense" [value]="money(report.summary.expense)" />
              <app-kpi-card label="Saldo" [value]="money(report.summary.balance)" [tone]="report.summary.balance.startsWith('-') ? 'negative' : 'positive'" />
            </section>

            <app-panel title="Evolução de Entradas e Saídas">
              <app-income-expense-bars
                [bars]="report.bars"
                [currency]="currency()"
                [ariaLabel]="barsLabel().aria"
                [caption]="barsLabel().caption"
                [columnHeader]="barsLabel().column"
              />
            </app-panel>

            <div class="grid grid-cols-1 gap-4 lg:grid-cols-2">
              <app-panel title="Despesas por Categoria">
                <app-expense-pie [items]="report.expenseByCategory" [currency]="currency()" />
              </app-panel>
              <app-panel title="Ranking de Categorias">
                <app-ranking-list [items]="report.ranking" [currency]="currency()" />
              </app-panel>
            </div>

            @if (report.monthlySummary; as monthly) {
              <app-panel title="Resumo Mensal">
                <app-monthly-summary-table [items]="monthly" [currency]="currency()" />
              </app-panel>
            }
          </div>
        }
      }
      @default {
        <p role="status" aria-busy="true" class="py-10 text-center text-sm text-content-muted">Carregando relatório…</p>
      }
    }
  `,
})
export class ReportsPage {
  protected readonly store = inject(ReportsStore);
  private readonly currencyStore = inject(CurrencyStore);

  protected readonly periods: readonly { value: ReportPeriod; label: string }[] = [
    { value: 'WEEK', label: 'Semana' },
    { value: 'MONTH', label: 'Mês' },
    { value: 'YEAR', label: 'Ano' },
  ];

  protected readonly currency = this.currencyStore.selectedCurrency;
  protected readonly barsLabel = computed(() => BARS_LABEL[this.store.data()?.period ?? this.store.period()]);
  protected readonly range = computed(() => {
    const report = this.store.data();
    return report ? rangeText(report.range) : '';
  });

  protected money(value: string): string {
    return formatMoney(value, this.currency());
  }
}
