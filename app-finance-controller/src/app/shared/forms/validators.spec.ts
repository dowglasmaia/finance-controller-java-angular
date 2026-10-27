import { DestroyRef } from '@angular/core';
import { FormControl, FormGroup, ValidatorFn } from '@angular/forms';
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
  revalidateOnChange,
  visibleErrorMessage,
} from './validators';

function messageOf(validator: ValidatorFn, value: string): string | null {
  const control = new FormControl(value, { nonNullable: true, validators: [validator] });
  return firstErrorMessage(control);
}

describe('validators', () => {
  it('requiredText: Campo obrigatório', () => {
    expect(messageOf(requiredText, '  ')).toBe('Campo obrigatório');
    expect(messageOf(requiredText, 'ok')).toBeNull();
  });

  it('requiredValue: Informe um valor', () => {
    expect(messageOf(requiredValue, '')).toBe('Informe um valor');
    expect(messageOf(requiredValue, '10')).toBeNull();
  });

  it('decimalValue: Valor inválido para texto não numérico; vazio passa', () => {
    expect(messageOf(decimalValue, 'abc')).toBe('Valor inválido');
    expect(messageOf(decimalValue, '12,5')).toBe('Valor inválido');
    expect(messageOf(decimalValue, '1234.56')).toBeNull();
    expect(messageOf(decimalValue, '')).toBeNull();
  });

  it('positiveAmount: O valor deve ser maior que zero', () => {
    expect(messageOf(positiveAmount, '0.00')).toBe('O valor deve ser maior que zero');
    expect(messageOf(positiveAmount, '0')).toBe('O valor deve ser maior que zero');
    expect(messageOf(positiveAmount, '0.01')).toBeNull();
    expect(messageOf(positiveAmount, '')).toBeNull();
  });

  it('requiredCategory: Selecione uma categoria', () => {
    expect(messageOf(requiredCategory, '')).toBe('Selecione uma categoria');
    expect(messageOf(requiredCategory, 'abc')).toBeNull();
  });

  it('emailFormat: Informe um e-mail válido', () => {
    expect(messageOf(emailFormat, 'sem-arroba')).toBe('Informe um e-mail válido');
    expect(messageOf(emailFormat, 'a@b')).toBe('Informe um e-mail válido');
    expect(messageOf(emailFormat, 'a@b.com')).toBeNull();
    expect(messageOf(emailFormat, '')).toBeNull();
  });

  it('minLengthPassword: A senha deve ter pelo menos 8 caracteres', () => {
    expect(messageOf(minLengthPassword(8), '1234567')).toBe('A senha deve ter pelo menos 8 caracteres');
    expect(messageOf(minLengthPassword(8), '12345678')).toBeNull();
    expect(messageOf(minLengthPassword(), '123')).toBe('A senha deve ter pelo menos 8 caracteres');
  });

  it('maxLength', () => {
    expect(messageOf(maxLength(3), 'abcd')).toBe('Use no máximo 3 caracteres');
    expect(messageOf(maxLength(3), 'abc')).toBeNull();
  });

  describe('matchesField', () => {
    it('As senhas não coincidem; reavalia quando a senha muda', () => {
      const form = new FormGroup({
        password: new FormControl('', { nonNullable: true }),
        confirm: new FormControl('', { nonNullable: true, validators: [matchesField('password')] }),
      });
      const destroyRef = { onDestroy: vi.fn() } as unknown as DestroyRef;
      revalidateOnChange(form.controls.password, form.controls.confirm, destroyRef);

      form.controls.password.setValue('senha-segura');
      form.controls.confirm.setValue('senha-diferente');
      expect(firstErrorMessage(form.controls.confirm)).toBe('As senhas não coincidem');

      form.controls.password.setValue('senha-diferente');
      expect(form.controls.confirm.valid).toBe(true);
    });
  });
});

describe('mensagens de erro', () => {
  it('visibleErrorMessage só aparece após interação', () => {
    const control = new FormControl('', { nonNullable: true, validators: [requiredText] });
    expect(visibleErrorMessage(control)).toBeNull();
    control.markAsTouched();
    expect(visibleErrorMessage(control)).toBe('Campo obrigatório');
  });

  it('firstErrorMessage cobre erros desconhecidos', () => {
    const control = new FormControl('x');
    control.setErrors({ qualquer: true });
    expect(firstErrorMessage(control)).toBe('Valor inválido');
    expect(firstErrorMessage(null)).toBeNull();
  });
});

describe('applyServerErrors', () => {
  it('marca erro server nos controles homônimos e devolve os sem campo', () => {
    const form = new FormGroup({ email: new FormControl('a@b.com', { nonNullable: true }) });
    const unmatched = applyServerErrors(form, [
      { field: 'email', message: 'E-mail já cadastrado' },
      { field: 'inexistente', message: 'Outro erro' },
    ]);

    expect(firstErrorMessage(form.controls.email)).toBe('E-mail já cadastrado');
    expect(form.controls.email.touched).toBe(true);
    expect(unmatched).toEqual([{ field: 'inexistente', message: 'Outro erro' }]);

    form.controls.email.setValue('novo@b.com');
    expect(form.controls.email.valid).toBe(true);
  });
});

describe('focusFirstInvalid', () => {
  it('foca o primeiro controle inválido na ordem do documento', () => {
    const form = document.createElement('form');
    form.innerHTML = `
      <input id="ok" />
      <input id="a" class="ng-invalid" />
      <input id="b" aria-invalid="true" />`;
    document.body.appendChild(form);

    expect(focusFirstInvalid(form)).toBe(true);
    expect(document.activeElement?.id).toBe('a');
    form.remove();
  });

  it('devolve false quando nada é inválido', () => {
    const form = document.createElement('form');
    form.innerHTML = '<input id="ok" />';
    expect(focusFirstInvalid(form)).toBe(false);
  });
});
