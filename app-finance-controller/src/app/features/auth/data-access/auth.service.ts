import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, defer, finalize, firstValueFrom, map, of, shareReplay, switchMap, tap } from 'rxjs';
import { AppStateReset } from '../../../core/state/app-state-reset';
import { PreferencesSync } from '../../../core/state/preferences-sync';
import { SessionStore } from '../../../core/state/session.store';
import { AuthApiService, AuthResponse } from './auth-api.service';

/** Fachada de autenticação: grava a sessão, restaura no arranque, renova e encerra. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(AuthApiService);
  private readonly session = inject(SessionStore);
  private readonly reset = inject(AppStateReset);
  private readonly preferences = inject(PreferencesSync);
  private readonly router = inject(Router);

  private refreshInFlight: Observable<string> | null = null;
  private restoreInFlight: Promise<boolean> | null = null;

  register(email: string, password: string): Observable<AuthResponse> {
    return this.api.registerUser({ email, password }).pipe(
      tap((response) => this.session.set(response)),
      switchMap((response) => this.preferences.load().pipe(map(() => response))),
    );
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.api.login({ email, password }).pipe(
      tap((response) => this.session.set(response)),
      switchMap((response) => this.preferences.load().pipe(map(() => response))),
    );
  }

  /** Renova o access token. Chamadas concorrentes compartilham a mesma requisição. */
  refresh(): Observable<string> {
    return defer(() => {
      if (this.refreshInFlight === null) {
        this.refreshInFlight = this.api.refreshToken().pipe(
          map((response) => response.accessToken),
          tap((token) => this.session.setAccessToken(token)),
          finalize(() => {
            this.refreshInFlight = null;
          }),
          shareReplay({ bufferSize: 1, refCount: false }),
        );
      }
      return this.refreshInFlight;
    });
  }

  /** Tenta restaurar a sessão pelo cookie de renovação. Nunca lança; resolve se há sessão. */
  restoreSession(): Promise<boolean> {
    this.restoreInFlight ??= this.doRestore().finally(() => {
      this.restoreInFlight = null;
    });
    return this.restoreInFlight;
  }

  /** Garante que a restauração inicial terminou (usado pelos guards). */
  async ensureInitialized(): Promise<void> {
    if (!this.session.initialized()) {
      await this.restoreSession();
    }
  }

  /** Encerra a sessão: logout best-effort, limpa o estado e vai a /entrar. */
  async logout(): Promise<void> {
    await firstValueFrom(this.api.logout().pipe(catchError(() => of(undefined))));
    this.reset.resetAll();
    await this.router.navigateByUrl('/entrar');
  }

  private async doRestore(): Promise<boolean> {
    try {
      const user = await firstValueFrom(
        this.refresh().pipe(switchMap(() => this.api.getCurrentUser())),
      );
      this.session.setUser(user);
      await firstValueFrom(this.preferences.load());
      return true;
    } catch {
      this.session.clear();
      return false;
    } finally {
      this.session.markInitialized();
    }
  }
}
