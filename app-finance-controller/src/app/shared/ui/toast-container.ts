import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import {
  CircleAlert,
  CircleCheck,
  Info,
  LucideAngularModule,
  LucideIconData,
  TriangleAlert,
  X,
} from 'lucide-angular';
import { Toast, ToastKind, ToastService } from './toast.service';

const ICONS: Record<ToastKind, LucideIconData> = {
  success: CircleCheck,
  error: CircleAlert,
  warning: TriangleAlert,
  info: Info,
};

const ICON_COLORS: Record<ToastKind, string> = {
  success: 'text-income',
  error: 'text-expense',
  warning: 'text-warning',
  info: 'text-accent',
};

const BORDER_COLORS: Record<ToastKind, string> = {
  success: 'border-l-income',
  error: 'border-l-expense',
  warning: 'border-l-warning',
  info: 'border-l-accent',
};

const KIND_LABELS: Record<ToastKind, string> = {
  success: 'Sucesso',
  error: 'Erro',
  warning: 'Aviso',
  info: 'Informação',
};

/** Região `aria-live="polite"` com as notificações; erros usam `role="alert"`. */
@Component({
  selector: 'app-toast-container',
  imports: [LucideAngularModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div
      class="pointer-events-none fixed inset-x-4 bottom-4 z-50 flex flex-col gap-2 sm:left-auto sm:w-96"
      aria-live="polite"
      aria-label="Notificações"
      role="region"
    >
      @for (toast of service.toasts(); track toast.id) {
        <div
          class="toast-enter pointer-events-auto flex items-start gap-3 rounded-lg border border-l-4 border-border-subtle bg-surface-raised p-3 text-content shadow-lg"
          [class]="borderClass(toast)"
          [attr.role]="toast.kind === 'error' ? 'alert' : null"
        >
          <lucide-icon [img]="icon(toast)" [size]="20" [class]="iconClass(toast)" aria-hidden="true" />
          <p class="min-w-0 flex-1 break-words text-sm">
            <span class="sr-only">{{ label(toast) }}: </span>{{ toast.message }}
          </p>
          <button
            type="button"
            class="-m-2 inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-lg text-content-muted hover:text-content"
            aria-label="Fechar notificação"
            (click)="service.dismiss(toast.id)"
          >
            <lucide-icon [img]="closeIcon" [size]="18" aria-hidden="true" />
          </button>
        </div>
      }
    </div>
  `,
  styles: `
    @keyframes toast-in {
      from {
        opacity: 0;
        transform: translateY(0.5rem);
      }
      to {
        opacity: 1;
        transform: none;
      }
    }
    .toast-enter {
      animation: toast-in 150ms ease-out;
    }
  `,
})
export class ToastContainerComponent {
  protected readonly service = inject(ToastService);
  protected readonly closeIcon = X;

  protected icon(toast: Toast): LucideIconData {
    return ICONS[toast.kind];
  }

  protected iconClass(toast: Toast): string {
    return `mt-0.5 shrink-0 ${ICON_COLORS[toast.kind]}`;
  }

  protected borderClass(toast: Toast): string {
    return BORDER_COLORS[toast.kind];
  }

  protected label(toast: Toast): string {
    return KIND_LABELS[toast.kind];
  }
}
