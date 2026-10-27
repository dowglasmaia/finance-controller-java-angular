import { Injectable, computed, signal } from '@angular/core';
import type { components } from '../api/generated/identity';

export type SessionUser = components['schemas']['UserResponse'];

export interface Session {
  readonly user: SessionUser;
  readonly accessToken: string;
}

/** Dono da sessão. O access token vive só em memória (nunca storage nem URL). */
@Injectable({ providedIn: 'root' })
export class SessionStore {
  private readonly userState = signal<SessionUser | null>(null);
  private readonly tokenState = signal<string | null>(null);
  private readonly initializedState = signal(false);

  readonly user = this.userState.asReadonly();
  readonly accessToken = this.tokenState.asReadonly();
  readonly initialized = this.initializedState.asReadonly();
  readonly isAuthenticated = computed(() => this.userState() !== null && this.tokenState() !== null);

  set(session: Session): void {
    this.userState.set(session.user);
    this.tokenState.set(session.accessToken);
  }

  setAccessToken(accessToken: string): void {
    this.tokenState.set(accessToken);
  }

  setUser(user: SessionUser): void {
    this.userState.set(user);
  }

  markInitialized(): void {
    this.initializedState.set(true);
  }

  clear(): void {
    this.userState.set(null);
    this.tokenState.set(null);
  }
}
