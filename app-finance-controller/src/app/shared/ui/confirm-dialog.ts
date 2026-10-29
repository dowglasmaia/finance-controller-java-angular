import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { ButtonDirective } from './button';
import { Modal } from './modal';

/**
 * Confirmação de ação destrutiva. O foco inicial vai para "Cancelar" (a opção segura).
 *
 * ```html
 * @if (target(); as item) {
 *   <app-confirm-dialog title="Excluir categoria?" [message]="..." confirmLabel="Excluir"
 *     [busy]="deleting()" (confirmed)="delete(item)" (cancelled)="target.set(null)" />
 * }
 * ```
 */
@Component({
  selector: 'app-confirm-dialog',
  imports: [Modal, ButtonDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-modal [title]="title()" (closed)="cancelled.emit()">
      <p class="text-sm text-content-muted">{{ message() }}</p>
      <div class="mt-6 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
        <button type="button" appButton variant="secondary" data-autofocus [disabled]="busy()" (click)="cancelled.emit()">
          Cancelar
        </button>
        <button type="button" appButton class="!bg-expense !text-surface" [disabled]="busy()" (click)="confirmed.emit()">
          {{ busy() ? 'Aguarde…' : confirmLabel() }}
        </button>
      </div>
    </app-modal>
  `,
})
export class ConfirmDialog {
  readonly title = input.required<string>();
  readonly message = input.required<string>();
  readonly confirmLabel = input<string>('Confirmar');
  readonly busy = input<boolean>(false);
  readonly confirmed = output<void>();
  readonly cancelled = output<void>();
}
