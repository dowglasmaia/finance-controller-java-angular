import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { Construction } from 'lucide-angular';
import { map } from 'rxjs';
import { AppPanel } from '../../shared/ui/app-panel';
import { EmptyState } from '../../shared/ui/empty-state';
import { PageHeader } from '../../shared/ui/page-header';

const DEFAULT_TITLE = 'Em breve';

/** Página provisória das fatias futuras. O título vem de `route.data['title']`. */
@Component({
  selector: 'app-coming-soon-page',
  imports: [PageHeader, AppPanel, EmptyState],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-page-header [title]="title()" />
    <app-panel>
      <app-empty-state
        [icon]="icon"
        title="Em breve"
        message="Esta área ainda está em construção e será entregue em uma próxima etapa."
      />
    </app-panel>
  `,
})
export class ComingSoonPage {
  protected readonly icon = Construction;
  private readonly route = inject(ActivatedRoute);

  protected readonly title = toSignal(
    this.route.data.pipe(map((data) => (typeof data['title'] === 'string' ? data['title'] : DEFAULT_TITLE))),
    { initialValue: DEFAULT_TITLE },
  );
}
