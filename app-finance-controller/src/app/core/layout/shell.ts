import { NgTemplateOutlet } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  Injector,
  afterNextRender,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { IsActiveMatchOptions, NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import {
  ArrowLeftRight,
  ChartColumn,
  LayoutDashboard,
  LogOut,
  LucideAngularModule,
  LucideIconData,
  Menu,
  PiggyBank,
  Tags,
  Target,
  User,
  X,
} from 'lucide-angular';
import { filter } from 'rxjs';
import { AuthService } from '../../features/auth/data-access/auth.service';
import { CurrencySelector } from '../../shared/ui/currency-selector';
import { MonthSwitcher } from '../../shared/ui/month-switcher';
import { ThemeToggleComponent } from '../../shared/ui/theme-toggle';
import { ToastService } from '../../shared/ui/toast.service';
import { ReferenceMonthStore } from '../state/reference-month.store';
import { SessionStore } from '../state/session.store';

export const APP_VERSION = 'v0.1.0';

const EXACT: IsActiveMatchOptions = { paths: 'exact', queryParams: 'ignored', fragment: 'ignored', matrixParams: 'ignored' };
const PREFIX: IsActiveMatchOptions = { paths: 'subset', queryParams: 'ignored', fragment: 'ignored', matrixParams: 'ignored' };

export interface NavItem {
  readonly path: string;
  readonly label: string;
  readonly icon: LucideIconData;
  readonly match: IsActiveMatchOptions;
}

/** As seis entradas do menu, na ordem da spec 24. */
export const NAV_ITEMS: readonly NavItem[] = [
  { path: '/', label: 'Dashboard', icon: LayoutDashboard, match: EXACT },
  { path: '/lancamentos', label: 'Lançamentos', icon: ArrowLeftRight, match: PREFIX },
  { path: '/relatorios', label: 'Relatórios', icon: ChartColumn, match: PREFIX },
  { path: '/categorias', label: 'Categorias', icon: Tags, match: PREFIX },
  { path: '/cofrinhos', label: 'Cofrinhos', icon: PiggyBank, match: PREFIX },
  { path: '/metas', label: 'Meta', icon: Target, match: PREFIX },
];

const FOCUSABLE = 'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

@Component({
  selector: 'app-shell',
  imports: [
    NgTemplateOutlet,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    LucideAngularModule,
    CurrencySelector,
    MonthSwitcher,
    ThemeToggleComponent,
  ],
  templateUrl: './shell.html',
  styles: `
    @keyframes drawer-in {
      from {
        transform: translateX(-100%);
      }
      to {
        transform: none;
      }
    }
    .drawer-enter {
      animation: drawer-in 150ms ease-out;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(document:keydown.escape)': 'onEscape()',
    '(document:click)': 'onDocumentClick($event)',
  },
})
export class ShellComponent {
  private readonly router = inject(Router);
  private readonly injector = inject(Injector);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  protected readonly session = inject(SessionStore);
  protected readonly month = inject(ReferenceMonthStore);

  protected readonly items = NAV_ITEMS;
  protected readonly version = APP_VERSION;
  protected readonly menuIcon = Menu;
  protected readonly closeIcon = X;
  protected readonly userIcon = User;
  protected readonly logoutIcon = LogOut;

  protected readonly drawerOpen = signal(false);
  protected readonly userMenuOpen = signal(false);
  protected readonly showMonth = signal(false);

  private readonly main = viewChild<ElementRef<HTMLElement>>('main');
  private readonly drawerPanel = viewChild<ElementRef<HTMLElement>>('drawerPanel');
  private readonly drawerClose = viewChild<ElementRef<HTMLElement>>('drawerClose');
  private readonly menuButton = viewChild<ElementRef<HTMLElement>>('menuButton');
  private readonly userMenu = viewChild<ElementRef<HTMLElement>>('userMenu');
  private readonly userButton = viewChild<ElementRef<HTMLElement>>('userButton');

  constructor() {
    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => {
        this.showMonth.set(this.currentRouteData()['showMonth'] === true);
        this.drawerOpen.set(false);
        this.userMenuOpen.set(false);
        this.focusPageTitle();
      });
  }

  protected openDrawer(): void {
    this.drawerOpen.set(true);
    afterNextRender(() => this.drawerClose()?.nativeElement.focus(), { injector: this.injector });
  }

  protected closeDrawer(restoreFocus = true): void {
    this.drawerOpen.set(false);
    if (restoreFocus) {
      this.menuButton()?.nativeElement.focus();
    }
  }

  /** Foco preso na gaveta: Tab/Shift+Tab circulam entre o primeiro e o último elemento focável. */
  protected onDrawerKeydown(event: KeyboardEvent): void {
    if (event.key !== 'Tab') {
      return;
    }
    const panel = this.drawerPanel()?.nativeElement;
    if (!panel) {
      return;
    }
    const focusable = Array.from(panel.querySelectorAll<HTMLElement>(FOCUSABLE));
    const first = focusable[0];
    const last = focusable[focusable.length - 1];
    if (!first || !last) {
      return;
    }
    const active = panel.ownerDocument.activeElement;
    if (event.shiftKey && active === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && active === last) {
      event.preventDefault();
      first.focus();
    }
  }

  protected toggleUserMenu(): void {
    this.userMenuOpen.update((open) => !open);
  }

  protected onUserMenuFocusOut(event: FocusEvent): void {
    const next = event.relatedTarget;
    if (next instanceof Node && !this.userMenu()?.nativeElement.contains(next)) {
      this.userMenuOpen.set(false);
    }
  }

  protected onEscape(): void {
    if (this.drawerOpen()) {
      this.closeDrawer();
    } else if (this.userMenuOpen()) {
      this.userMenuOpen.set(false);
      this.userButton()?.nativeElement.focus();
    }
  }

  protected onDocumentClick(event: MouseEvent): void {
    const target = event.target;
    if (this.userMenuOpen() && target instanceof Node && !this.userMenu()?.nativeElement.contains(target)) {
      this.userMenuOpen.set(false);
    }
  }

  protected skipToContent(event: Event): void {
    event.preventDefault();
    this.main()?.nativeElement.focus();
  }

  protected async signOut(): Promise<void> {
    this.userMenuOpen.set(false);
    try {
      await this.auth.logout(); // limpa a sessão e navega para /entrar
    } catch {
      this.toast.error('Não foi possível sair. Tente novamente.');
    }
  }

  private currentRouteData(): Record<string, unknown> {
    let route = this.router.routerState.snapshot.root;
    while (route.firstChild) {
      route = route.firstChild;
    }
    return route.data;
  }

  /** Após cada navegação: foco no `h1` da página (ou no `main`), para leitores de tela e teclado. */
  private focusPageTitle(): void {
    afterNextRender(
      () => {
        const main = this.main()?.nativeElement;
        if (!main) {
          return;
        }
        const target = main.querySelector<HTMLElement>('h1') ?? main;
        if (!target.hasAttribute('tabindex')) {
          target.setAttribute('tabindex', '-1');
        }
        target.focus();
      },
      { injector: this.injector },
    );
  }
}
