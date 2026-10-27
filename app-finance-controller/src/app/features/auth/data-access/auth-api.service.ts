import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type { components, operations } from '../../../core/api/generated/identity';
import { API_BASE_URL } from '../../../core/http/api-base-url.token';

type Schemas = components['schemas'];
type Body<T extends keyof operations, S extends keyof operations[T]['responses']> = operations[T]['responses'][S] extends {
  content: { 'application/json': infer B };
}
  ? B
  : never;

export type RegisterRequest = Schemas['RegisterRequest'];
export type LoginRequest = Schemas['LoginRequest'];
export type AuthResponse = Body<'login', 200>;
export type AccessTokenResponse = Body<'refreshToken', 200>;
export type UserResponse = Body<'getCurrentUser', 200>;

/** Cliente do contrato `identity`. `withCredentials` apenas nas chamadas que usam o cookie de renovação. */
@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  registerUser(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/auth/register`, request, { withCredentials: true });
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/auth/login`, request, { withCredentials: true });
  }

  refreshToken(): Observable<AccessTokenResponse> {
    return this.http.post<AccessTokenResponse>(`${this.baseUrl}/auth/refresh`, null, { withCredentials: true });
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/auth/logout`, null, { withCredentials: true });
  }

  getCurrentUser(): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.baseUrl}/auth/me`);
  }
}
