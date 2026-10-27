import {
  ChangeDetectionStrategy,
  Component,
  Directive,
  computed,
  effect,
  inject,
  input,
  signal,
} from '@angular/core';
import { AbstractControl } from '@angular/forms';
import { visibleErrorMessage } from '../forms/validators';

let nextFieldId = 0;

/**
 * Campo de formulário: rótulo visível, projeção do controle, ajuda e erro.
 *
 * ```html
 * <app-form-field label="E-mail" [control]="form.controls.email" help="Usado para entrar.">
 *   <input appFieldControl type="email" formControlName="email" autocomplete="email" />
 * </app-form-field>
 * ```
 *
 * O erro vem de `control` (mensagens dos validadores, só após tocar/alterar) ou do input `error`.
 */
@Component({
  selector: 'app-form-field',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex flex-col gap-1.5">
      <label [attr.for]="controlId" class="text-sm font-medium text-content">
        {{ label() }}
        @if (required()) {
          <span aria-hidden="true" class="text-expense">*</span>
        }
      </label>
      <ng-content />
      @if (help()) {
        <p [id]="helpId" class="text-xs text-content-muted">{{ help() }}</p>
      }
      @if (errorMessage(); as message) {
        <p [id]="errorId" role="alert" class="text-xs font-medium text-expense">{{ message }}</p>
      }
    </div>
  `,
})
export class FormFieldComponent {
  readonly label = input.required<string>();
  readonly help = input<string>('');
  /** Mensagem de erro explícita (tem prioridade sobre a do `control`). */
  readonly error = input<string | null>(null);
  /** Controle observado para derivar a mensagem de erro. */
  readonly control = input<AbstractControl | null>(null);
  /** Apenas marca o rótulo com `*` (o atributo `required` do controle é do consumidor). */
  readonly required = input<boolean>(false);

  readonly controlId = `app-field-${nextFieldId}`;
  readonly helpId = `${this.controlId}-help`;
  readonly errorId = `${this.controlId}-error`;

  private readonly controlEvents = signal(0);

  readonly errorMessage = computed(() => {
    this.controlEvents();
    return this.error() ?? visibleErrorMessage(this.control());
  });
  readonly hasError = computed(() => this.errorMessage() !== null);
  readonly hasHelp = computed(() => this.help() !== '');
  /** Valor de `aria-describedby` (ajuda e/ou erro presentes) ou `null`. */
  readonly describedBy = computed(() => {
    const ids = [this.hasHelp() ? this.helpId : '', this.hasError() ? this.errorId : ''].filter((id) => id !== '');
    return ids.length > 0 ? ids.join(' ') : null;
  });

  constructor() {
    nextFieldId += 1;
    effect((onCleanup) => {
      const control = this.control();
      if (!control) {
        return;
      }
      const subscription = control.events.subscribe(() => this.controlEvents.update((n) => n + 1));
      onCleanup(() => subscription.unsubscribe());
    });
  }
}

/**
 * Liga o `input`/`select`/`textarea` projetado ao `app-form-field` pai: define `id` (casa com o `for`
 * do rótulo), `aria-invalid` e `aria-describedby`. Não defina `id` no controle.
 */
@Directive({
  selector: '[appFieldControl]',
  host: {
    '[id]': 'field.controlId',
    '[attr.aria-invalid]': 'field.hasError() ? "true" : null',
    '[attr.aria-describedby]': 'field.describedBy()',
  },
})
export class FieldControlDirective {
  protected readonly field = inject(FormFieldComponent);
}

/** Atalho para o array `imports` dos consumidores. */
export const FORM_FIELD_IMPORTS = [FormFieldComponent, FieldControlDirective] as const;
