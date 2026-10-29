import type { BudgetAlert } from './data-access/transactions-api.service';

export interface AlertToast {
  readonly kind: 'error' | 'warning';
  readonly message: string;
}

/** Texto e tipo do toast do alerta de meta, na prioridade da origem (DESC-BUD-07). */
export function budgetAlertToast(alert: BudgetAlert): AlertToast {
  const exceeded = alert.status === 'EXCEEDED';
  if (alert.scope === 'CATEGORY') {
    const name = alert.categoryName ?? 'categoria';
    return exceeded
      ? { kind: 'error', message: `Meta de ${name} estourada!` }
      : { kind: 'warning', message: `Você está perto da meta de ${name}.` };
  }
  return exceeded
    ? { kind: 'error', message: 'Meta mensal estourada!' }
    : { kind: 'warning', message: 'Você já usou 80% da sua meta mensal.' };
}
