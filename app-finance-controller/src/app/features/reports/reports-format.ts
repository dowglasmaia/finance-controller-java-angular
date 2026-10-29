import { formatDate } from '../../shared/money/format-money';

const MONTH_NAMES = ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'];

/** `2026-06` → `Jun/2026`. */
export function monthName(key: string): string {
  const [year, month] = key.split('-');
  return `${MONTH_NAMES[Number(month) - 1] ?? month}/${year}`;
}

/** `21/06/2026 a 27/06/2026`. */
export function rangeText(range: { start: string; end: string }): string {
  return `${formatDate(range.start)} a ${formatDate(range.end)}`;
}

/** Variação do saldo como o servidor a entrega (`"50.0"`, `"-100.0"`) → `+50,0%`, `-100,0%`; ausente → `—`. */
export function changeText(raw: string | undefined): string {
  if (raw === undefined) {
    return '—';
  }
  const value = Number(raw);
  if (!Number.isFinite(value)) {
    return raw;
  }
  const magnitude = raw.replace('-', '').replace('.', ',');
  return value > 0 ? `+${magnitude}%` : value < 0 ? `-${magnitude}%` : `${magnitude}%`;
}

/** Cor pelo sinal de um decimal em string. */
export function signClass(raw: string | undefined): string {
  if (raw === undefined || Number(raw) === 0) {
    return 'text-content';
  }
  return Number(raw) > 0 ? 'text-success' : 'text-expense';
}
