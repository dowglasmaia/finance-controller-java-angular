import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideAngularModule, SearchX } from 'lucide-angular';
import { PageHeader } from '../../shared/ui/page-header';

/** Rota `**`: página não encontrada, com caminho de volta ao Dashboard. */
@Component({
  selector: 'app-not-found-page',
  imports: [RouterLink, LucideAngularModule, PageHeader],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-page-header title="Página não encontrada" subtitle="O endereço que você acessou não existe." />
    <div class="flex flex-col items-start gap-4">
      <lucide-icon [img]="icon" [size]="40" class="text-content-muted" aria-hidden="true" />
      <a
        routerLink="/"
        class="inline-flex min-h-11 items-center rounded-lg bg-accent px-4 text-sm font-medium text-on-accent"
      >
        Ir para o Dashboard
      </a>
    </div>
  `,
})
export class NotFoundPage {
  protected readonly icon = SearchX;
}
