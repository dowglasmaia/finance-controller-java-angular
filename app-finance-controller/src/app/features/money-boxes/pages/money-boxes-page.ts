import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { LucideAngularModule, PiggyBank, Plus, Target } from 'lucide-angular';
import { isApiError } from '../../../core/http/api-error';
import { CurrencyStore } from '../../../core/state/currency.store';
import { formatMoney } from '../../../shared/money/format-money';
import { AppPanel } from '../../../shared/ui/app-panel';
import { ButtonDirective } from '../../../shared/ui/button';
import { ConfirmDialog } from '../../../shared/ui/confirm-dialog';
import { EmptyState } from '../../../shared/ui/empty-state';
import { KpiCard } from '../../../shared/ui/kpi-card';
import { PageHeader } from '../../../shared/ui/page-header';
import { ToastService } from '../../../shared/ui/toast.service';
import { percentOf } from '../../budget/budget-status';
import { EntryKind, MoneyBox, MoneyBoxEntry } from '../data-access/money-boxes-api.service';
import { MoneyBoxesStore } from '../data-access/money-boxes.store';
import { EntryDialog } from '../ui/entry-dialog';
import { GoalDialog } from '../ui/goal-dialog';
import { MoneyBoxCard } from '../ui/money-box-card';
import { MoneyBoxDetail } from '../ui/money-box-detail';
import { MoneyBoxDialog } from '../ui/money-box-dialog';

interface EntryTarget {
  readonly box: MoneyBox;
  readonly entry: MoneyBoxEntry | null;
  readonly kind: EntryKind;
}

