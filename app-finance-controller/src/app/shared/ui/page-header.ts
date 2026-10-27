import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Cabeçalho de página: `h1` (alvo do foco pós-navegação), subtítulo e slot `[actions]`. */
@Component({
  selector: 'app-page-header',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="mb-6 flex flex-wrap items-start justify-between gap-3">
      <div class="min-w-0">
        <h1 tabindex="-1" class="break-words text-2xl font-semibold text-content focus:outline-none">
          {{ title() }}
        </h1>
        @if (subtitle()) {
          <p class="mt-1 text-sm text-content-muted">{{ subtitle() }}</p>
        }
      </div>
      <div class="flex flex-wrap items-center gap-2">
        <ng-content select="[actions]" />
      </div>
    </header>
  `,
})
export class PageHeader {
  readonly title = input.required<string>();
  readonly subtitle = input<string>('');
}
