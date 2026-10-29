import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ArrowLeftRight, ChevronLeft, ChevronRight, LucideAngularModule, Plus } from 'lucide-angular';
import { isApiError } from '../../../core/http/api-error';
import { CurrencyStore } from '../../../core/state/currency.store';
import { formatMoney } from '../../../shared/money/format-money';
import { AppPanel } from '../../../shared/ui/app-panel';
import { ButtonDirective } from '../../../shared/ui/button';
import { ConfirmDialog } from '../../../shared/ui/confirm-dialog';
import { EmptyState } from '../../../shared/ui/empty-state';
import { PageHeader } from '../../../shared/ui/page-header';
import { ToastService } from '../../../shared/ui/toast.service';
import { KindFilter, Transaction, TransactionsApiService } from '../data-access/transactions-api.service';
import { TransactionsStore } from '../data-access/transactions.store';
import { TransactionDialog } from '../ui/transaction-dialog';
import { TransactionsList } from '../ui/transactions-list';

/** `/lancamentos`: lista do mês e da moeda selecionados, filtro por direção, subtotais, paginação e CRUD em diálogo. */
@Component({
  selector: 'app-transactions-page',
  imports: [
    PageHeader,
    AppPanel,
    EmptyState,
    ButtonDirective,
    ConfirmDialog,
    TransactionDialog,
    TransactionsList,
    LucideAngularModule,
  ],
  providers: [TransactionsStore],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-page-header title="Lançamentos" subtitle="Gerencie suas entradas e saídas">
      <button actions type="button" appButton (click)="openCreate()">
        <lucide-icon [img]="plus" [size]="18" aria-hidden="true" />
        Novo Lançamento
      </button>
    </app-page-header>

    <app-panel class="mb-4 block">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div role="radiogroup" aria-label="Filtrar por tipo" class="inline-flex self-start overflow-hidden rounded-control border border-line-input">
          @for (option of filters; track option.value) {
            <button
              type="button"
              role="radio"
              class="min-h-11 px-4 text-sm font-semibold text-content hover:bg-surface-sunken aria-checked:bg-accent aria-checked:text-on-accent"
              [attr.aria-checked]="store.kind() === option.value"
              (click)="store.setKind(option.value)"
            >
              {{ option.label }}
            </button>
          }
        </div>

        @if (store.totals(); as totals) {
          <dl class="grid grid-cols-3 gap-4 text-sm" aria-label="Subtotais do período">
            <div>
              <dt class="text-content-muted">Entradas</dt>
              <dd class="font-semibold text-income">{{ money(totals.income) }}</dd>
            </div>
            <div>
              <dt class="text-content-muted">Saídas</dt>
              <dd class="font-semibold text-expense">{{ money(totals.expense) }}</dd>
            </div>
            <div>
              <dt class="text-content-muted">Saldo</dt>
              <dd class="font-semibold" [class]="balanceNegative(totals.balance) ? 'text-expense' : 'text-income'">
                {{ money(totals.balance) }}
              </dd>
            </div>
          </dl>
        }
      </div>
    </app-panel>

    <app-panel>
      @switch (store.status()) {
        @case ('error') {
          <app-empty-state message="Não foi possível carregar os lançamentos.">
            <button type="button" appButton variant="secondary" (click)="store.refresh()">Tentar novamente</button>
          </app-empty-state>
        }
        @case ('loaded') {
          @if (store.items().length === 0) {
            <app-empty-state [icon]="emptyIcon" message="Nenhum lançamento no período." />
          } @else {
            <app-transactions-list [items]="store.items()" (edit)="openEdit($event)" (remove)="deleteTarget.set($event)" />

            <nav class="mt-4 flex flex-wrap items-center justify-between gap-3 text-sm text-content-muted" aria-label="Paginação">
              <p role="status">{{ store.range().from }}–{{ store.range().to }} de {{ store.range().total }}</p>
              <div class="flex items-center gap-2">
                <button type="button" appButton variant="secondary" aria-label="Página anterior" [disabled]="store.page() <= 1" (click)="store.goTo(store.page() - 1)">
                  <lucide-icon [img]="previous" [size]="18" aria-hidden="true" />
                </button>
                <span>{{ store.page() }}/{{ store.totalPages() }}</span>
                <button type="button" appButton variant="secondary" aria-label="Próxima página" [disabled]="store.page() >= store.totalPages()" (click)="store.goTo(store.page() + 1)">
                  <lucide-icon [img]="next" [size]="18" aria-hidden="true" />
                </button>
              </div>
            </nav>
          }
        }
        @default {
          <p role="status" aria-busy="true" class="py-10 text-center text-sm text-content-muted">Carregando lançamentos…</p>
        }
      }
    </app-panel>

    @if (dialogOpen()) {
      <app-transaction-dialog [transaction]="editTarget()" (saved)="store.refresh()" (closed)="closeDialog()" />
    }

    @if (deleteTarget(); as target) {
      <app-confirm-dialog
        title="Excluir lançamento?"
        [message]="'O lançamento “' + target.description + '” será removido.'"
        confirmLabel="Excluir"
        [busy]="deleting()"
        (confirmed)="confirmDelete(target)"
        (cancelled)="deleteTarget.set(null)"
      />
    }
  `,
})
export class TransactionsPage {
  protected readonly store = inject(TransactionsStore);
  private readonly api = inject(TransactionsApiService);
  private readonly currency = inject(CurrencyStore);
  private readonly toast = inject(ToastService);

  protected readonly plus = Plus;
  protected readonly previous = ChevronLeft;
  protected readonly next = ChevronRight;
  protected readonly emptyIcon = ArrowLeftRight;
  protected readonly filters: readonly { value: KindFilter; label: string }[] = [
    { value: 'ALL', label: 'Todos' },
    { value: 'INCOME', label: 'Entradas' },
    { value: 'EXPENSE', label: 'Saídas' },
  ];

  protected readonly dialogOpen = signal(false);
  protected readonly editTarget = signal<Transaction | null>(null);
  protected readonly deleteTarget = signal<Transaction | null>(null);
  protected readonly deleting = signal(false);

  protected money(value: string): string {
    return formatMoney(value, this.currency.selectedCurrency());
  }

  protected balanceNegative(value: string): boolean {
    return value.startsWith('-');
  }

  protected openCreate(): void {
    this.editTarget.set(null);
    this.dialogOpen.set(true);
  }

  protected openEdit(transaction: Transaction): void {
    this.editTarget.set(transaction);
    this.dialogOpen.set(true);
  }

  protected closeDialog(): void {
    this.dialogOpen.set(false);
    this.editTarget.set(null);
  }

  protected confirmDelete(transaction: Transaction): void {
    this.deleting.set(true);
    this.api.deleteTransaction(transaction.id).subscribe({
      next: () => {
        this.deleting.set(false);
        this.deleteTarget.set(null);
        this.toast.success('Lançamento excluído.');
        this.store.refresh();
      },
      error: (error: unknown) => {
        this.deleting.set(false);
        this.deleteTarget.set(null);
        this.toast.error(isApiError(error) ? error.message : 'Não foi possível excluir o lançamento.');
        this.store.refresh();
      },
    });
  }
}
