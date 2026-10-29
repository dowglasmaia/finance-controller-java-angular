import { Injectable, inject } from '@angular/core';
import { CurrencyStore } from './currency.store';
import { ReferenceMonthStore } from './reference-month.store';
import { SessionStore } from './session.store';
import { ThemeStore } from './theme.store';

/**
 * Limpa o estado em memória (logout / sessão expirada).
 * Ponto de extensão: funcionalidades registram callbacks (ex.: descartar consultas em cache).
 * Moeda e tema voltam ao padrão (BRL e tema escuro) e saem do dispositivo: pertencem ao utilizador e vêm do servidor
 * no próximo login (F10).
 */
@Injectable({ providedIn: 'root' })
export class AppStateReset {
  private readonly session = inject(SessionStore);
  private readonly referenceMonth = inject(ReferenceMonthStore);
  private readonly currency = inject(CurrencyStore);
  private readonly theme = inject(ThemeStore);
  private readonly callbacks = new Set<() => void>();

  /** Registra um callback de limpeza; devolve a função que o remove. */
  register(callback: () => void): () => void {
    this.callbacks.add(callback);
    return () => this.callbacks.delete(callback);
  }

  resetAll(): void {
    this.session.clear();
    this.referenceMonth.reset();
    this.currency.resetToDefault();
    this.theme.resetToDefault();
    for (const callback of this.callbacks) {
      callback();
    }
  }
}
