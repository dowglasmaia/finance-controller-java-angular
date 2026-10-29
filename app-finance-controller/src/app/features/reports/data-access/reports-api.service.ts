import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type { components } from '../../../core/api/generated/reports';
import { API_BASE_URL } from '../../../core/http/api-base-url.token';

type Schemas = components['schemas'];

export type Report = Schemas['Report'];
export type ReportPeriod = Schemas['ReportPeriod'];
export type ReportBar = Schemas['ReportBar'];
export type CategoryShare = Schemas['CategoryShare'];
export type MonthlySummaryItem = Schemas['MonthlySummaryItem'];
export type CurrencyCode = Schemas['Currency'];

/** Cliente do contrato `reports` (sem estado). */
@Injectable({ providedIn: 'root' })
export class ReportsApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  getReport(period: ReportPeriod, anchorDate: string, currency: CurrencyCode): Observable<Report> {
    return this.http.get<Report>(`${this.baseUrl}/reports`, {
      params: new HttpParams().set('period', period).set('anchorDate', anchorDate).set('currency', currency),
    });
  }
}
