import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type { components } from '../../../core/api/generated/overview';
import { API_BASE_URL } from '../../../core/http/api-base-url.token';

type Schemas = components['schemas'];

export type MonthlyOverview = Schemas['MonthlyOverview'];
export type Bar = Schemas['Bar'];
export type CategoryExpense = Schemas['CategoryExpense'];
export type OverviewTransaction = Schemas['OverviewTransaction'];
export type CurrencyCode = Schemas['Currency'];

/** Cliente do contrato `overview` (sem estado). */
@Injectable({ providedIn: 'root' })
export class OverviewApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  getMonthlyOverview(month: string, currency: CurrencyCode): Observable<MonthlyOverview> {
    return this.http.get<MonthlyOverview>(`${this.baseUrl}/overview`, {
      params: new HttpParams().set('month', month).set('currency', currency),
    });
  }
}
