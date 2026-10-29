import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { formatDate, formatMoney } from '../../../shared/money/format-money';
import { CurrencyCode, OverviewTransaction } from '../data-access/overview-api.service';

/**
 * Últimas transações do mês (até 8): data, descrição, categoria, subcategoria, tipo (chip `Entrada`/`Saída`) e valor com
 * sinal e cor; tabela a partir de `md` e cartões abaixo. Subcategoria ausente: `—`. Vazio: `Nenhum lançamento no período.`
 */
@Component({
  selector: 'app-latest-transactions',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (items().length === 0) {
      <p class="py-8 text-center text-sm text-content-muted">Nenhum lançamento no período.</p>
    } @else {
      <table class="hidden w-full border-collapse text-sm md:table">
        <caption class="sr-only">Últimas transações do mês</caption>
        <thead>
          <tr class="border-b border-line text-left text-xs uppercase tracking-wide text-content-muted">
            <th scope="col" class="px-3 py-2 font-medium">Data</th>
            <th scope="col" class="px-3 py-2 font-medium">Descrição</th>
            <th scope="col" class="px-3 py-2 font-medium">Categoria</th>
            <th scope="col" class="px-3 py-2 font-medium">Subcategoria</th>
            <th scope="col" class="px-3 py-2 font-medium">Tipo</th>
            <th scope="col" class="px-3 py-2 text-right font-medium">Valor</th>
          </tr>
        </thead>
        <tbody>
          @for (item of items(); track item.id) {
            <tr class="border-b border-line last:border-0 odd:bg-surface-sunken/40">
              <td class="whitespace-nowrap px-3 py-2 text-content">{{ date(item) }}</td>
              <td class="px-3 py-2 font-medium text-content">{{ item.description }}</td>
              <td class="px-3 py-2 text-content">
                <span class="inline-flex items-center gap-2">
                  <span class="size-2.5 rounded-full" [style.background-color]="item.category.color" aria-hidden="true"></span>
                  {{ item.category.name }}
                </span>
              </td>
              <td class="px-3 py-2 text-content-muted">{{ item.subcategory?.name ?? '—' }}</td>
              <td class="px-3 py-2"><span class="rounded-full px-2 py-0.5 text-xs font-medium" [class]="chipClass(item)">{{ kind(item) }}</span></td>
              <td class="whitespace-nowrap px-3 py-2 text-right font-semibold" [class]="amountClass(item)">{{ amount(item) }}</td>
            </tr>
          }
        </tbody>
      </table>

      <ul class="flex flex-col gap-3 md:hidden" aria-label="Últimas transações do mês">
        @for (item of items(); track item.id) {
          <li class="rounded-card border border-line bg-surface-raised p-3">
            <div class="flex items-start justify-between gap-3">
              <div class="min-w-0">
                <p class="break-words font-medium text-content">{{ item.description }}</p>
                <p class="text-xs text-content-muted">{{ date(item) }} · {{ item.category.name }} · {{ item.subcategory?.name ?? '—' }}</p>
              </div>
              <p class="whitespace-nowrap font-semibold" [class]="amountClass(item)">{{ amount(item) }}</p>
            </div>
            <p class="mt-1"><span class="rounded-full px-2 py-0.5 text-xs font-medium" [class]="chipClass(item)">{{ kind(item) }}</span></p>
          </li>
        }
      </ul>
    }
    <p class="mt-3 text-right text-sm">
      <a routerLink="/lancamentos" class="font-medium text-accent underline underline-offset-2">Ver todas →</a>
    </p>
  `,
})
export class LatestTransactions {
  readonly items = input.required<readonly OverviewTransaction[]>();

  protected date(item: OverviewTransaction): string {
    return formatDate(item.date);
  }

  protected kind(item: OverviewTransaction): string {
    return item.kind === 'INCOME' ? 'Entrada' : 'Saída';
  }

  protected amount(item: OverviewTransaction): string {
    return formatMoney(`${item.kind === 'INCOME' ? '+' : '-'}${item.amount}`, item.currency as CurrencyCode);
  }

  protected amountClass(item: OverviewTransaction): string {
    return item.kind === 'INCOME' ? 'text-income' : 'text-expense';
  }

  protected chipClass(item: OverviewTransaction): string {
    return item.kind === 'INCOME' ? 'bg-income/15 text-income' : 'bg-expense/15 text-expense';
  }
}
