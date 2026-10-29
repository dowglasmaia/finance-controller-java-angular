import { Injectable, computed, effect, inject, signal, untracked } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { EMPTY, catchError, switchMap, tap } from 'rxjs';
import { CurrencyStore } from '../../../core/state/currency.store';
import { ReferenceMonthStore } from '../../../core/state/reference-month.store';
import { KindFilter, TransactionPage, TransactionsApiService } from './transactions-api.service';

export const PAGE_SIZE = 15;

export type TransactionsStatus = 'loading' | 'loaded' | 'error';

/**
 * Estado da lista de lançamentos da página (um por página aberta: nada fica em cache entre utilizadores). A consulta
 * depende do mês, da moeda, do filtro e da página; mudar qualquer um cancela a anterior e, exceto a página, volta
 * para a primeira. Depois de uma mutação, `refresh()` recarrega, e a página é ajustada se o total diminuir.
 */
@Injectable()
export class TransactionsStore {
  private readonly api = inject(TransactionsApiService);
  private readonly month = inject(ReferenceMonthStore);
  private readonly currency = inject(CurrencyStore);

  private readonly kindState = signal<KindFilter>('ALL');
  private readonly pageState = signal(1);
  private readonly tick = signal(0);
  private readonly statusState = signal<TransactionsStatus>('loading');
  private readonly dataState = signal<TransactionPage | null>(null);

  readonly kind = this.kindState.asReadonly();
  readonly page = this.pageState.asReadonly();
  readonly status = this.statusState.asReadonly();
  readonly data = this.dataState.asReadonly();

  readonly items = computed(() => this.dataState()?.items ?? []);
  readonly totals = computed(() => this.dataState()?.totals ?? null);
  readonly totalItems = computed(() => this.dataState()?.totalItems ?? 0);
  readonly totalPages = computed(() => this.dataState()?.totalPages ?? 1);
  /** `1–15 de N` (ou `0 de 0`). */
  readonly range = computed(() => {
    const total = this.totalItems();
    if (total === 0) {
      return { from: 0, to: 0, total };
    }
    const from = (this.pageState() - 1) * PAGE_SIZE + 1;
    return { from, to: Math.min(from + this.items().length - 1, total), total };
  });

  private readonly query = computed(() => ({
    month: this.month.monthKey(),
    currency: this.currency.selectedCurrency(),
    type: this.kindState(),
    page: this.pageState(),
    size: PAGE_SIZE,
    tick: this.tick(),
  }));

  constructor() {
    // mês ou moeda diferentes: volta à primeira página
    effect(() => {
      this.month.monthKey();
      this.currency.selectedCurrency();
      untracked(() => this.pageState.set(1));
    });

    toObservable(this.query)
      .pipe(
        tap(() => this.statusState.set('loading')),
        switchMap(({ tick: _tick, ...query }) =>
          this.api.listTransactions(query).pipe(
            catchError(() => {
              this.statusState.set('error');
              return EMPTY;
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((result) => {
        // a página pode ter deixado de existir (exclusão do último item dela)
        if (result.items.length === 0 && result.page > 1 && result.totalPages < result.page) {
          this.pageState.set(result.totalPages);
          return;
        }
        this.dataState.set(result);
        this.statusState.set('loaded');
      });
  }

  setKind(kind: KindFilter): void {
    if (kind !== this.kindState()) {
      this.kindState.set(kind);
      this.pageState.set(1);
    }
  }

  goTo(page: number): void {
    this.pageState.set(Math.min(Math.max(1, page), this.totalPages()));
  }

  /** Recarrega a consulta atual (após salvar/excluir ou "Tentar novamente"). */
  refresh(): void {
    this.tick.update((n) => n + 1);
  }
}
