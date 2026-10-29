import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type { components } from '../../../core/api/generated/transactions';
import { API_BASE_URL } from '../../../core/http/api-base-url.token';

type Schemas = components['schemas'];

export type Transaction = Schemas['Transaction'];
export type TransactionKind = Schemas['TransactionKind'];
export type TransactionPage = Schemas['TransactionPage'];
export type TransactionRequest = Schemas['TransactionRequest'];
export type TransactionResult = Schemas['TransactionResult'];
export type BudgetAlert = Schemas['BudgetAlert'];
export type CurrencyCode = Schemas['Currency'];
export type KindFilter = 'ALL' | TransactionKind;

export interface ListQuery {
  readonly month: string;
  readonly currency: CurrencyCode;
  readonly type: KindFilter;
  readonly page: number;
  readonly size: number;
}

/** Cliente do contrato `transactions` (sem estado). `getTransaction` existe no contrato e não é usada: a lista traz tudo. */
@Injectable({ providedIn: 'root' })
export class TransactionsApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  listTransactions(query: ListQuery): Observable<TransactionPage> {
    const params = new HttpParams()
      .set('month', query.month)
      .set('currency', query.currency)
      .set('type', query.type)
      .set('page', query.page)
      .set('size', query.size);
    return this.http.get<TransactionPage>(`${this.baseUrl}/transactions`, { params });
  }

  createTransaction(request: TransactionRequest): Observable<TransactionResult> {
    return this.http.post<TransactionResult>(`${this.baseUrl}/transactions`, request);
  }

  updateTransaction(transactionId: string, request: TransactionRequest): Observable<TransactionResult> {
    return this.http.put<TransactionResult>(`${this.baseUrl}/transactions/${transactionId}`, request);
  }

  deleteTransaction(transactionId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/transactions/${transactionId}`);
  }
}
