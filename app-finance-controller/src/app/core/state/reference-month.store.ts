import { Injectable, computed, signal } from '@angular/core';

function firstDayOfCurrentMonth(): Date {
  const now = new Date();
  return new Date(now.getFullYear(), now.getMonth(), 1);
}

function capitalize(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1);
}

/** Dono do mês de referência. Sem persistência: ao recarregar volta ao mês atual. */
@Injectable({ providedIn: 'root' })
export class ReferenceMonthStore {
  private readonly state = signal<Date>(firstDayOfCurrentMonth());
  private readonly formatter = new Intl.DateTimeFormat('pt-BR', { month: 'long', year: 'numeric' });

  readonly referenceMonth = this.state.asReadonly();

  /** Rótulo no formato "Junho 2026". */
  readonly label = computed(() => {
    const parts = this.formatter.formatToParts(this.state());
    const month = parts.find((part) => part.type === 'month')?.value ?? '';
    const year = parts.find((part) => part.type === 'year')?.value ?? '';
    return `${capitalize(month)} ${year}`.trim();
  });

  /** Mês no formato `YYYY-MM`, como o contrato espera. */
  readonly monthKey = computed(() => {
    const date = this.state();
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
  });

  previous(): void {
    this.state.update((date) => new Date(date.getFullYear(), date.getMonth() - 1, 1));
  }

  next(): void {
    this.state.update((date) => new Date(date.getFullYear(), date.getMonth() + 1, 1));
  }

  reset(): void {
    this.state.set(firstDayOfCurrentMonth());
  }
}
