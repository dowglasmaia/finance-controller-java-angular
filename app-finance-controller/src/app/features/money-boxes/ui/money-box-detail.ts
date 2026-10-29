import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal } from '@angular/core';
import { LucideAngularModule, Pencil, Trash2 } from 'lucide-angular';
import { isApiError } from '../../../core/http/api-error';
import { ButtonDirective } from '../../../shared/ui/button';
import { ConfirmDialog } from '../../../shared/ui/confirm-dialog';
import { Modal } from '../../../shared/ui/modal';
import { ToastService } from '../../../shared/ui/toast.service';
import { formatDate, formatMoney } from '../../../shared/money/format-money';
import { percentOf } from '../../budget/budget-status';
import { CurrencyCode, MoneyBox, MoneyBoxEntry } from '../data-access/money-boxes-api.service';
import { MoneyBoxesStore } from '../data-access/money-boxes.store';

/**
 * Detalhe do cofrinho: saldo, barra de meta e o histórico `Aportes` (mais recentes primeiro). Editar ou excluir uma
 * movimentação que deixaria o saldo negativo é recusado pelo servidor e a movimentação permanece.
 */
@Component({
  selector: 'app-money-box-detail',
  imports: [Modal, ButtonDirective, ConfirmDialog, LucideAngularModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-modal [title]="box().name" (closed)="closed.emit()">
      <div class="flex flex-col gap-4">
        <div>
          <p class="text-sm text-content-muted">Saldo</p>
          <p class="break-words text-3xl font-semibold text-reserve">{{ balance() }}</p>
          @if (box().goal) {
            <div
              class="mt-2 h-2 overflow-hidden rounded-full bg-surface-sunken"
              role="progressbar"
              aria-valuemin="0"
              aria-valuemax="100"
              [attr.aria-valuenow]="percent()"
              [attr.aria-label]="'Progresso: ' + percent() + '% da meta'"
            >
              <div class="h-full rounded-full" [style.background-color]="box().color" [style.width.%]="percent()"></div>
            </div>
            <p class="mt-1 text-xs text-content-muted">{{ percent() }}% de {{ goal() }}</p>
          }
        </div>

        <div class="grid grid-cols-2 gap-2">
          <button type="button" class="inline-flex min-h-11 items-center justify-center rounded-control bg-income/15 px-3 text-sm font-semibold text-income hover:bg-income/25" (click)="deposit.emit()">
            Aporte
          </button>
          <button
            type="button"
            class="inline-flex min-h-11 items-center justify-center rounded-control bg-expense/15 px-3 text-sm font-semibold text-expense hover:bg-expense/25 disabled:cursor-not-allowed disabled:opacity-50"
            [disabled]="!canWithdraw()"
            [attr.title]="canWithdraw() ? null : 'Saldo insuficiente'"
            (click)="withdraw.emit()"
          >
            Saque
          </button>
        </div>
        @if (!canWithdraw()) {
          <p class="-mt-2 text-xs text-content-muted">Saldo insuficiente para saque.</p>
        }

        <section aria-labelledby="entries-title">
          <h3 id="entries-title" class="mb-2 text-base font-semibold text-content">Aportes</h3>
          @switch (store.entriesStatus()) {
            @case ('error') {
              <p role="alert" class="text-sm text-expense">Não foi possível carregar os aportes.</p>
              <button type="button" appButton variant="secondary" (click)="store.reloadEntries()">Tentar novamente</button>
            }
            @case ('loaded') {
              @if (store.entries().length === 0) {
                <p class="py-4 text-center text-sm text-content-muted">Nenhum aporte ainda.</p>
              } @else {
                <ul class="divide-y divide-line">
                  @for (entry of store.entries(); track entry.id) {
                    <li class="flex items-center gap-2 py-2">
                      <div class="min-w-0 flex-1">
                        <p class="break-words text-sm font-medium text-content">{{ noteOf(entry) }}</p>
                        <p class="text-xs text-content-muted">{{ date(entry) }}</p>
                      </div>
                      <p class="whitespace-nowrap text-sm font-semibold" [class]="entry.kind === 'DEPOSIT' ? 'text-income' : 'text-expense'">
                        <span aria-hidden="true">{{ entry.kind === 'DEPOSIT' ? '+' : '−' }}</span>
                        <span class="sr-only">{{ entry.kind === 'DEPOSIT' ? 'Depósito de' : 'Saque de' }}</span>
                        {{ amount(entry) }}
                      </p>
                      <button
                        type="button"
                        class="inline-flex min-h-11 min-w-11 items-center justify-center rounded-control text-content-muted hover:bg-surface-sunken hover:text-content"
                        [attr.aria-label]="'Editar ' + describe(entry)"
                        (click)="editEntry.emit(entry)"
                      >
                        <lucide-icon [img]="pencil" [size]="18" aria-hidden="true" />
                      </button>
                      <button
                        type="button"
                        class="inline-flex min-h-11 min-w-11 items-center justify-center rounded-control text-content-muted hover:bg-surface-sunken hover:text-expense"
                        [attr.aria-label]="'Excluir ' + describe(entry)"
                        (click)="deleteTarget.set(entry)"
                      >
                        <lucide-icon [img]="trash" [size]="18" aria-hidden="true" />
                      </button>
                    </li>
                  }
                </ul>
              }
            }
            @default {
              <p role="status" aria-busy="true" class="py-4 text-center text-sm text-content-muted">Carregando aportes…</p>
            }
          }
        </section>

        <div class="flex justify-end">
          <button type="button" appButton variant="secondary" (click)="closed.emit()">Fechar</button>
        </div>
      </div>
    </app-modal>

    @if (deleteTarget(); as target) {
      <app-confirm-dialog
        title="Excluir movimentação?"
        [message]="'O movimento de ' + date(target) + ' (' + amount(target) + ') será removido do histórico.'"
        confirmLabel="Excluir"
        [busy]="deleting()"
        (confirmed)="confirmDelete(target)"
        (cancelled)="deleteTarget.set(null)"
      />
    }
  `,
})
export class MoneyBoxDetail {
  readonly box = input.required<MoneyBox>();
  readonly currency = input.required<CurrencyCode>();
  readonly closed = output<void>();
  readonly deposit = output<void>();
  readonly withdraw = output<void>();
  readonly editEntry = output<MoneyBoxEntry>();

  protected readonly store = inject(MoneyBoxesStore);
  private readonly toast = inject(ToastService);

  protected readonly pencil = Pencil;
  protected readonly trash = Trash2;
  protected readonly deleteTarget = signal<MoneyBoxEntry | null>(null);
  protected readonly deleting = signal(false);

  protected readonly balance = computed(() => formatMoney(this.box().balance, this.currency()));
  protected readonly goal = computed(() => formatMoney(this.box().goal ?? '0', this.currency()));
  protected readonly percent = computed(() => Math.min(100, percentOf(this.box().progress)));
  protected readonly canWithdraw = computed(() => Number(this.box().balance) > 0);

  protected noteOf(entry: MoneyBoxEntry): string {
    return entry.note ?? (entry.kind === 'DEPOSIT' ? 'Depósito' : 'Saque');
  }

  protected date(entry: MoneyBoxEntry): string {
    return formatDate(entry.date);
  }

  /** `depósito de R$ 1.500,00 em 01/10/2026`: distingue movimentos do mesmo dia para leitor de tela. */
  protected describe(entry: MoneyBoxEntry): string {
    return `${entry.kind === 'DEPOSIT' ? 'depósito' : 'saque'} de ${this.amount(entry)} em ${this.date(entry)}`;
  }

  protected amount(entry: MoneyBoxEntry): string {
    return formatMoney(entry.amount, this.currency());
  }

  protected confirmDelete(entry: MoneyBoxEntry): void {
    this.deleting.set(true);
    this.store.deleteEntry(this.box().id, entry.id).subscribe({
      next: () => {
        this.deleting.set(false);
        this.deleteTarget.set(null);
        this.toast.success('Movimentação excluída.');
      },
      error: (error: unknown) => {
        this.deleting.set(false);
        this.deleteTarget.set(null);
        this.toast.error(
          isApiError(error) && error.code === 'INSUFFICIENT_BALANCE'
            ? 'Saldo insuficiente: excluir este movimento deixaria o saldo negativo.'
            : isApiError(error)
              ? error.message
              : 'Não foi possível excluir a movimentação.',
        );
      },
    });
  }
}
