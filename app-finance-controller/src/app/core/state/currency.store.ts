import { Injectable, signal } from '@angular/core';
import { readStorage, writeStorage } from './safe-storage';

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
 * Ponto de extensão (F10): sincronizar com as preferências do servidor após o login.
 */
@Injectable({ providedIn: 'root' })
export class CurrencyStore {
  private readonly state = signal<Currency>(loadCurrency());

  readonly selectedCurrency = this.state.asReadonly();

  select(currency: Currency): void {
    this.state.set(currency);
    writeStorage(STORAGE_KEY, currency);
  }
}
