import { deltaFor, formatPercent, greetingFor } from './dashboard-format';

describe('greetingFor', () => {
  it.each([
    [0, 'Bom dia'],
    [11, 'Bom dia'],
    [12, 'Boa tarde'],
    [17, 'Boa tarde'],
    [18, 'Boa noite'],
    [23, 'Boa noite'],
  ])('%dh → %s', (hour, expected) => {
    expect(greetingFor(hour)).toBe(expected);
  });
});

describe('deltaFor', () => {
  it('sem base não há variação para mostrar', () => {
    expect(deltaFor(undefined, false)).toBeNull();
    expect(deltaFor(undefined, true)).toBeNull();
  });

  it('"0" (menos de 0,5%) mostra 0% neutro', () => {
    const delta = deltaFor('0', false);

    expect(delta?.text).toBe('0%');
    expect(delta?.tone).toBe('neutral');
  });

  it('entradas: aumentar é bom e diminuir é ruim, com sinal e vírgula', () => {
    expect(deltaFor('25.0', false)).toMatchObject({ text: '+25,0%', tone: 'positive' });
    expect(deltaFor('-3.2', false)).toMatchObject({ text: '-3,2%', tone: 'negative' });
  });

  it('saídas: gastar menos é a boa notícia', () => {
    expect(deltaFor('-10.5', true)).toMatchObject({ text: '-10,5%', tone: 'positive' });
    expect(deltaFor('8.0', true)).toMatchObject({ text: '+8,0%', tone: 'negative' });
  });

  it('descreve a variação por extenso para leitor de tela', () => {
    expect(deltaFor('25.0', false)?.description).toBe('aumento de 25,0% sobre o mês anterior');
    expect(deltaFor('-3.2', false)?.description).toBe('redução de 3,2% sobre o mês anterior');
  });
});

describe('formatPercent', () => {
  it.each([
    ['33.3333', '33,3%'],
    ['66.6667', '66,7%'],
    ['100.0000', '100,0%'],
    ['0.0000', '0,0%'],
  ])('%s → %s', (raw, expected) => {
    expect(formatPercent(raw)).toBe(expected);
  });
});
