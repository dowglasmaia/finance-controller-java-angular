import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { LucideAngularModule, LucideIconData } from 'lucide-angular';

export type KpiTone = 'positive' | 'negative' | 'neutral';

export interface KpiDelta {
  /** Texto pronto, ex.: `+25,0%`. */
  readonly text: string;
  readonly tone: KpiTone;
  readonly icon: LucideIconData;
  /** Frase para leitor de tela, ex.: `aumento de 25,0% sobre o mês anterior`. */
  readonly description: string;
}

const TONE_CLASS: Record<KpiTone, string> = {
  positive: 'text-income',
  negative: 'text-expense',
  neutral: 'text-content',
};

/**
 * Indicador do dashboard: rótulo, valor já formatado e, opcionalmente, a variação sobre o mês anterior. O sinal e o
 * ícone da variação repetem o que a cor diz, então o estado não depende só de cor.
 */
@Component({
  selector: 'app-kpi-card',
  imports: [LucideAngularModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex h-full flex-col gap-1 rounded-card border border-line bg-surface-raised p-4 shadow-card">
      <p class="text-sm text-content-muted">{{ label() }}</p>
      <p class="break-words text-2xl font-semibold" [class]="valueClass()">{{ value() }}</p>
      @if (delta(); as d) {
        <p class="inline-flex items-center gap-1 text-xs font-medium" [class]="deltaClass()">
          <lucide-icon [img]="d.icon" [size]="14" aria-hidden="true" />
          <span aria-hidden="true">{{ d.text }}</span>
          <span class="sr-only">{{ d.description }}</span>
        </p>
      }
    </div>
  `,
})
export class KpiCard {
  readonly label = input.required<string>();
  readonly value = input.required<string>();
  readonly tone = input<KpiTone>('neutral');
  readonly delta = input<KpiDelta | null>(null);

  protected readonly valueClass = computed(() => TONE_CLASS[this.tone()]);
  protected readonly deltaClass = computed(() => TONE_CLASS[this.delta()?.tone ?? 'neutral']);
}
