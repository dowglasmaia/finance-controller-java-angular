import { Injectable, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { EMPTY, catchError, switchMap, tap } from 'rxjs';
import { CurrencyStore } from '../../../core/state/currency.store';
import { ReferenceMonthStore } from '../../../core/state/reference-month.store';
import { Report, ReportPeriod, ReportsApiService } from './reports-api.service';

export type ReportStatus = 'loading' | 'loaded' | 'error';

/** Dia do mês de referência enviado como âncora do período (P-05, regra mantida da origem). */
export const ANCHOR_DAY = '15';

/**
 * Estado da página de relatórios (um por página aberta): uma chamada por período, mês de referência e moeda. Trocar
 * qualquer um cancela a consulta anterior (`switchMap`) e limpa o dado anterior, para nunca mostrar números de outro
 * período sob o título do novo. O período começa em Mês e não é persistido.
 */
@Injectable()
export class ReportsStore {
  private readonly api = inject(ReportsApiService);
  private readonly month = inject(ReferenceMonthStore);
  private readonly currency = inject(CurrencyStore);

  private readonly tick = signal(0);
  private readonly periodState = signal<ReportPeriod>('MONTH');
  private readonly statusState = signal<ReportStatus>('loading');
  private readonly dataState = signal<Report | null>(null);

  readonly period = this.periodState.asReadonly();
  readonly status = this.statusState.asReadonly();
  readonly data = this.dataState.asReadonly();

  private readonly query = computed(() => ({
    period: this.periodState(),
    anchorDate: `${this.month.monthKey()}-${ANCHOR_DAY}`,
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
        switchMap(({ period, anchorDate, currency }) =>
          this.api.getReport(period, anchorDate, currency).pipe(
            catchError(() => {
              this.statusState.set('error');
              return EMPTY;
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((report) => {
        this.dataState.set(report);
        this.statusState.set('loaded');
      });
  }

  selectPeriod(period: ReportPeriod): void {
    this.periodState.set(period);
  }

  refresh(): void {
    this.tick.update((n) => n + 1);
  }
}
