import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type { components } from '../../../core/api/generated/budgets';
import { API_BASE_URL } from '../../../core/http/api-base-url.token';

type Schemas = components['schemas'];

export type Budget = Schemas['Budget'];
export type BudgetProgress = Schemas['BudgetProgress'];
export type ProgressEntry = Schemas['ProgressEntry'];
export type BudgetStatus = Schemas['BudgetStatus'];
export type SaveBudgetRequest = Schemas['SaveBudgetRequest'];
export type CurrencyCode = Schemas['Currency'];

/** Cliente do contrato `budgets` (sem estado). */
@Injectable({ providedIn: 'root' })
export class BudgetApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  getBudget(currency: CurrencyCode, month: string): Observable<Budget> {
    return this.http.get<Budget>(`${this.baseUrl}/budgets/${currency}`, { params: new HttpParams().set('month', month) });
  }

  saveBudget(currency: CurrencyCode, request: SaveBudgetRequest): Observable<Budget> {
    return this.http.put<Budget>(`${this.baseUrl}/budgets/${currency}`, request);
  }

  getBudgetProgress(currency: CurrencyCode, month: string): Observable<BudgetProgress> {
    return this.http.get<BudgetProgress>(`${this.baseUrl}/budgets/${currency}/progress`, {
      params: new HttpParams().set('month', month),
    });
  }
}
