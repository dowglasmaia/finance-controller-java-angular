import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Currency, CurrencyStore } from '../../core/state/currency.store';

interface CurrencyOption {
  readonly value: Currency;
  readonly label: string;
  readonly name: string;
}

/** Seletor BRL | EUR ligado ao `CurrencyStore`. */
@Component({
  selector: 'app-currency-selector',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div
      role="group"
      aria-label="Moeda"
      class="inline-flex overflow-hidden rounded-control border border-line-input bg-surface-raised"
    >
      @for (option of options; track option.value) {
        <button
          type="button"
          class="inline-flex min-h-11 min-w-11 items-center justify-center px-3 text-sm font-medium text-content hover:bg-surface-sunken aria-pressed:bg-accent aria-pressed:text-on-accent"
          [attr.aria-pressed]="store.selectedCurrency() === option.value"
          [attr.aria-label]="option.name"
          (click)="store.select(option.value)"
        >
          {{ option.label }}
        </button>
      }
    </div>
  `,
})
export class CurrencySelector {
  protected readonly store = inject(CurrencyStore);
  protected readonly options: readonly CurrencyOption[] = [
    { value: 'BRL', label: 'BRL', name: 'BRL, Real brasileiro' },
    { value: 'EUR', label: 'EUR', name: 'EUR, Euro' },
  ];
}
