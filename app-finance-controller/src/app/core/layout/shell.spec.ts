import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { AuthService } from '../../features/auth/data-access/auth.service';
import { SessionStore } from '../state/session.store';
import { ShellComponent } from './shell';

@Component({ template: '<h1>Página</h1>' })
class DummyPage {}

describe('ShellComponent', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          {
            path: '',
            component: ShellComponent,
            children: [
              { path: '', pathMatch: 'full', component: DummyPage, data: { showMonth: true } },
              { path: 'lancamentos', component: DummyPage, data: { showMonth: true } },
              { path: 'categorias', component: DummyPage },
            ],
          },
        ]),
        { provide: AuthService, useValue: { logout: vi.fn(() => Promise.resolve()) } },
      ],
    });
    TestBed.inject(SessionStore).set({
      user: { id: 'u1', email: 'ana@exemplo.com' },
      accessToken: 'token',
    });
  });

  async function open(url: string) {
    const harness = await RouterTestingHarness.create(url);
    await harness.fixture.whenStable();
    harness.detectChanges();
    const root: HTMLElement = harness.fixture.nativeElement;
    return { harness, root };
  }

  it('mostra as seis entradas do menu na ordem da spec', async () => {
    const { root } = await open('/');
    const links = Array.from(root.querySelectorAll('aside nav a')).map((a) => a.textContent?.trim());

    expect(links).toEqual(['Dashboard', 'Lançamentos', 'Relatórios', 'Categorias', 'Cofrinhos', 'Meta']);
  });

  it('marca a rota ativa com aria-current="page" (Dashboard só em "/")', async () => {
    const { root } = await open('/lancamentos');
    const current = Array.from(root.querySelectorAll('aside nav [aria-current="page"]'));

    expect(current.map((a) => a.textContent?.trim())).toEqual(['Lançamentos']);
  });

  it('mostra o seletor de mês só nas rotas com data.showMonth', async () => {
    const { harness, root } = await open('/lancamentos');
    expect(root.querySelector('app-month-switcher')).not.toBeNull();

    await harness.navigateByUrl('/categorias');
    harness.detectChanges();
    expect(root.querySelector('app-month-switcher')).toBeNull();
  });

  it('move o foco para o h1 após a navegação', async () => {
    const { harness, root } = await open('/');
    await harness.navigateByUrl('/categorias');
    harness.detectChanges();
    await harness.fixture.whenStable();

    expect(document.activeElement).toBe(root.querySelector('main h1'));
  });

  it('abre a gaveta, prende o foco no botão fechar e fecha com Esc devolvendo o foco', async () => {
    const { harness, root } = await open('/');
    const menuButton = root.querySelector<HTMLButtonElement>('button[aria-label="Abrir menu"]');
    if (!menuButton) {
      throw new Error('botão de menu ausente');
    }

    menuButton.click();
    harness.detectChanges();
    await harness.fixture.whenStable();

    const dialog = root.querySelector('[role="dialog"]');
    expect(dialog).not.toBeNull();
    expect(menuButton.getAttribute('aria-expanded')).toBe('true');

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    harness.detectChanges();

    expect(root.querySelector('[role="dialog"]')).toBeNull();
    expect(document.activeElement).toBe(menuButton);
  });

  it('o menu do utilizador mostra o e-mail e a ação Sair', async () => {
    const { harness, root } = await open('/');
    const trigger = root.querySelector<HTMLButtonElement>('button[aria-controls="menu-usuario"]');
    trigger?.click();
    harness.detectChanges();

    const menu = root.querySelector('#menu-usuario');
    expect(menu?.textContent).toContain('ana@exemplo.com');
    expect(menu?.textContent).toContain('Sair');
  });
});
