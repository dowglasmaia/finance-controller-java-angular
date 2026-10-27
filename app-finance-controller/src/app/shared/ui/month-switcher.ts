import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { ChevronLeft, ChevronRight, LucideAngularModule } from 'lucide-angular';

/** ‹ Junho 2026 › — apresentação pura; o estado fica no `ReferenceMonthStore` (ligado pelo shell). */
@Component({
  selector: 'app-month-switcher',
  imports: [LucideAngularModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div
      role="group"
      aria-label="Mês de referência"
      class="inline-flex items-center rounded-lg border border-border-subtle bg-surface-raised"
    >
      <button
        type="button"
        class="inline-flex h-11 w-11 items-center justify-center rounded-l-lg text-content hover:bg-surface"
        aria-label="Mês anterior"
        (click)="previous.emit()"
      >
        <lucide-icon [img]="previousIcon" [size]="20" aria-hidden="true" />
      </button>
      <button
        type="button"
        class="inline-flex min-h-11 min-w-32 items-center justify-center px-2 text-sm font-medium text-content hover:bg-surface"
        [attr.aria-label]="label() + ' — voltar ao mês atual'"
        title="Voltar ao mês atual"
        (click)="reset.emit()"
      >
        {{ label() }}
      </button>
      <button
        type="button"
        class="inline-flex h-11 w-11 items-center justify-center rounded-r-lg text-content hover:bg-surface"
        aria-label="Próximo mês"
        (click)="next.emit()"
      >
        <lucide-icon [img]="nextIcon" [size]="20" aria-hidden="true" />
      </button>
    </div>
  `,
})
export class MonthSwitcher {
  readonly label = input.required<string>();
  readonly previous = output<void>();
  readonly next = output<void>();
  readonly reset = output<void>();

  protected readonly previousIcon = ChevronLeft;
  protected readonly nextIcon = ChevronRight;
}
