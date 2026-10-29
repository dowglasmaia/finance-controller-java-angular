import { Locator, Page, expect } from '@playwright/test';
import { Api, categoryId } from './support';

export async function openMenuIfCollapsed(page: Page): Promise<void> {
  const menu = page.getByRole('button', { name: 'Abrir menu' });
  if (await menu.isVisible()) {
    await menu.click();
  }
}

export async function goTo(page: Page, name: 'Dashboard' | 'Lançamentos' | 'Relatórios' | 'Categorias' | 'Cofrinhos' | 'Meta'): Promise<void> {
  await openMenuIfCollapsed(page);
  await page.getByRole('navigation').getByRole('link', { name, exact: true }).click();
}

export async function selectCurrency(page: Page, currency: 'BRL' | 'EUR'): Promise<void> {
  await page.getByRole('button', { name: currency === 'BRL' ? 'BRL, Real brasileiro' : 'EUR, Euro' }).click();
}

export async function logoutThroughUi(page: Page, email: string): Promise<void> {
  await openMenuIfCollapsed(page);
  await page.getByRole('button', { name: new RegExp(email.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')) }).click();
  await page.getByRole('button', { name: 'Sair' }).click();
  await expect(page).toHaveURL(/\/entrar/);
}

/** Cria um lançamento pelo diálogo da tela de Lançamentos (a data padrão é "hoje", fixada em 15/06/2026). */
export async function createTransactionThroughUi(
  page: Page,
  t: { kind: 'Entrada' | 'Saída'; description: string; amount: string; category: string; currency?: 'BRL' | 'EUR' },
): Promise<void> {
  await page.getByRole('button', { name: 'Novo Lançamento' }).click();
  const dialog = page.getByRole('dialog');
  await dialog.getByRole('radio', { name: t.kind, exact: true }).click();
  await dialog.locator('input[formcontrolname="description"]').fill(t.description);
  await dialog.locator('input[formcontrolname="amount"]').fill(t.amount);
  if (t.currency) {
    await dialog.locator('select[formcontrolname="currency"]').selectOption(t.currency);
  }
  await dialog.locator('select[formcontrolname="categoryId"]').selectOption({ label: t.category });
  await dialog.getByRole('button', { name: 'Salvar' }).click();
  await expect(dialog).toBeHidden();
}

/** Um toast com o texto dado (as notificações ficam num contêiner próprio). */
export function toast(page: Page, text: string | RegExp): Locator {
  return page.getByText(text).first();
}

export async function saveBudget(api: Api, month: string, monthly: string, categories: Record<string, string> = {}): Promise<void> {
  const categoryLimits = [];
  for (const [name, limit] of Object.entries(categories)) {
    categoryLimits.push({ categoryId: await categoryId(api, name), limit });
  }
  await api.put('/api/v1/budgets/BRL', { month, monthlyLimit: monthly, categoryLimits });
}

/** Avança com Tab até o elemento ficar com foco (a jornada só por teclado); falha se não chegar. */
export async function tabTo(page: Page, target: Locator, maxTabs = 80): Promise<void> {
  for (let i = 0; i < maxTabs; i++) {
    if (await target.evaluate((el) => el === document.activeElement).catch(() => false)) {
      return;
    }
    await page.keyboard.press('Tab');
  }
  throw new Error('o elemento nunca recebeu foco por teclado');
}
