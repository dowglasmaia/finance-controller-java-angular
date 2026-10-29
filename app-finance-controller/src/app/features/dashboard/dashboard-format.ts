import { LucideIconData, Minus, TrendingDown, TrendingUp } from 'lucide-angular';
import type { KpiDelta } from '../../shared/ui/kpi-card';

/** Saudação por hora do dia (DESC-DSH-01): menos de 12h `Bom dia`, menos de 18h `Boa tarde`, senão `Boa noite`. */
export function greetingFor(hour: number): string {
  if (hour < 12) {
    return 'Bom dia';
  }
  return hour < 18 ? 'Boa tarde' : 'Boa noite';
}

function toPtBr(decimal: string): string {
  return decimal.replace('.', ',');
}

/**
 * Variação sobre o mês anterior como o servidor a entrega (`"25.0"`, `"-3.2"`, `"0"`; ausente sem base). Para saídas
 * (`lowerIsBetter`) gastar menos é a boa notícia. Sem base não há variação para mostrar.
 */
export function deltaFor(raw: string | undefined, lowerIsBetter: boolean): KpiDelta | null {
  if (raw === undefined) {
    return null;
  }
  const value = Number(raw);
  if (value === 0 || Number.isNaN(value)) {
    return { text: '0%', tone: 'neutral', icon: Minus, description: 'sem variação sobre o mês anterior' };
  }
  const up = value > 0;
  const good = up !== lowerIsBetter;
  const icon: LucideIconData = up ? TrendingUp : TrendingDown;
  const magnitude = toPtBr(raw.replace('-', ''));
  return {
    text: `${up ? '+' : '-'}${magnitude}%`,
    tone: good ? 'positive' : 'negative',
    icon,
    description: `${up ? 'aumento' : 'redução'} de ${magnitude}% sobre o mês anterior`,
  };
}

/** Percentual do servidor (`"33.3333"`) com 1 casa e vírgula (`33,3%`). */
export function formatPercent(raw: string): string {
  const value = Number(raw);
  return `${Number.isFinite(value) ? value.toFixed(1).replace('.', ',') : raw}%`;
}
