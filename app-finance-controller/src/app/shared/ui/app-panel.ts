import { ChangeDetectionStrategy, Component, input } from '@angular/core';

let nextPanelId = 0;

/** Cartão com título (h2), ação opcional (slot `[panelAction]`) e conteúdo. */
@Component({
  selector: 'app-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section
      class="rounded-card border border-line bg-surface-raised p-4 shadow-card sm:p-6"
      [attr.aria-labelledby]="title() ? titleId : null"
    >
      @if (title()) {
        <div class="mb-4 flex flex-wrap items-center justify-between gap-2">
          <h2 [id]="titleId" class="text-lg font-semibold text-content">{{ title() }}</h2>
          <div class="flex items-center gap-2">
            <ng-content select="[panelAction]" />
          </div>
        </div>
      }
      <ng-content />
    </section>
  `,
})
export class AppPanel {
  readonly title = input<string>('');
  protected readonly titleId = `app-panel-title-${nextPanelId++}`;
}
