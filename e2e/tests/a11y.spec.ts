import AxeBuilder from '@axe-core/playwright';
import { Page, expect, test } from '@playwright/test';
import { Api, FIXED_NOW, addTransaction, createUser, loginThroughUi } from './support';
import { goTo } from './ui';

/**
 * Acessibilidade automática (axe-core, regras WCAG 2.0/2.1 A e AA, incluindo contraste, que só funciona num navegador
 * de verdade) em todas as páginas e diálogos, nos dois temas e em 320, 768 e 1280 px. Qualquer violação reprova.
 */
const WIDTHS = [320, 768, 1280] as const;
const THEMES = ['dark', 'light'] as const;
type Theme = (typeof THEMES)[number];

async function applyTheme(page: Page, theme: Theme): Promise<void> {
  const dark = await page.evaluate(() => document.documentElement.classList.contains('dark'));
  if ((theme === 'dark') !== dark) {
    await page.evaluate((mode) => {
      localStorage.setItem('fc.theme', mode);
      document.documentElement.classList.toggle('dark', mode === 'DARK');
    }, theme === 'dark' ? 'DARK' : 'LIGHT');
  }
}

async function scan(page: Page, label: string): Promise<void> {
  const results = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa']).analyze();
  const summary = results.violations.map((v) => ({
    id: v.id,
    impact: v.impact,
    help: v.help,
    nodes: v.nodes.slice(0, 4).map((n) => `${n.target.join(' ')} :: ${(n.failureSummary ?? '').split('\n').slice(0, 2).join(' | ')}`),
  }));
  expect(summary, `${label}: ${JSON.stringify(summary, null, 1)}`).toEqual([]);
}

async function seededUser(baseURL: string, prefix: string): Promise<Api> {
  const user = await createUser(baseURL, prefix);
  await addTransaction(user, { kind: 'INCOME', amount: '3000.00', date: '2026-06-01', category: 'Salário' });
  await addTransaction(user, { kind: 'EXPENSE', amount: '800.00', date: '2026-06-05', category: 'Moradia' });
  await addTransaction(user, { kind: 'EXPENSE', amount: '320.50', date: '2026-06-12', category: 'Alimentação' });
  await user.put('/api/v1/budgets/BRL', { month: '2026-06', monthlyLimit: '2000', categoryLimits: [] });
  const box = await user.post('/api/v1/money-boxes', { name: 'Reserva', icon: 'savings', color: '#6D28D9', currency: 'BRL', goal: '2000' });
  await user.post(`/api/v1/money-boxes/${box.id}/entries`, { kind: 'DEPOSIT', amount: '1000', date: '2026-06-10', note: 'Aporte' });
  await user.put('/api/v1/reserves/BRL/goal', { amount: '5000' });
  return user;
}

for (const theme of THEMES) {
  for (const width of WIDTHS) {
    test.describe(`${theme} ${width}px`, () => {
      test.use({ viewport: { width, height: 800 } });

      test('páginas públicas', async ({ page }) => {
        await page.clock.install({ time: FIXED_NOW });
        for (const path of ['/entrar', '/cadastrar', '/esqueci-senha', '/redefinir-senha', '/nao-existe']) {
          await page.goto(path);
          await applyTheme(page, theme);
          await page.waitForTimeout(150);
          await scan(page, `${theme} ${width} ${path}`);
        }
        // formulário com erros de validação visíveis
        await page.goto('/entrar');
        await applyTheme(page, theme);
        await page.getByRole('button', { name: 'Entrar' }).click();
        await expect(page.getByText('Campo obrigatório').first()).toBeVisible();
        await scan(page, `${theme} ${width} /entrar com erros`);
      });

      test('páginas autenticadas com dados', async ({ page, baseURL }) => {
        const user = await seededUser(baseURL!, 'a11y');
        await loginThroughUi(page, user);
        await applyTheme(page, theme);
        for (const [name, check] of [
          ['Dashboard', 'Resumo de Junho 2026'],
          ['Lançamentos', 'Lançamentos'],
          ['Categorias', 'Categorias'],
          ['Meta', 'Meta de Gastos'],
          ['Cofrinhos', 'Cofrinhos'],
          ['Relatórios', 'Relatórios'],
        ] as const) {
          await goTo(page, name);
          await expect(page.getByRole('heading', { level: 1 })).toContainText(check === 'Resumo de Junho 2026' ? /Bom dia|Boa tarde|Boa noite/ : check);
          await page.waitForTimeout(300);
          await scan(page, `${theme} ${width} ${name}`);
        }
        await page.getByRole('radio', { name: 'Ano' }).click();
        await expect(page.locator('app-monthly-summary-table')).toBeVisible();
        await scan(page, `${theme} ${width} Relatórios (Ano)`);
        await page.getByRole('radio', { name: 'Semana' }).click();
        await page.waitForTimeout(300);
        await scan(page, `${theme} ${width} Relatórios (Semana)`);
      });

      test('diálogos abertos', async ({ page, baseURL }) => {
        const user = await seededUser(baseURL!, 'a11yd');
        await loginThroughUi(page, user);
        await applyTheme(page, theme);

        await goTo(page, 'Lançamentos');
        await page.getByRole('button', { name: 'Novo Lançamento' }).click();
        await expect(page.getByRole('dialog')).toBeVisible();
        await scan(page, `${theme} ${width} diálogo de lançamento`);
        await page.getByRole('dialog').getByRole('button', { name: 'Salvar' }).click(); // erros de validação
        await scan(page, `${theme} ${width} diálogo de lançamento com erros`);
        await page.keyboard.press('Escape');

        await goTo(page, 'Categorias');
        await page.getByRole('button', { name: 'Nova Categoria' }).click();
        await scan(page, `${theme} ${width} diálogo de categoria`);
        await page.keyboard.press('Escape');
        await page.getByRole('button', { name: 'Excluir Moradia' }).click();
        await scan(page, `${theme} ${width} confirmação de exclusão`);
        await page.getByRole('button', { name: 'Cancelar' }).click();

        await goTo(page, 'Cofrinhos');
        await page.getByRole('button', { name: 'Novo Cofrinho' }).first().click();
        await scan(page, `${theme} ${width} diálogo de cofrinho`);
        await page.keyboard.press('Escape');
        await page.getByRole('button', { name: 'Aporte em Reserva' }).click();
        await scan(page, `${theme} ${width} diálogo de movimentação`);
        await page.keyboard.press('Escape');
        await page.getByRole('button', { name: 'Editar meta geral' }).click();
        await scan(page, `${theme} ${width} diálogo de meta geral`);
        await page.keyboard.press('Escape');
        await page.getByRole('button', { name: /Reserva \(ver aportes\)/ }).click();
        await expect(page.getByRole('dialog')).toContainText('Aportes');
        await scan(page, `${theme} ${width} detalhe do cofrinho`);
      });
    });
  }
}
