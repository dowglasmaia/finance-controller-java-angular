import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { formatMoney } from '../../../shared/money/format-money';
import { changeText, monthName, signClass } from '../reports-format';
import { CurrencyCode, MonthlySummaryItem } from '../data-access/reports-api.service';

/**
 * `Resumo Mensal` do período Ano: mês, entradas, saídas, saldo (cor e sinal) e `% vs anterior`, que é `—` no primeiro
 * mês e quando o saldo anterior é zero (o servidor omite a variação nesses casos).
 */
@Component({
  selector: 'app-monthly-summary-table',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="overflow-x-auto">
      <table class="w-full min-w-[32rem] text-sm">
        <caption class="sr-only">Resumo mensal do ano</caption>
        <thead>
          <tr class="border-b border-line text-left text-content-muted">
            <th scope="col" class="py-2 pr-3 font-medium">Mês</th>
            <th scope="col" class="px-3 py-2 text-right font-medium">Entradas</th>
            <th scope="col" class="px-3 py-2 text-right font-medium">Saídas</th>
            <th scope="col" class="px-3 py-2 text-right font-medium">Saldo</th>
            <th scope="col" class="py-2 pl-3 text-right font-medium">% vs anterior</th>
          </tr>
        </thead>
        <tbody>
          @for (row of rows(); track row.month) {
            <tr class="border-b border-line last:border-0">
              <th scope="row" class="py-2 pr-3 text-left font-medium text-content">{{ row.label }}</th>
              <td class="whitespace-nowrap px-3 py-2 text-right text-income">{{ row.income }}</td>
              <td class="whitespace-nowrap px-3 py-2 text-right text-expense">{{ row.expense }}</td>
              <td class="whitespace-nowrap px-3 py-2 text-right font-medium" [class]="row.balanceClass">{{ row.balance }}</td>
              <td class="whitespace-nowrap py-2 pl-3 text-right" [class]="row.changeClass">{{ row.change }}</td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
})
export class MonthlySummaryTable {
  readonly items = input.required<readonly MonthlySummaryItem[]>();
  readonly currency = input.required<CurrencyCode>();

  protected readonly rows = computed(() =>
    this.items().map((item) => ({
      month: item.month,
      label: monthName(item.month),
      income: formatMoney(item.income, this.currency()),
      expense: formatMoney(item.expense, this.currency()),
      balance: formatMoney(item.balance, this.currency()),
      balanceClass: signClass(item.balance),
      change: changeText(item.changePercent),
      changeClass: signClass(item.changePercent),
    })),
  );
}
