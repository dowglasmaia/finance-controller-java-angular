import { Injectable, signal } from '@angular/core';

export type ToastKind = 'success' | 'error' | 'warning' | 'info';

export interface Toast {
  readonly id: number;
  readonly kind: ToastKind;
  readonly message: string;
}

export const TOAST_DURATION_MS = 5000;

/** Fila de notificações. Auto-dispensa após ~5 s; a renderização é do `ToastContainerComponent`. */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private readonly state = signal<readonly Toast[]>([]);
  private readonly timers = new Map<number, ReturnType<typeof setTimeout>>();
  private nextId = 1;

  readonly toasts = this.state.asReadonly();

  show(kind: ToastKind, message: string, durationMs: number = TOAST_DURATION_MS): number {
    const id = this.nextId++;
    this.state.update((list) => [...list, { id, kind, message }]);
    this.timers.set(
      id,
      setTimeout(() => this.dismiss(id), durationMs),
    );
    return id;
  }

  success(message: string): number {
    return this.show('success', message);
  }

  error(message: string): number {
    return this.show('error', message);
  }

  warning(message: string): number {
    return this.show('warning', message);
  }

  info(message: string): number {
    return this.show('info', message);
  }

  dismiss(id: number): void {
    const timer = this.timers.get(id);
    if (timer !== undefined) {
      clearTimeout(timer);
      this.timers.delete(id);
    }
    this.state.update((list) => list.filter((toast) => toast.id !== id));
  }
}
