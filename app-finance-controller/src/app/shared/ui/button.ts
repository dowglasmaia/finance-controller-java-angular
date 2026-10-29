import { Directive, computed, input } from '@angular/core';

export type ButtonVariant = 'primary' | 'secondary' | 'ghost';

const BASE =
  'inline-flex min-h-11 items-center justify-center gap-2 rounded-control px-4 text-sm font-semibold transition-colors duration-fast ease-standard disabled:cursor-not-allowed disabled:opacity-60';

const VARIANTS: Record<ButtonVariant, string> = {
  primary: 'bg-accent text-on-accent hover:bg-accent/90',
  secondary: 'border border-line-input bg-surface-raised text-content hover:bg-surface-sunken',
  ghost: 'text-content hover:bg-surface-sunken',
};

/**
 * Estilo de botão do sistema, para `<button>` e `<a>`:
 * `primary` (ação principal, uma por contexto), `secondary` e `ghost`.
 *
 * ```html
 * <button appButton type="submit">Salvar</button>
 * <a appButton variant="secondary" routerLink="/">Voltar</a>
 * ```
 */
@Directive({
  selector: '[appButton]',
  host: { '[class]': 'classes()' },
})
export class ButtonDirective {
  readonly variant = input<ButtonVariant>('primary');
  protected readonly classes = computed(() => `${BASE} ${VARIANTS[this.variant()]}`);
}
