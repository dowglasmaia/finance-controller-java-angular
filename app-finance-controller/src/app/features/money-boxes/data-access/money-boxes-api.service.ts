import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type { components as BoxContract } from '../../../core/api/generated/money-boxes';
import type { components as ReserveContract } from '../../../core/api/generated/reserves';
import { API_BASE_URL } from '../../../core/http/api-base-url.token';

export type MoneyBox = BoxContract['schemas']['MoneyBox'];
export type MoneyBoxEntry = BoxContract['schemas']['MoneyBoxEntry'];
export type EntryKind = BoxContract['schemas']['MoneyBoxEntryKind'];
export type SaveMoneyBoxRequest = BoxContract['schemas']['SaveMoneyBoxRequest'];
export type SaveEntryRequest = BoxContract['schemas']['SaveMoneyBoxEntryRequest'];
export type CurrencyCode = BoxContract['schemas']['Currency'];
export type ReservesSummary = ReserveContract['schemas']['ReservesSummary'];

/** Cliente dos contratos `money-boxes` e `reserves` (sem estado). */
@Injectable({ providedIn: 'root' })
export class MoneyBoxesApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  listMoneyBoxes(currency: CurrencyCode): Observable<MoneyBox[]> {
    return this.http.get<MoneyBox[]>(`${this.baseUrl}/money-boxes`, { params: new HttpParams().set('currency', currency) });
  }

  createMoneyBox(request: SaveMoneyBoxRequest): Observable<MoneyBox> {
    return this.http.post<MoneyBox>(`${this.baseUrl}/money-boxes`, request);
  }

  updateMoneyBox(id: string, request: SaveMoneyBoxRequest): Observable<MoneyBox> {
    return this.http.put<MoneyBox>(`${this.baseUrl}/money-boxes/${id}`, request);
  }

  deleteMoneyBox(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/money-boxes/${id}`);
  }

  listMoneyBoxEntries(id: string): Observable<MoneyBoxEntry[]> {
    return this.http.get<MoneyBoxEntry[]>(`${this.baseUrl}/money-boxes/${id}/entries`);
  }

  createMoneyBoxEntry(id: string, request: SaveEntryRequest): Observable<MoneyBoxEntry> {
    return this.http.post<MoneyBoxEntry>(`${this.baseUrl}/money-boxes/${id}/entries`, request);
  }

  updateMoneyBoxEntry(id: string, entryId: string, request: SaveEntryRequest): Observable<MoneyBoxEntry> {
    return this.http.put<MoneyBoxEntry>(`${this.baseUrl}/money-boxes/${id}/entries/${entryId}`, request);
  }

  deleteMoneyBoxEntry(id: string, entryId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/money-boxes/${id}/entries/${entryId}`);
  }

  getReservesSummary(currency: CurrencyCode): Observable<ReservesSummary> {
    return this.http.get<ReservesSummary>(`${this.baseUrl}/reserves/${currency}/summary`);
  }

  saveReserveGoal(currency: CurrencyCode, amount: string): Observable<ReservesSummary> {
    return this.http.put<ReservesSummary>(`${this.baseUrl}/reserves/${currency}/goal`, { amount });
  }
}
