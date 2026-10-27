import { DOCUMENT } from '@angular/common';
import { DestroyRef, Injectable, computed, effect, inject, signal } from '@angular/core';
import { readStorage, writeStorage } from './safe-storage';

export type ThemeMode = 'SYSTEM' | 'LIGHT' | 'DARK';

const STORAGE_KEY = 'fc.theme';
const DARK_QUERY = '(prefers-color-scheme: dark)';

function isThemeMode(value: unknown): value is ThemeMode {
  return value === 'SYSTEM' || value === 'LIGHT' || value === 'DARK';
}

function loadMode(): ThemeMode {
  const stored = readStorage(STORAGE_KEY);
  return isThemeMode(stored) ? stored : 'SYSTEM';
}

/**
 * Dono do tema. Aplica a classe `dark` em `<html>`.
 * Ponto de extensão (F10): sincronizar com as preferências do servidor após o login.
 */
@Injectable({ providedIn: 'root' })
export class ThemeStore {
  private readonly document = inject(DOCUMENT);
  private readonly mode = signal<ThemeMode>(loadMode());
  private readonly systemDark = signal(false);

  readonly themeMode = this.mode.asReadonly();
  readonly effectiveDark = computed(() => {
    const mode = this.mode();
    return mode === 'DARK' || (mode === 'SYSTEM' && this.systemDark());
  });

  constructor() {
    this.watchSystemPreference();
    effect(() => {
      this.document.documentElement.classList.toggle('dark', this.effectiveDark());
    });
  }

  setMode(mode: ThemeMode): void {
    this.mode.set(mode);
    writeStorage(STORAGE_KEY, mode);
  }

  /** dark → light; qualquer outro → dark. */
  toggle(): void {
    this.setMode(this.mode() === 'DARK' ? 'LIGHT' : 'DARK');
  }

  private watchSystemPreference(): void {
    const view = this.document.defaultView;
    if (!view || typeof view.matchMedia !== 'function') {
      return;
    }
    const query = view.matchMedia(DARK_QUERY);
    this.systemDark.set(query.matches);
    const onChange = (event: MediaQueryListEvent): void => this.systemDark.set(event.matches);
    query.addEventListener('change', onChange);
    inject(DestroyRef).onDestroy(() => query.removeEventListener('change', onChange));
  }
}
