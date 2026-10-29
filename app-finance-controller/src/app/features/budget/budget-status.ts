import { CircleAlert, CircleCheck, LucideIconData, Minus, TriangleAlert } from 'lucide-angular';
import type { BudgetStatus } from './data-access/budget-api.service';

export interface StatusPresentation {
  readonly label: string;
  readonly icon: LucideIconData;
  /** Classe de cor do texto/ícone (tokens semânticos). */
  readonly textClass: string;
  /** Classe de cor da barra. */
  readonly barClass: string;
}

/** Cada estado tem texto e ícone além da cor (acessibilidade). */
export const STATUS_PRESENTATION: Record<BudgetStatus, StatusPresentation> = {
  OK: { label: 'Dentro da meta', icon: CircleCheck, textClass: 'text-income', barClass: 'bg-income' },
  WARNING: { label: 'Perto da meta', icon: TriangleAlert, textClass: 'text-warning', barClass: 'bg-warning' },
  EXCEEDED: { label: 'Meta estourada', icon: CircleAlert, textClass: 'text-expense', barClass: 'bg-expense' },
  NONE: { label: 'Sem meta definida', icon: Minus, textClass: 'text-content-muted', barClass: 'bg-content-muted' },
};

/** Percentual inteiro da fração decimal do servidor (`"0.8025"` → 80); a barra é limitada a 100. */
export function percentOf(fraction: string | undefined): number {
  if (fraction === undefined) {
    return 0;
  }
  const value = Number(fraction);
  return Number.isFinite(value) ? Math.round(value * 100) : 0;
}
