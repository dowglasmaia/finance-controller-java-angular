import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type { components } from '../api/generated/preferences';
import { API_BASE_URL } from '../http/api-base-url.token';

export type Preferences = components['schemas']['Preferences'];

/** Cliente do contrato `preferences` (sem estado). */
@Injectable({ providedIn: 'root' })
export class PreferencesApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  getPreferences(): Observable<Preferences> {
    return this.http.get<Preferences>(`${this.baseUrl}/preferences`);
  }

  savePreferences(preferences: Preferences): Observable<Preferences> {
    return this.http.put<Preferences>(`${this.baseUrl}/preferences`, preferences);
  }
}
