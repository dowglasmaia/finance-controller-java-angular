import { Injectable, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { EMPTY, catchError, switchMap, tap } from 'rxjs';
import { CurrencyStore } from '../../../core/state/currency.store';
import { ReferenceMonthStore } from '../../../core/state/reference-month.store';
import { MonthlyOverview, OverviewApiService } from './overview-api.service';

export type OverviewStatus = 'loading' | 'loaded' | 'error';

/**
 * Estado do dashboard (um por página aberta): uma chamada por mês e moeda. Trocar o mês ou a moeda cancela a anterior
 * (`switchMap`) e a resposta de um mês antigo nunca sobrescreve a do mês atual. Durante a recarga o dado anterior é
 * limpo, para não mostrar números de outro mês sob o título do novo.
 */
@Injectable()
export class OverviewStore {
  private readonly api = inject(OverviewApiService);
  private readonly month = inject(ReferenceMonthStore);
  private readonly currency = inject(CurrencyStore);

  private readonly tick = signal(0);
  private readonly statusState = signal<OverviewStatus>('loading');
  private readonly dataState = signal<MonthlyOverview | null>(null);

  readonly status = this.statusState.asReadonly();
  readonly data = this.dataState.asReadonly();

  private readonly query = computed(() => ({
    month: this.month.monthKey(),
    currency: this.currency.selectedCurrency(),
    tick: this.tick(),
  }));

  constructor() {
    toObservable(this.query)
      .pipe(
        tap(() => {
          this.statusState.set('loading');
          this.dataState.set(null);
        }),
        switchMap(({ month, currency }) =>
          this.api.getMonthlyOverview(month, currency).pipe(
            catchError(() => {
              this.statusState.set('error');
              return EMPTY;
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((overview) => {
        this.dataState.set(overview);
        this.statusState.set('loaded');
      });
  }

  /** "Tentar novamente". */
  refresh(): void {
    this.tick.update((n) => n + 1);
  }
}
