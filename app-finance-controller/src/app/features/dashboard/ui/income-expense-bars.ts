import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { formatMoney } from '../../../shared/money/format-money';
import { Bar, CurrencyCode } from '../data-access/overview-api.service';

const BAND_WIDTH = 64;
const BAR_WIDTH = 20;
const CHART_HEIGHT = 120;
const LABEL_Y = 138;

/**
 * Barras duplas Entradas × Saídas por faixa de 7 dias (S1…S5). SVG sem biblioteca: a altura é só geometria (proporção do
 * maior valor), os valores exibidos vêm formatados do texto do servidor. Alternativa textual: tabela para leitor de tela
 * e `title` em cada barra (dica com o valor). Cada barra traz a legenda "Entradas" e "Saídas" fora da cor.
 */
@Component({
  selector: 'app-income-expense-bars',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <figure class="m-0">
      <svg
        role="img"
        [attr.viewBox]="'0 0 ' + width() + ' 146'"
        class="h-auto w-full"
        aria-label="Entradas e saídas por faixa de 7 dias do mês"
      >
        <line [attr.x1]="0" [attr.x2]="width()" [attr.y1]="baseline" [attr.y2]="baseline" class="stroke-line" stroke-width="1" />
        @for (band of layout(); track band.label) {
          <g>
            <rect [attr.x]="band.x" [attr.y]="band.incomeY" [attr.width]="barWidth" [attr.height]="band.incomeHeight" rx="3" class="fill-income">
              <title>{{ band.label }} · Entradas: {{ band.incomeText }}</title>
            </rect>
            <rect [attr.x]="band.x + barWidth + 4" [attr.y]="band.expenseY" [attr.width]="barWidth" [attr.height]="band.expenseHeight" rx="3" class="fill-expense">
              <title>{{ band.label }} · Saídas: {{ band.expenseText }}</title>
            </rect>
            <text [attr.x]="band.x + barWidth + 2" [attr.y]="labelY" text-anchor="middle" class="fill-content-muted" font-size="11">{{ band.label }}</text>
          </g>
        }
      </svg>
      <figcaption class="mt-2 flex items-center justify-center gap-4 text-xs text-content-muted">
        <span class="inline-flex items-center gap-1.5"><span class="size-3 rounded-sm bg-income" aria-hidden="true"></span>Entradas</span>
        <span class="inline-flex items-center gap-1.5"><span class="size-3 rounded-sm bg-expense" aria-hidden="true"></span>Saídas</span>
      </figcaption>
      <table class="sr-only">
        <caption>Entradas e saídas por faixa de 7 dias</caption>
        <thead>
          <tr><th scope="col">Faixa</th><th scope="col">Entradas</th><th scope="col">Saídas</th></tr>
        </thead>
        <tbody>
          @for (band of layout(); track band.label) {
            <tr><th scope="row">{{ band.label }}</th><td>{{ band.incomeText }}</td><td>{{ band.expenseText }}</td></tr>
          }
        </tbody>
      </table>
    </figure>
  `,
})
export class IncomeExpenseBars {
  readonly bars = input.required<readonly Bar[]>();
  readonly currency = input.required<CurrencyCode>();

  protected readonly barWidth = BAR_WIDTH;
  protected readonly baseline = CHART_HEIGHT;
  protected readonly labelY = LABEL_Y;
  protected readonly width = computed(() => this.bars().length * BAND_WIDTH);

  protected readonly layout = computed(() => {
    const bars = this.bars();
    // só geometria: proporção do maior valor da lista
    const max = Math.max(1, ...bars.flatMap((bar) => [Number(bar.income), Number(bar.expense)]));
    const height = (value: string): number => Math.round((Number(value) / max) * (CHART_HEIGHT - 6));
    return bars.map((bar, index) => {
      const incomeHeight = height(bar.income);
      const expenseHeight = height(bar.expense);
      return {
        label: bar.label,
        x: index * BAND_WIDTH + (BAND_WIDTH - (2 * BAR_WIDTH + 4)) / 2,
        incomeHeight,
        incomeY: CHART_HEIGHT - incomeHeight,
        expenseHeight,
        expenseY: CHART_HEIGHT - expenseHeight,
        incomeText: formatMoney(bar.income, this.currency()),
        expenseText: formatMoney(bar.expense, this.currency()),
      };
    });
  });
}
