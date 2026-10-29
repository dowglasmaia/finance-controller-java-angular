import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { LucideAngularModule, Pencil, Trash2 } from 'lucide-angular';
import { formatMoney } from '../../../shared/money/format-money';
import { percentOf } from '../../budget/budget-status';
import { iconFor } from '../../categories/category-catalog';
import { CurrencyCode, MoneyBox } from '../data-access/money-boxes-api.service';

/** Cartão de um cofrinho: ícone/cor, nome, descrição, saldo, barra de meta (se houver) e as ações. */
@Component({
  selector: 'app-money-box-card',
  imports: [LucideAngularModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <article class="flex h-full flex-col gap-3 rounded-card border border-line bg-surface-raised p-4 shadow-card">
      <div class="flex items-start gap-3">
        <span
          class="flex size-11 shrink-0 items-center justify-center rounded-control"
          [style.background-color]="box().color + '26'"
          [style.color]="box().color"
          aria-hidden="true"
        >
          <lucide-icon [img]="icon()" [size]="22" />
        </span>
        <div class="min-w-0 flex-1">
          <h3 class="break-words text-base font-semibold text-content">
            <button type="button" class="text-left hover:underline focus-visible:underline" (click)="open.emit(box())">
              {{ box().name }}<span class="sr-only"> (ver aportes)</span>
            </button>
          </h3>
          @if (box().description) {
            <p class="break-words text-xs text-content-muted">{{ box().description }}</p>
          }
        </div>
        <div class="flex shrink-0">
          <button
            type="button"
            class="inline-flex min-h-11 min-w-11 items-center justify-center rounded-control text-content-muted hover:bg-surface-sunken hover:text-content"
            [attr.aria-label]="'Editar ' + box().name"
            (click)="edit.emit(box())"
          >
            <lucide-icon [img]="pencil" [size]="18" aria-hidden="true" />
          </button>
          <button
            type="button"
            class="inline-flex min-h-11 min-w-11 items-center justify-center rounded-control text-content-muted hover:bg-surface-sunken hover:text-expense"
            [attr.aria-label]="'Excluir ' + box().name"
            (click)="remove.emit(box())"
          >
            <lucide-icon [img]="trash" [size]="18" aria-hidden="true" />
          </button>
        </div>
      </div>

      <p class="break-words text-2xl font-semibold text-reserve">{{ balance() }}</p>

      @if (box().goal) {
        <div>
          <div
            class="h-2 overflow-hidden rounded-full bg-surface-sunken"
            role="progressbar"
            aria-valuemin="0"
            aria-valuemax="100"
            [attr.aria-valuenow]="percent()"
            [attr.aria-label]="'Progresso de ' + box().name + ': ' + percent() + '% da meta'"
          >
            <div class="h-full rounded-full transition-[width] duration-fast ease-standard" [style.background-color]="box().color" [style.width.%]="percent()"></div>
          </div>
          <p class="mt-1 text-xs text-content-muted">{{ percent() }}% de {{ goal() }}</p>
        </div>
      }

      <div class="mt-auto grid grid-cols-2 gap-2">
        <button
          type="button"
          class="inline-flex min-h-11 items-center justify-center rounded-control bg-income/15 px-3 text-sm font-semibold text-income hover:bg-income/25"
          [attr.aria-label]="'Aporte em ' + box().name"
          (click)="deposit.emit(box())"
        >
          Aporte
        </button>
        <button
          type="button"
          class="inline-flex min-h-11 items-center justify-center rounded-control bg-expense/15 px-3 text-sm font-semibold text-expense hover:bg-expense/25 disabled:cursor-not-allowed disabled:opacity-50"
          [attr.aria-label]="'Saque de ' + box().name"
          [disabled]="!canWithdraw()"
          [attr.aria-describedby]="canWithdraw() ? null : 'noBalance-' + box().id"
          [attr.title]="canWithdraw() ? null : 'Saldo insuficiente'"
          (click)="withdraw.emit(box())"
        >
          Saque
        </button>
      </div>
      @if (!canWithdraw()) {
        <p [id]="'noBalance-' + box().id" class="-mt-1 text-xs text-content-muted">Saldo insuficiente para saque.</p>
      }
    </article>
  `,
})
export class MoneyBoxCard {
  readonly box = input.required<MoneyBox>();
  readonly currency = input.required<CurrencyCode>();
  readonly open = output<MoneyBox>();
  readonly edit = output<MoneyBox>();
  readonly remove = output<MoneyBox>();
  readonly deposit = output<MoneyBox>();
  readonly withdraw = output<MoneyBox>();

  protected readonly pencil = Pencil;
  protected readonly trash = Trash2;
  protected readonly icon = computed(() => iconFor(this.box().icon));
  protected readonly balance = computed(() => formatMoney(this.box().balance, this.currency()));
  protected readonly goal = computed(() => formatMoney(this.box().goal ?? '0', this.currency()));
  protected readonly percent = computed(() => Math.min(100, percentOf(this.box().progress)));
  /** O servidor decide o saldo; sem saldo positivo o saque nem é oferecido. */
  protected readonly canWithdraw = computed(() => Number(this.box().balance) > 0);
}
