import { Directive, ElementRef, forwardRef, inject } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

const MAX_DECIMALS = 2;

export interface SanitizedMoney {
  /** Texto mostrado ao utilizador (vírgula decimal, só dígitos). */
  readonly display: string;
  /** Decimal para a API (`"1234.56"`), sem passar por `number`; vazio se ainda não há dígitos. */
  readonly value: string;
}

/**
 * Limpa o que foi digitado: só dígitos e um separador decimal (`,` ou `.`), no máximo 2 casas. O valor da API nunca
 * termina em separador (`"12,"` → `"12"`), e nada é convertido para `number`.
 */
export function sanitizeMoney(raw: string): SanitizedMoney {
  let integer = '';
  let fraction = '';
  let seenSeparator = false;
  for (const char of raw) {
    if (char >= '0' && char <= '9') {
      if (seenSeparator) {
        if (fraction.length < MAX_DECIMALS) {
          fraction += char;
        }
      } else {
        integer += char;
      }
    } else if ((char === ',' || char === '.') && !seenSeparator) {
      seenSeparator = true;
    }
  }
  const display = seenSeparator ? `${integer},${fraction}` : integer;
  const value = fraction === '' ? integer : `${integer === '' ? '0' : integer}.${fraction}`;
  return { display, value: integer === '' && fraction === '' ? '' : value };
}

/**
 * Campo de valor monetário: `<input appMoneyInput formControlName="amount">`. O controle guarda o decimal da API
 * como texto (`"1234.56"`); o campo mostra com vírgula. Combine com `requiredValue`, `decimalValue` e `positiveAmount`.
 */
@Directive({
  selector: 'input[appMoneyInput]',
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => MoneyInputDirective), multi: true }],
  host: {
    inputmode: 'decimal',
    autocomplete: 'off',
    '(input)': 'handleInput()',
    '(blur)': 'onTouched()',
  },
})
export class MoneyInputDirective implements ControlValueAccessor {
  private readonly element = inject<ElementRef<HTMLInputElement>>(ElementRef).nativeElement;
  private onChange: (value: string) => void = () => undefined;
  protected onTouched: () => void = () => undefined;

  writeValue(value: string | null): void {
    this.element.value = value === null || value === undefined ? '' : sanitizeMoney(value).display;
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(disabled: boolean): void {
    this.element.disabled = disabled;
  }

  protected handleInput(): void {
    const { display, value } = sanitizeMoney(this.element.value);
    if (display !== this.element.value) {
      this.element.value = display;
    }
    this.onChange(value);
  }
}