/** `/cofrinhos`: resumo das reservas, meta geral, cartões por cofrinho, detalhe e diálogos. Só a moeda selecionada. */
@Component({
  selector: 'app-money-boxes-page',
  imports: [
    PageHeader,
    AppPanel,
    EmptyState,
    ButtonDirective,
    KpiCard,
    MoneyBoxCard,
    MoneyBoxDialog,
    MoneyBoxDetail,
    EntryDialog,
    GoalDialog,
    ConfirmDialog,
    LucideAngularModule,
  ],
  providers: [MoneyBoxesStore],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-page-header title="Cofrinhos" subtitle="Suas reservas e o progresso rumo às metas">
      <button actions type="button" appButton variant="secondary" [disabled]="store.status() !== 'loaded'" (click)="goalOpen.set(true)">
        <lucide-icon [img]="targetIcon" [size]="18" aria-hidden="true" />
        {{ store.summary()?.goal ? 'Editar meta geral' : 'Definir meta geral' }}
      </button>
      <button actions type="button" appButton (click)="openCreate()">
        <lucide-icon [img]="plus" [size]="18" aria-hidden="true" />
        Novo Cofrinho
      </button>
    </app-page-header>

    @switch (store.status()) {
      @case ('error') {
        <app-panel>
          <app-empty-state message="Não foi possível carregar os cofrinhos.">
            <button type="button" appButton variant="secondary" (click)="store.refresh()">Tentar novamente</button>
          </app-empty-state>
        </app-panel>
      }
      @case ('loaded') {
        @if (store.summary(); as summary) {
          <section aria-label="Resumo das reservas" class="mb-6 grid grid-cols-2 gap-3 lg:grid-cols-4">
            <app-kpi-card label="Saldo Atual" tone="reserve" [value]="total()" />
            <app-kpi-card label="Meta Total" [value]="goalText()" />
            <app-kpi-card label="Progresso" [value]="summary.goal ? percent() + '%' : '—'" />
            <app-kpi-card label="Reservas" tone="reserve" [value]="'' + summary.boxCount" />
          </section>

          @if (summary.goal) {
            <section class="mb-6 rounded-card border border-line bg-surface-raised p-4 shadow-card" aria-label="Progresso da meta geral">
              <div
                class="h-3 overflow-hidden rounded-full bg-surface-sunken"
                role="progressbar"
                aria-valuemin="0"
                aria-valuemax="100"
                [attr.aria-valuenow]="barPercent()"
                [attr.aria-label]="'Meta geral: ' + percent() + '% concluído'"
              >
                <div class="h-full rounded-full bg-reserve transition-[width] duration-fast ease-standard" [style.width.%]="barPercent()"></div>
              </div>
              <p class="mt-2 text-sm text-content">{{ percent() }}% concluído — {{ total() }} de {{ goalText() }}</p>
            </section>
          }
        }

        @if (store.boxes().length === 0) {
          <app-panel>
            <app-empty-state [icon]="piggy" message="Nenhum cofrinho ainda. Crie sua primeira reserva.">
              <button type="button" appButton (click)="openCreate()">Novo Cofrinho</button>
            </app-empty-state>
          </app-panel>
        } @else {
          <ul class="grid gap-4 sm:grid-cols-2 xl:grid-cols-3" aria-label="Cofrinhos">
            @for (box of store.boxes(); track box.id) {
              <li>
                <app-money-box-card
                  [box]="box"
                  [currency]="currency()"
                  (open)="store.openDetail($event.id)"
                  (edit)="openEdit($event)"
                  (remove)="deleteTarget.set($event)"
                  (deposit)="openEntry($event, 'DEPOSIT')"
                  (withdraw)="openEntry($event, 'WITHDRAWAL')"
                />
              </li>
            }
          </ul>
        }
      }
      @default {
        <p role="status" aria-busy="true" class="py-10 text-center text-sm text-content-muted">Carregando cofrinhos…</p>
      }
    }

    @if (store.detail(); as detail) {
      <app-money-box-detail
        [box]="detail"
        [currency]="currency()"
        (closed)="store.closeDetail()"
        (deposit)="openEntry(detail, 'DEPOSIT')"
        (withdraw)="openEntry(detail, 'WITHDRAWAL')"
        (editEntry)="openEntryEdit(detail, $event)"
      />
    }

    @if (boxDialogOpen()) {
      <app-money-box-dialog [box]="editTarget()" (closed)="closeBoxDialog()" />
    }

    @if (entryTarget(); as target) {
      <app-entry-dialog [box]="target.box" [entry]="target.entry" [initialKind]="target.kind" (closed)="entryTarget.set(null)" />
    }

    @if (goalOpen()) {
      <app-goal-dialog [current]="store.summary()?.goal" (closed)="goalOpen.set(false)" />
    }

    @if (deleteTarget(); as target) {
      <app-confirm-dialog
        title="Excluir cofrinho?"
        [message]="'O cofrinho “' + target.name + '” e todas as suas movimentações serão removidos.'"
        confirmLabel="Excluir"
        [busy]="deleting()"
        (confirmed)="confirmDelete(target)"
        (cancelled)="deleteTarget.set(null)"
      />
    }
  `,
})
export class MoneyBoxesPage {
  protected readonly store = inject(MoneyBoxesStore);
  private readonly currencyStore = inject(CurrencyStore);
  private readonly toast = inject(ToastService);

  protected readonly plus = Plus;
  protected readonly piggy = PiggyBank;
  protected readonly targetIcon = Target;

  protected readonly boxDialogOpen = signal(false);
  protected readonly editTarget = signal<MoneyBox | null>(null);
  protected readonly entryTarget = signal<EntryTarget | null>(null);
  protected readonly goalOpen = signal(false);
  protected readonly deleteTarget = signal<MoneyBox | null>(null);
  protected readonly deleting = signal(false);

  protected readonly currency = this.currencyStore.selectedCurrency;
  protected readonly total = computed(() => formatMoney(this.store.summary()?.total ?? '0', this.currency()));
  protected readonly goalText = computed(() => {
    const goal = this.store.summary()?.goal;
    return goal ? formatMoney(goal, this.currency()) : '—';
  });
  protected readonly percent = computed(() => percentOf(this.store.summary()?.progress));
  protected readonly barPercent = computed(() => Math.min(100, this.percent()));

  protected openCreate(): void {
    this.editTarget.set(null);
    this.boxDialogOpen.set(true);
  }

  protected openEdit(box: MoneyBox): void {
    this.editTarget.set(box);
    this.boxDialogOpen.set(true);
  }

  protected closeBoxDialog(): void {
    this.boxDialogOpen.set(false);
    this.editTarget.set(null);
  }

  protected openEntry(box: MoneyBox, kind: EntryKind): void {
    this.entryTarget.set({ box, entry: null, kind });
  }

  protected openEntryEdit(box: MoneyBox, entry: MoneyBoxEntry): void {
    this.entryTarget.set({ box, entry, kind: entry.kind });
  }

  protected confirmDelete(box: MoneyBox): void {
    this.deleting.set(true);
    this.store.deleteBox(box.id).subscribe({
      next: () => {
        this.deleting.set(false);
        this.deleteTarget.set(null);
        this.toast.success('Cofrinho excluído.');
      },
      error: (error: unknown) => {
        this.deleting.set(false);
        this.deleteTarget.set(null);
        if (isApiError(error) && error.status === 404) {
          this.store.refresh();
        }
        this.toast.error(isApiError(error) ? error.message : 'Não foi possível excluir o cofrinho.');
      },
    });
  }
}
