import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { LucideAngularModule, Moon, Sun } from 'lucide-angular';
import { ThemeStore } from '../../core/state/theme.store';

/** Alterna claro/escuro. `aria-pressed` = tema escuro ativo. */
@Component({
  selector: 'app-theme-toggle',
  imports: [LucideAngularModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <button
      type="button"
      class="inline-flex h-11 w-11 items-center justify-center rounded-lg border border-border-subtle bg-surface-raised text-content hover:bg-surface"
      aria-label="Tema escuro"
      [attr.aria-pressed]="theme.effectiveDark()"
      [title]="theme.effectiveDark() ? 'Mudar para o tema claro' : 'Mudar para o tema escuro'"
      (click)="theme.toggle()"
    >
      <lucide-icon [img]="theme.effectiveDark() ? sunIcon : moonIcon" [size]="20" aria-hidden="true" />
    </button>
  `,
})
export class ThemeToggleComponent {
  protected readonly theme = inject(ThemeStore);
  protected readonly sunIcon = Sun;
  protected readonly moonIcon = Moon;
}
