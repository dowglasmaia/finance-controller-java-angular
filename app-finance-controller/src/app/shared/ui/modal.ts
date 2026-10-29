import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  afterNextRender,
  inject,
  input,
  output,
  viewChild,
} from '@angular/core';

let nextModalId = 0;

const FOCUSABLE =
  'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

/**
 * Diálogo modal acessível: `role="dialog"` + `aria-modal`, foco inicial no primeiro controle (ou no elemento com
 * `data-autofocus`), foco preso (Tab/Shift+Tab circulam), Esc e clique no fundo emitem `closed`, e o foco volta ao
 * elemento que o abriu. Quem usa decide quando destruí-lo (`@if`).
 *
 * ```html
 * @if (open()) {
 *   <app-modal title="Nova Categoria" (closed)="open.set(false)"> ...conteúdo... </app-modal>
 * }
 * ```
 */
@Component({
  selector: 'app-modal',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div
      class="fixed inset-0 z-50 flex items-end justify-center bg-black/50 sm:items-center sm:p-4"
      (mousedown)="onBackdrop($event)"
    >
      <div
        #panel
        role="dialog"
        aria-modal="true"
        tabindex="-1"
        [attr.aria-labelledby]="titleId"
        class="flex max-h-[92vh] w-full flex-col overflow-y-auto rounded-t-card border border-line bg-surface-raised p-5 shadow-overlay focus:outline-none sm:max-w-lg sm:rounded-card sm:p-6"
        (keydown)="onKeydown($event)"
      >
        <h2 [id]="titleId" class="mb-4 text-lg font-semibold text-content">{{ title() }}</h2>
        <ng-content />
      </div>
    </div>
  `,
})
export class Modal {
  readonly title = input.required<string>();
  readonly closed = output<void>();

  protected readonly titleId = `app-modal-title-${nextModalId++}`;
  private readonly panel = viewChild.required<ElementRef<HTMLElement>>('panel');
  private readonly previouslyFocused = document.activeElement instanceof HTMLElement ? document.activeElement : null;

  constructor() {
    afterNextRender(() => {
      const panel = this.panel().nativeElement;
      const target = panel.querySelector<HTMLElement>('[data-autofocus]') ?? panel.querySelector<HTMLElement>(FOCUSABLE);
      (target ?? panel).focus();
    });
    inject(DestroyRef).onDestroy(() => this.previouslyFocused?.focus());
  }

  protected onBackdrop(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.closed.emit();
    }
  }

  protected onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      event.stopPropagation();
      this.closed.emit();
      return;
    }
    if (event.key !== 'Tab') {
      return;
    }
    const items = Array.from(this.panel().nativeElement.querySelectorAll<HTMLElement>(FOCUSABLE));
    if (items.length === 0) {
      event.preventDefault();
      return;
    }
    const first = items[0];
    const last = items[items.length - 1];
    const active = document.activeElement;
    if (event.shiftKey && (active === first || active === this.panel().nativeElement)) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && active === last) {
      event.preventDefault();
      first.focus();
    }
  }
}
