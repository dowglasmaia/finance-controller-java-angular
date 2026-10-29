import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { LucideAngularModule, Pencil, Trash2 } from 'lucide-angular';
import { formatDate, formatMoney } from '../../../shared/money/format-money';
import { Transaction } from '../data-access/transactions-api.service';

/**
 * Lista de lançamentos: tabela a partir de `md` e cartões abaixo (FRONTEND-SPEC-07). Apresentação pura: emite
 * `edit` e `remove`. O valor leva sinal e cor (entrada `+`, saída `−`) sem depender só da cor.
 */
@Component({
  selector: 'app-transactions-list',
  imports: [LucideAngularModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <table class="hidden w-full border-collapse text-sm md:table">
      <caption class="sr-only">Lançamentos do período</caption>
      <thead>
        <tr class="border-b border-line text-left text-xs uppercase tracking-wide text-content-muted">
          <th scope="col" class="px-3 py-2 font-medium">Data</th>
          <th scope="col" class="px-3 py-2 font-medium">Descrição</th>
          <th scope="col" class="px-3 py-2 font-medium">Categoria</th>
          <th scope="col" class="px-3 py-2 font-medium">Subcategoria</th>
          <th scope="col" class="px-3 py-2 text-right font-medium">Valor</th>
          <th scope="col" class="px-3 py-2 text-right font-medium"><span class="sr-only">Ações</span></th>
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
            <td class="whitespace-nowrap px-3 py-2 text-right font-semibold" [class]="amountClass(item)">{{ amount(item) }}</td>
            <td class="whitespace-nowrap px-3 py-2 text-right">
              <button type="button" class="action" [attr.aria-label]="'Editar ' + item.description" (click)="edit.emit(item)">
                <lucide-icon [img]="pencil" [size]="18" aria-hidden="true" />
              </button>
              <button type="button" class="action" [attr.aria-label]="'Excluir ' + item.description" (click)="remove.emit(item)">
                <lucide-icon [img]="trash" [size]="18" aria-hidden="true" />
              </button>
            </td>
          </tr>
        }
      </tbody>
    </table>

    <ul class="flex flex-col gap-3 md:hidden" aria-label="Lançamentos do período">
      @for (item of items(); track item.id) {
        <li class="rounded-card border border-line bg-surface-raised p-3">
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0">
              <p class="break-words font-medium text-content">{{ item.description }}</p>
              <p class="text-xs text-content-muted">{{ date(item) }} · {{ item.category.name }}@if (item.subcategory) { · {{ item.subcategory.name }}}</p>
            </div>
            <p class="whitespace-nowrap font-semibold" [class]="amountClass(item)">{{ amount(item) }}</p>
          </div>
          <div class="mt-2 flex justify-end gap-1">
            <button type="button" class="action" [attr.aria-label]="'Editar ' + item.description" (click)="edit.emit(item)">
              <lucide-icon [img]="pencil" [size]="18" aria-hidden="true" />
            </button>
            <button type="button" class="action" [attr.aria-label]="'Excluir ' + item.description" (click)="remove.emit(item)">
              <lucide-icon [img]="trash" [size]="18" aria-hidden="true" />
            </button>
          </div>
        </li>
      }
    </ul>
  `,
  styles: `
    .action {
      display: inline-flex;
      min-height: 2.75rem;
      min-width: 2.75rem;
      align-items: center;
      justify-content: center;
      border-radius: var(--radius-control);
      color: var(--color-content-muted);
    }
    .action:hover {
      background-color: var(--color-surface-sunken);
      color: var(--color-content);
    }
  `,
})
export class TransactionsList {
  readonly items = input.required<readonly Transaction[]>();
  readonly edit = output<Transaction>();
  readonly remove = output<Transaction>();

  protected readonly pencil = Pencil;
  protected readonly trash = Trash2;

  protected date(item: Transaction): string {
    return formatDate(item.date);
  }

  protected amount(item: Transaction): string {
    return formatMoney(`${item.kind === 'INCOME' ? '+' : '-'}${item.amount}`, item.currency);
  }

  protected amountClass(item: Transaction): string {
    return item.kind === 'INCOME' ? 'text-income' : 'text-expense';
  }
}
