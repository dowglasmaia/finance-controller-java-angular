import { Injectable, signal } from '@angular/core';
import { readStorage, removeStorage, writeStorage } from './safe-storage';

export type Currency = 'BRL' | 'EUR';

const STORAGE_KEY = 'fc.currency';

function isCurrency(value: unknown): value is Currency {
  return value === 'BRL' || value === 'EUR';
}

function loadCurrency(): Currency {
  const stored = readStorage(STORAGE_KEY);
  return isCurrency(stored) ? stored : 'BRL';
}

/**
 * Dono da moeda selecionada. Persistência local em `localStorage`.
 * A `PreferencesSync` sincroniza com o servidor por utilizador; ao sair, volta ao padrão e limpa o dispositivo.
 */
@Injectable({ providedIn: 'root' })
export class CurrencyStore {
  private readonly state = signal<Currency>(loadCurrency());

  readonly selectedCurrency = this.state.asReadonly();

  select(currency: Currency): void {
    this.state.set(currency);
    writeStorage(STORAGE_KEY, currency);
  }

  /** Logout: padrão `BRL` e nada guardado no dispositivo. */
  resetToDefault(): void {
    this.state.set('BRL');
    removeStorage(STORAGE_KEY);
  }
}
