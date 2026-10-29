import { FormControl, FormGroup, Validators } from '@angular/forms';
import { formatCurrencyManual, formatDate, formatMoney } from '../money/format-money';
import {
  applyServerErrors,
  decimalValue,
  emailFormat,
  firstErrorMessage,
  focusFirstInvalid,
  matchesField,
  maxLength,
  minLengthPassword,
  positiveAmount,
  requiredCategory,
  requiredText,
  requiredValue,
  visibleErrorMessage,
} from './validators';

/** Os ramos de borda dos validadores e da formatação (valores nulos, numéricos, só espaços), um a um. */
describe('validadores: ramos de borda', () => {
  const valid = (validator: (c: FormControl) => unknown, value: unknown) => validator(new FormControl(value as never));

  it('nulo, indefinido e só espaços contam como vazio nos obrigatórios', () => {
    for (const value of [null, undefined, '', '   ']) {
      expect(valid(requiredText, value)).not.toBeNull();
      expect(valid(requiredValue, value)).not.toBeNull();
      expect(valid(requiredCategory, value)).not.toBeNull();
    }
    expect(valid(requiredText, 'a')).toBeNull();
  });

  it('valores numéricos são tratados como texto', () => {
    expect(valid(decimalValue, 12)).toBeNull();
    expect(valid(decimalValue, 12.5)).toBeNull();
    expect(valid(positiveAmount, 0)).not.toBeNull();
    expect(valid(positiveAmount, 3)).toBeNull();
    expect(valid(emailFormat, 12345)).not.toBeNull();
    expect(valid(maxLength(3), 12345)).not.toBeNull();
    expect(valid(maxLength(3), null)).toBeNull();
    expect(valid(minLengthPassword(), null)).toBeNull();
    expect(valid(minLengthPassword(), 1234567)).not.toBeNull();
  });

  it('decimal e positivo: zeros, ponto isolado e símbolos', () => {
    for (const bad of ['.', '1.', '.5', '-1', '1,5', 'abc', '1e3', '--1']) {
      expect(valid(decimalValue, bad), bad).not.toBeNull();
    }
    for (const zero of ['0', '0.0', '000', '0.00']) {
      expect(valid(positiveAmount, zero), zero).not.toBeNull();
    }
    for (const ok of ['0.01', '10', '1.50', ' 7 ']) {
      expect(valid(positiveAmount, ok), ok).toBeNull();
    }
    expect(valid(positiveAmount, 'abc')).toBeNull(); // formato inválido é do decimalValue
  });

  it('e-mail: formatos aceitos e recusados', () => {
    for (const bad of ['a', 'a@b', 'a b@c.com', '@c.com', 'a@.com']) {
      expect(valid(emailFormat, bad), bad).not.toBeNull();
    }
    expect(valid(emailFormat, ' a@b.co ')).toBeNull();
    expect(valid(emailFormat, '')).toBeNull();
  });

  it('confirmação sem o outro campo ou vazia não reclama', () => {
    expect(new FormControl('x', matchesField('outro')).errors).toBeNull();
    const group = new FormGroup({ a: new FormControl('x'), b: new FormControl('', matchesField('a')) });
    expect(group.controls.b.errors).toBeNull();
    group.controls.b.setValue('y');
    expect(group.controls.b.errors).not.toBeNull();
  });

  it('firstErrorMessage: nulo, Validators.required, desconhecido e mensagem própria', () => {
    expect(firstErrorMessage(null)).toBeNull();
    expect(firstErrorMessage(new FormControl('ok'))).toBeNull();
    expect(firstErrorMessage(new FormControl('', Validators.required))).toBe('Campo obrigatório');
    expect(firstErrorMessage(new FormControl('', Validators.email))).toBeNull();
    const weird = new FormControl('x');
    weird.setErrors({ qualquer: true });
    expect(firstErrorMessage(weird)).toBe('Valor inválido');
    expect(firstErrorMessage(new FormControl('', requiredText))).toBe('Campo obrigatório');
  });

  it('visibleErrorMessage: nulo, só tocado e só alterado', () => {
    expect(visibleErrorMessage(null)).toBeNull();
    const touched = new FormControl('', requiredText);
    touched.markAsTouched();
    expect(visibleErrorMessage(touched)).toBe('Campo obrigatório');
    const dirty = new FormControl('', requiredText);
    dirty.markAsDirty();
    expect(visibleErrorMessage(dirty)).toBe('Campo obrigatório');
  });

  it('applyServerErrors sem lista devolve vazio e foco ignora controles desabilitados', () => {
    const form = new FormGroup({ a: new FormControl('') });
    expect(applyServerErrors(form, undefined)).toEqual([]);
    const host = document.createElement('div');
    host.innerHTML = '<input class="ng-invalid" disabled><input aria-invalid="true" id="alvo">';
    document.body.appendChild(host);
    expect(focusFirstInvalid(host)).toBe(true);
    expect(document.activeElement?.id).toBe('alvo');
    host.remove();
  });
});

describe('formatação monetária: ramos de borda', () => {
  it('o texto manual recusa o inválido, arredonda para cima a partir de 5 e agrupa milhares', () => {
    expect(formatCurrencyManual('abc', 'BRL').replace(/\s/g, ' ')).toBe('abc');
    expect(formatCurrencyManual('1.005', 'BRL').replace(/\s/g, ' ')).toBe('R$ 1,01');
    expect(formatCurrencyManual('1.004', 'BRL').replace(/\s/g, ' ')).toBe('R$ 1,00');
    expect(formatCurrencyManual('0.004', 'EUR').replace(/\s/g, ' ')).toBe('€ 0,00');
    expect(formatCurrencyManual('1234567.8', 'EUR').replace(/\s/g, ' ')).toBe('€ 1.234.567,80');
    expect(formatCurrencyManual('7', 'BRL').replace(/\s/g, ' ')).toBe('R$ 7,00');
  });

  it('formatMoney aceita sinais e espaços e devolve o inválido como veio', () => {
    expect(formatMoney(' 10.5 ', 'BRL')).toMatch(/R\$\s10,50/);
    expect(formatMoney('+3', 'EUR')).toMatch(/^\+€\s3,00/);
    expect(formatMoney('-0.01', 'BRL')).toMatch(/^-R\$\s0,01/);
    expect(formatMoney('', 'BRL')).toBe('');
    expect(formatMoney('1,5', 'BRL')).toBe('1,5');
  });

  it('formatDate aceita data com hora e recusa o resto', () => {
    expect(formatDate('2026-06-15T10:20:30Z')).toBe('15/06/2026');
    expect(formatDate('15/06/2026')).toBe('15/06/2026');
    expect(formatDate('')).toBe('');
  });
});
