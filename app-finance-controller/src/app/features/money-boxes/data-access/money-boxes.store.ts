import { Injectable, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { EMPTY, Observable, catchError, forkJoin, of, switchMap, tap } from 'rxjs';
import { CurrencyStore } from '../../../core/state/currency.store';
import {
  MoneyBox,
  MoneyBoxEntry,
  MoneyBoxesApiService,
  ReservesSummary,
  SaveEntryRequest,
  SaveMoneyBoxRequest,
} from './money-boxes-api.service';

export type LoadStatus = 'loading' | 'loaded' | 'error';

/**
 * Estado da página de cofrinhos (um por página aberta). A lista e o resumo dependem só da moeda: trocá-la cancela a
 * consulta anterior e mostra o carregando. Depois de qualquer gravação a lista e o resumo recarregam em silêncio
 * (sem esconder a tela), e o histórico do cofrinho aberto também. Os valores calculados (saldo, progresso, total) vêm
 * do servidor; o cliente só os exibe.
 */
@Injectable()
export class MoneyBoxesStore {
  private readonly api = inject(MoneyBoxesApiService);
  private readonly currencyStore = inject(CurrencyStore);

  private readonly tick = signal(0);
  private readonly statusState = signal<LoadStatus>('loading');
  private readonly boxesState = signal<readonly MoneyBox[]>([]);
  private readonly summaryState = signal<ReservesSummary | null>(null);
  private shownCurrency = this.currencyStore.selectedCurrency();

  private readonly detailIdState = signal<string | null>(null);
  private readonly entriesTick = signal(0);
  private readonly entriesStatusState = signal<LoadStatus>('loading');
  private readonly entriesState = signal<readonly MoneyBoxEntry[]>([]);

  readonly status = this.statusState.asReadonly();
  readonly boxes = this.boxesState.asReadonly();
  readonly summary = this.summaryState.asReadonly();
  readonly detailId = this.detailIdState.asReadonly();
  readonly entries = this.entriesState.asReadonly();
  readonly entriesStatus = this.entriesStatusState.asReadonly();
  /** O cofrinho aberto no detalhe, sempre na versão mais recente da lista. */
  readonly detail = computed(() => this.boxesState().find((box) => box.id === this.detailIdState()) ?? null);

  constructor() {
    toObservable(computed(() => ({ currency: this.currencyStore.selectedCurrency(), tick: this.tick() })))
      .pipe(
        tap(({ currency }) => {
          if (currency !== this.shownCurrency || this.summaryState() === null) {
            this.shownCurrency = currency;
            this.statusState.set('loading');
            this.boxesState.set([]);
            this.summaryState.set(null);
          }
        }),
        switchMap(({ currency }) =>
          forkJoin({ boxes: this.api.listMoneyBoxes(currency), summary: this.api.getReservesSummary(currency) }).pipe(
            catchError(() => {
              if (this.summaryState() === null) {
                this.statusState.set('error');
              }
              return EMPTY;
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe(({ boxes, summary }) => {
        this.boxesState.set(boxes);
        this.summaryState.set(summary);
        this.statusState.set('loaded');
      });

    toObservable(computed(() => ({ id: this.detailIdState(), tick: this.entriesTick() })))
      .pipe(
        tap(({ id }) => {
          if (id === null) {
            this.entriesState.set([]);
          }
        }),
        switchMap(({ id }) =>
          id === null
            ? of(null)
            : this.api.listMoneyBoxEntries(id).pipe(
                catchError(() => {
                  this.entriesStatusState.set('error');
                  return EMPTY;
                }),
              ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((entries) => {
        if (entries !== null) {
          this.entriesState.set(entries);
          this.entriesStatusState.set('loaded');
        }
      });
  }

  openDetail(id: string): void {
    this.entriesStatusState.set('loading');
    this.entriesState.set([]);
    this.detailIdState.set(id);
  }

  closeDetail(): void {
    this.detailIdState.set(null);
  }

  refresh(): void {
    this.tick.update((n) => n + 1);
  }

  reloadEntries(): void {
    this.entriesTick.update((n) => n + 1);
  }

  createBox(request: SaveMoneyBoxRequest): Observable<MoneyBox> {
    return this.api.createMoneyBox(request).pipe(tap(() => this.refresh()));
  }

  updateBox(id: string, request: SaveMoneyBoxRequest): Observable<MoneyBox> {
    return this.api.updateMoneyBox(id, request).pipe(tap(() => this.refresh()));
  }

  deleteBox(id: string): Observable<void> {
    return this.api.deleteMoneyBox(id).pipe(
      tap(() => {
        if (this.detailIdState() === id) {
          this.closeDetail();
        }
        this.refresh();
      }),
    );
  }

  createEntry(boxId: string, request: SaveEntryRequest): Observable<MoneyBoxEntry> {
    return this.api.createMoneyBoxEntry(boxId, request).pipe(tap(() => this.changed()));
  }

  updateEntry(boxId: string, entryId: string, request: SaveEntryRequest): Observable<MoneyBoxEntry> {
    return this.api.updateMoneyBoxEntry(boxId, entryId, request).pipe(tap(() => this.changed()));
  }

  deleteEntry(boxId: string, entryId: string): Observable<void> {
    return this.api.deleteMoneyBoxEntry(boxId, entryId).pipe(tap(() => this.changed()));
  }

  saveGoal(amount: string): Observable<ReservesSummary> {
    return this.api.saveReserveGoal(this.currencyStore.selectedCurrency(), amount).pipe(
      tap((summary) => this.summaryState.set(summary)),
    );
  }

  private changed(): void {
    this.refresh();
    this.reloadEntries();
  }
}
