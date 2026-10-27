import { Injectable, inject } from '@angular/core';
import { ReferenceMonthStore } from './reference-month.store';
import { SessionStore } from './session.store';

/**
 * Limpa o estado em memória (logout / sessão expirada).
 * Ponto de extensão: funcionalidades registram callbacks (ex.: descartar consultas em cache).
 * Moeda e tema são preferências do dispositivo e não são resetadas (F10 trará a sincronização por utilizador).
 */
@Injectable({ providedIn: 'root' })
export class AppStateReset {
  private readonly session = inject(SessionStore);
  private readonly referenceMonth = inject(ReferenceMonthStore);
  private readonly callbacks = new Set<() => void>();

  /** Registra um callback de limpeza; devolve a função que o remove. */
  register(callback: () => void): () => void {
    this.callbacks.add(callback);
    return () => this.callbacks.delete(callback);
  }

  resetAll(): void {
    this.session.clear();
    this.referenceMonth.reset();
    for (const callback of this.callbacks) {
      callback();
    }
  }
}
