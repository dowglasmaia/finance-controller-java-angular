import { Injectable, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { EMPTY, Observable, catchError, forkJoin, switchMap, tap } from 'rxjs';
import { CurrencyStore } from '../../../core/state/currency.store';
import { ReferenceMonthStore } from '../../../core/state/reference-month.store';
import { Budget, BudgetApiService, BudgetProgress, SaveBudgetRequest } from './budget-api.service';

export type BudgetStatusState = 'loading' | 'loaded' | 'error';

export interface BudgetData {
  readonly budget: Budget;
  readonly progress: BudgetProgress;
}

/**
 * Estado da página de metas (um por página aberta). A configuração e o progresso dependem do mês e da moeda: trocar
 * qualquer um cancela a consulta anterior e recarrega os dois. Depois de gravar, recarrega para o progresso refletir
 * os novos limites.
 */
@Injectable()
export class BudgetStore {
  private readonly api = inject(BudgetApiService);
  private readonly month = inject(ReferenceMonthStore);
  private readonly currencyStore = inject(CurrencyStore);

  private readonly tick = signal(0);
  private readonly statusState = signal<BudgetStatusState>('loading');
  private readonly dataState = signal<BudgetData | null>(null);

  readonly status = this.statusState.asReadonly();
  readonly data = this.dataState.asReadonly();

  private readonly query = computed(() => ({
    month: this.month.monthKey(),
    currency: this.currencyStore.selectedCurrency(),
    tick: this.tick(),
  }));

  constructor() {
    toObservable(this.query)
      .pipe(
        tap(() => this.statusState.set('loading')),
        switchMap(({ month, currency }) =>
          forkJoin({
            budget: this.api.getBudget(currency, month),
            progress: this.api.getBudgetProgress(currency, month),
          }).pipe(
            catchError(() => {
              this.statusState.set('error');
              return EMPTY;
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((data) => {
        this.dataState.set(data);
        this.statusState.set('loaded');
      });
  }

  /** Grava tudo de uma vez; sucesso recarrega, falha não altera nada (os valores digitados ficam na página). */
  save(request: SaveBudgetRequest): Observable<Budget> {
    return this.api.saveBudget(this.currencyStore.selectedCurrency(), request).pipe(tap(() => this.refresh()));
  }

  refresh(): void {
    this.tick.update((n) => n + 1);
  }
}
