import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { Inbox, LucideAngularModule, LucideIconData } from 'lucide-angular';

/** Estado vazio: ícone + mensagem (e título opcional). */
@Component({
  selector: 'app-empty-state',
  imports: [LucideAngularModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex flex-col items-center gap-3 px-4 py-10 text-center text-content-muted">
      <lucide-icon [img]="icon()" [size]="40" aria-hidden="true" />
      @if (title()) {
        <p class="text-base font-medium text-content">{{ title() }}</p>
      }
      <p class="max-w-prose text-sm">{{ message() }}</p>
      <ng-content />
    </div>
  `,
})
export class EmptyState {
  readonly message = input.required<string>();
  readonly title = input<string>('');
  readonly icon = input<LucideIconData>(Inbox);
}
