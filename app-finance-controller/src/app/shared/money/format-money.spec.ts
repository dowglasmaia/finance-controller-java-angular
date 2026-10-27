import { formatCurrencyManual, formatDate, formatMoney } from './format-money';

/** O Intl separa símbolo e valor com NBSP (U+00A0); normalizamos para comparar com a spec. */
const plain = (text: string): string => text.replace(/ /g, ' ');

describe('formatMoney', () => {
  it('formata BRL e EUR em pt-BR', () => {
    expect(plain(formatMoney('1234.56', 'BRL'))).toBe('R$ 1.234,56');
    expect(plain(formatMoney('1234.56', 'EUR'))).toBe('€ 1.234,56');
  });

  it('preserva o sinal explícito antes do símbolo', () => {
    expect(plain(formatMoney('+100.00', 'BRL'))).toBe('+R$ 100,00');
    expect(plain(formatMoney('-100.00', 'BRL'))).toBe('-R$ 100,00');
    expect(plain(formatMoney('-100', 'EUR'))).toBe('-€ 100,00');
  });

  it('não perde precisão em valores grandes (sem number)', () => {
    expect(plain(formatMoney('9007199254740993.00', 'BRL'))).toBe('R$ 9.007.199.254.740.993,00');
  });

  it('devolve a entrada inválida como veio', () => {
    expect(formatMoney('abc', 'BRL')).toBe('abc');
  });
});

describe('formatCurrencyManual (reserva)', () => {
  it('agrupa milhares e arredonda a 2 casas', () => {
    expect(plain(formatCurrencyManual('1234.56', 'BRL'))).toBe('R$ 1.234,56');
    expect(plain(formatCurrencyManual('0.5', 'EUR'))).toBe('€ 0,50');
    expect(plain(formatCurrencyManual('10.005', 'BRL'))).toBe('R$ 10,01');
  });
});

describe('formatDate', () => {
  it('converte YYYY-MM-DD em dd/MM/yyyy sem fuso', () => {
    expect(formatDate('2026-06-15')).toBe('15/06/2026');
    expect(formatDate('2026-01-01')).toBe('01/01/2026');
  });

  it('devolve a entrada inválida como veio', () => {
    expect(formatDate('15/06/2026')).toBe('15/06/2026');
  });
});
