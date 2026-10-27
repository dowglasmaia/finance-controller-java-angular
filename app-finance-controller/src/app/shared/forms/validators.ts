import { DestroyRef } from '@angular/core';
import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import type { FieldError } from '../../core/http/api-error';

/** Mensagens exatas da spec 28 (preservadas da origem) e as de autenticação. */
export const VALIDATION_MESSAGES = {
  required: 'Campo obrigatório',
  valueRequired: 'Informe um valor',
  invalidValue: 'Valor inválido',
  positive: 'O valor deve ser maior que zero',
  category: 'Selecione uma categoria',
  email: 'Informe um e-mail válido',
  passwordMin: (min: number): string => `A senha deve ter pelo menos ${min} caracteres`,
  maxLength: (max: number): string => `Use no máximo ${max} caracteres`,
  mismatch: 'As senhas não coincidem',
} as const;

/** Cada erro produzido aqui carrega a mensagem pronta para exibição. */
export interface ValidationMessage {
  readonly message: string;
}

function fail(key: string, message: string): ValidationErrors {
  const detail: ValidationMessage = { message };
  return { [key]: detail };
}

function isBlank(value: unknown): boolean {
  return value === null || value === undefined || (typeof value === 'string' && value.trim() === '');
}

function asText(value: unknown): string {
  return typeof value === 'string' ? value : value === null || value === undefined ? '' : String(value);
}

/** Texto obrigatório: `Campo obrigatório`. */
export const requiredText: ValidatorFn = (control) =>
  isBlank(control.value) ? fail('required', VALIDATION_MESSAGES.required) : null;

/** Valor obrigatório: `Informe um valor`. */
export const requiredValue: ValidatorFn = (control) =>
  isBlank(control.value) ? fail('valueRequired', VALIDATION_MESSAGES.valueRequired) : null;

/** Categoria obrigatória: `Selecione uma categoria`. */
export const requiredCategory: ValidatorFn = (control) =>
  isBlank(control.value) ? fail('category', VALIDATION_MESSAGES.category) : null;

const DECIMAL_PATTERN = /^\d+(\.\d+)?$/;

/** Decimal em string (`"1234.56"`) — vazio passa (combine com `requiredValue`); senão `Valor inválido`. */
export const decimalValue: ValidatorFn = (control) => {
  if (isBlank(control.value)) {
    return null;
  }
  return DECIMAL_PATTERN.test(asText(control.value).trim()) ? null : fail('invalidValue', VALIDATION_MESSAGES.invalidValue);
};

/** Decimal em string > 0, sem converter para `number`. Vazio ou inválido passam (use os outros). */
export const positiveAmount: ValidatorFn = (control) => {
  const text = asText(control.value).trim();
  if (text === '' || !DECIMAL_PATTERN.test(text)) {
    return null;
  }
  return /[1-9]/.test(text) ? null : fail('positive', VALIDATION_MESSAGES.positive);
};

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/** Formato de e-mail; vazio passa. */
export const emailFormat: ValidatorFn = (control) => {
  if (isBlank(control.value)) {
    return null;
  }
  return EMAIL_PATTERN.test(asText(control.value).trim()) ? null : fail('email', VALIDATION_MESSAGES.email);
};

/** Tamanho mínimo de senha (padrão 8); vazio passa. */
export function minLengthPassword(min = 8): ValidatorFn {
  return (control) => {
    const text = asText(control.value);
    if (text === '') {
      return null;
    }
    return text.length >= min ? null : fail('passwordMin', VALIDATION_MESSAGES.passwordMin(min));
  };
}

export function maxLength(max: number): ValidatorFn {
  return (control) =>
    asText(control.value).length <= max ? null : fail('maxLength', VALIDATION_MESSAGES.maxLength(max));
}

/**
 * Validador do campo de confirmação: o valor deve ser igual ao do campo `otherField` do mesmo grupo.
 * Quando o outro campo muda, use `revalidateOnChange` para reavaliar a confirmação.
 */
export function matchesField(otherField: string): ValidatorFn {
  return (control) => {
    const other = control.parent?.get(otherField);
    if (!other || isBlank(control.value)) {
      return null;
    }
    return control.value === other.value ? null : fail('mismatch', VALIDATION_MESSAGES.mismatch);
  };
}

/** Reavalia `target` sempre que `source` mudar (ex.: senha → confirmação). Cancela ao destruir. */
export function revalidateOnChange(source: AbstractControl, target: AbstractControl, destroyRef: DestroyRef): void {
  const subscription = source.valueChanges.subscribe(() => target.updateValueAndValidity());
  destroyRef.onDestroy(() => subscription.unsubscribe());
}

function isValidationMessage(value: unknown): value is ValidationMessage {
  return typeof value === 'object' && value !== null && typeof (value as { message?: unknown }).message === 'string';
}

/**
 * Primeira mensagem de erro do controle (validadores desta lib, `server` ou, como reserva,
 * `Validators.required` → `Campo obrigatório`; qualquer outro → `Valor inválido`). `null` se válido.
 */
export function firstErrorMessage(control: AbstractControl | null): string | null {
  const errors = control?.errors;
  if (!errors) {
    return null;
  }
  for (const value of Object.values(errors) as unknown[]) {
    if (isValidationMessage(value)) {
      return value.message;
    }
  }
  return 'required' in errors ? VALIDATION_MESSAGES.required : VALIDATION_MESSAGES.invalidValue;
}

/** Mesma mensagem, mas só depois de o utilizador interagir (tocado ou alterado). */
export function visibleErrorMessage(control: AbstractControl | null): string | null {
  if (!control || !(control.touched || control.dirty)) {
    return null;
  }
  return firstErrorMessage(control);
}

const INVALID_CONTROL_SELECTOR = [
  'input[aria-invalid="true"]',
  'select[aria-invalid="true"]',
  'textarea[aria-invalid="true"]',
  'input.ng-invalid',
  'select.ng-invalid',
  'textarea.ng-invalid',
].join(',');

/** Foca o primeiro controle inválido (ordem do documento) dentro do formulário. Devolve se focou algum. */
export function focusFirstInvalid(formEl: HTMLElement): boolean {
  const candidates = formEl.querySelectorAll<HTMLElement>(INVALID_CONTROL_SELECTOR);
  for (const element of Array.from(candidates)) {
    if (!(element as HTMLInputElement).disabled) {
      element.focus();
      return true;
    }
  }
  return false;
}

/**
 * Marca o erro `server` nos controles homônimos de `ApiError.fieldErrors`.
 * Devolve os erros sem controle correspondente (para exibir em toast). O erro some quando o valor muda.
 */
export function applyServerErrors(form: AbstractControl, fieldErrors: readonly FieldError[] | undefined): FieldError[] {
  const unmatched: FieldError[] = [];
  for (const fieldError of fieldErrors ?? []) {
    const control = form.get(fieldError.field);
    if (!control) {
      unmatched.push(fieldError);
      continue;
    }
    const detail: ValidationMessage = { message: fieldError.message };
    control.setErrors({ ...control.errors, server: detail });
    control.markAsTouched();
  }
  return unmatched;
}
