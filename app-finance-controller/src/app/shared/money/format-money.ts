export type CurrencyCode = 'BRL' | 'EUR';

/** Decimal em string com sinal opcional: `"1234.56"`, `"+100"`, `"-100.5"`. */
const DECIMAL_PATTERN = /^([+-]?)(\d+)(?:\.(\d+))?$/;

/** `Intl.NumberFormat#format` aceita string decimal (NumberFormat v3); os tipos da lib ainda não. */
interface StringFormatter {
  format(value: string): string;
}

const formatters = new Map<CurrencyCode, Intl.NumberFormat>();

function formatterFor(currency: CurrencyCode): Intl.NumberFormat {
  let formatter = formatters.get(currency);
  if (!formatter) {
    formatter = new Intl.NumberFormat('pt-BR', { style: 'currency', currency });
    formatters.set(currency, formatter);
  }
  return formatter;
}

let intlAcceptsDecimalStrings: boolean | undefined;

/** O runtime precisa preservar precisão além de `number` (2^53); senão usa o texto manual. */
function supportsDecimalStrings(): boolean {
  if (intlAcceptsDecimalStrings === undefined) {
    const probe = (formatterFor('BRL') as unknown as StringFormatter).format('9007199254740993.00');
    intlAcceptsDecimalStrings = probe.includes('9.007.199.254.740.993');
  }
  return intlAcceptsDecimalStrings;
}

const SYMBOLS: Record<CurrencyCode, string> = { BRL: 'R$', EUR: '€' };

function groupThousands(digits: string): string {
  return digits.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
}

/** Reserva sem `number`: arredonda a 2 casas (meio para cima) por aritmética de inteiros. */
export function formatCurrencyManual(unsigned: string, currency: CurrencyCode): string {
  const match = DECIMAL_PATTERN.exec(unsigned);
  if (!match) {
    return unsigned;
  }
  const integerPart = match[2] ?? '0';
  const fraction = (match[3] ?? '').padEnd(3, '0');
  let cents = BigInt(integerPart) * 100n + BigInt(fraction.slice(0, 2));
  if (fraction.charAt(2) >= '5') {
    cents += 1n;
  }
  const text = cents.toString().padStart(3, '0');
  const whole = text.slice(0, -2);
  const decimals = text.slice(-2);
  return `${SYMBOLS[currency]} ${groupThousands(whole)},${decimals}`;
}

/**
 * Formata uma string decimal da API em moeda pt-BR (`R$ 1.234,56`, `€ 1.234,56`) sem passar por `number`.
 * Sinal explícito (`+`/`-`) do valor é preservado antes do símbolo: `+R$ 100,00`, `-R$ 100,00`.
 * O `Intl` separa símbolo e valor com NBSP (U+00A0). Entrada inválida é devolvida como veio.
 */
export function formatMoney(value: string, currency: CurrencyCode): string {
  const trimmed = value.trim();
  const match = DECIMAL_PATTERN.exec(trimmed);
  if (!match) {
    return value;
  }
  const sign = match[1] ?? '';
  const unsigned = trimmed.slice(sign.length);
  const body = supportsDecimalStrings()
    ? (formatterFor(currency) as unknown as StringFormatter).format(unsigned)
    : formatCurrencyManual(unsigned, currency);
  return `${sign}${body}`;
}

const DATE_PATTERN = /^(\d{4})-(\d{2})-(\d{2})/;

/** `YYYY-MM-DD` (ou ISO com hora) → `dd/MM/yyyy`, sem objeto `Date` e sem fuso. Inválida: devolve a entrada. */
export function formatDate(value: string): string {
  const match = DATE_PATTERN.exec(value);
  if (!match) {
    return value;
  }
  return `${match[3]}/${match[2]}/${match[1]}`;
}
