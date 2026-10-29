import { Page, expect, test } from '@playwright/test';
import { addTransaction, createUser, loginThroughUi, money } from './support';
import { goTo, openMenuIfCollapsed, tabTo, toast } from './ui';

const kpi = (page: Page, label: string) => page.locator('app-kpi-card', { hasText: label });

/** "R$ 1.234,56" → 1234.56 (só para somar o que a tela mostra; os valores vêm prontos do servidor). */
function parseMoney(text: string): number {
  const digits = text.replace(/[^\d,-]/g, '').replace(',', '.');
  return Number(digits);
}

async function boxDialogFill(page: Page, fields: Record<string, string>): Promise<void> {
  for (const [name, value] of Object.entries(fields)) {
    await page.getByRole('dialog').locator(`input[formcontrolname="${name}"]`).fill(value);
  }
}

test.describe('J-07 Cofrinho completo', () => {
  test('criar, aportar 1000 e 500, sacar 300 → 1.200; 1.300 é recusado; sem saldo o saque fica indisponível; moeda bloqueada; meta geral independente', async ({ page, baseURL }) => {
    const user = await createUser(baseURL!, 'j07');
    await loginThroughUi(page, user);
    await goTo(page, 'Cofrinhos');

    await page.getByRole('button', { name: 'Novo Cofrinho' }).first().click();
    await boxDialogFill(page, { name: 'Reserva E2E', goal: '2000' });
    await page.getByRole('dialog').getByRole('button', { name: 'Salvar' }).click();
    await expect(toast(page, 'Cofrinho salvo.')).toBeVisible();
    const card = page.locator('app-money-box-card', { hasText: 'Reserva E2E' });
    await expect(card.getByRole('button', { name: 'Saque de Reserva E2E' })).toBeDisabled();
    await expect(card).toContainText('Saldo insuficiente para saque.');

    for (const amount of ['1000', '500']) {
      await card.getByRole('button', { name: 'Aporte em Reserva E2E' }).click();
      await boxDialogFill(page, { amount });
      await page.getByRole('dialog').getByRole('button', { name: 'Depositar' }).click();
      await expect(toast(page, 'Aporte registrado.')).toBeVisible();
    }
    await card.getByRole('button', { name: 'Saque de Reserva E2E' }).click();
    await boxDialogFill(page, { amount: '300' });
    await page.getByRole('dialog').getByRole('button', { name: 'Sacar' }).click();
    await expect(toast(page, 'Saque registrado.')).toBeVisible();
    await expect(card).toContainText(money('1200.00'));
    await expect(kpi(page, 'Saldo Atual')).toContainText(money('1200.00'));

    // 1.300 com saldo 1.200: recusado e o saldo continua
    await card.getByRole('button', { name: 'Saque de Reserva E2E' }).click();
    await boxDialogFill(page, { amount: '1300' });
    await page.getByRole('dialog').getByRole('button', { name: 'Sacar' }).click();
    await expect(page.getByRole('dialog').getByRole('alert')).toContainText('Saldo insuficiente');
    await page.getByRole('dialog').getByRole('button', { name: 'Cancelar' }).click();
    await expect(card).toContainText(money('1200.00'));

    // moeda bloqueada em cofrinho com aportes
    await card.getByRole('button', { name: 'Editar Reserva E2E' }).click();
    await expect(page.getByRole('dialog').locator('select[formcontrolname="currency"]')).toBeDisabled();
    await expect(page.getByRole('dialog')).toContainText('Não é possível alterar a moeda de um cofrinho com aportes.');
    await page.getByRole('dialog').getByRole('button', { name: 'Cancelar' }).click();

    // meta geral independente da soma das metas individuais
    await page.getByRole('button', { name: 'Definir meta geral' }).click();
    await boxDialogFill(page, { goal: '5000' });
    await page.getByRole('dialog').getByRole('button', { name: 'Salvar' }).click();
    await expect(toast(page, 'Meta geral salva.')).toBeVisible();
    await expect(kpi(page, 'Meta Total')).toContainText(money('5000.00'));
    await expect(page.getByText(/24% concluído/)).toBeVisible();
    await expect(card).toContainText(/60% de R\$\s2\.000,00/);

    // reservas aparecem no dashboard sem mexer no saldo do mês
    await goTo(page, 'Dashboard');
    await expect(kpi(page, 'Reservas')).toContainText(money('1200.00'));
    await expect(kpi(page, 'Saldo')).toContainText(money('0.00'));
  });
});

test.describe('J-08 Relatórios', () => {
  test('Semana, Mês e Ano coerentes com os totais; ano com resumo mensal; mês de referência diferente do atual', async ({ page, baseURL }) => {
    const user = await createUser(baseURL!, 'j08');
    // junho/2026 (mês atual do relógio fixo)
    await addTransaction(user, { kind: 'INCOME', amount: '3000.00', date: '2026-06-01', category: 'Salário' });
    await addTransaction(user, { kind: 'INCOME', amount: '250.00', date: '2026-06-24', category: 'Bônus' });
    await addTransaction(user, { kind: 'EXPENSE', amount: '800.00', date: '2026-06-05', category: 'Moradia' });
    await addTransaction(user, { kind: 'EXPENSE', amount: '320.50', date: '2026-06-12', category: 'Alimentação' });
    await addTransaction(user, { kind: 'EXPENSE', amount: '40.00', date: '2026-06-16', category: 'Lazer' });
    await addTransaction(user, { kind: 'EXPENSE', amount: '60.00', date: '2026-06-27', category: 'Carro' });
    // março/2026 (mês diferente do atual)
    await addTransaction(user, { kind: 'INCOME', amount: '1000.00', date: '2026-03-04', category: 'Salário' });
    await addTransaction(user, { kind: 'EXPENSE', amount: '200.00', date: '2026-03-20', category: 'Moradia' });
    await loginThroughUi(page, user);
    await goTo(page, 'Relatórios');

    const barsRows = page.locator('app-income-expense-bars table tbody tr');
    const sumColumn = async (index: number) => {
      const cells = await barsRows.locator(`td:nth-of-type(${index})`).allTextContents();
      return Math.round(cells.reduce((sum, c) => sum + parseMoney(c), 0) * 100) / 100;
    };

    // Mês (junho): barras somam os KPIs
    await expect(page.getByText('Período: 01/06/2026 a 30/06/2026')).toBeVisible();
    await expect(kpi(page, 'Entradas')).toContainText(money('3250.00'));
    await expect(kpi(page, 'Saídas')).toContainText(money('1220.50'));
    await expect(kpi(page, 'Saldo')).toContainText(money('2029.50'));
    await expect(barsRows).toHaveCount(5);
    expect(await sumColumn(1)).toBe(3250);
    expect(await sumColumn(2)).toBe(1220.5);
    await expect(page.locator('app-ranking-list')).toContainText('Moradia');

    // Semana (14/06 a 20/06): só a despesa de 16/06
    await page.getByRole('radio', { name: 'Semana' }).click();
    await expect(page.getByText('Período: 14/06/2026 a 20/06/2026')).toBeVisible();
    await expect(barsRows).toHaveCount(7);
    await expect(kpi(page, 'Saídas')).toContainText(money('40.00'));
    expect(await sumColumn(2)).toBe(40);
    await expect(page.locator('app-monthly-summary-table')).toHaveCount(0);

    // Ano: 12 barras e resumo mensal com a variação do saldo
    await page.getByRole('radio', { name: 'Ano' }).click();
    await expect(barsRows).toHaveCount(12);
    await expect(kpi(page, 'Entradas')).toContainText(money('4250.00'));
    await expect(page.locator('app-monthly-summary-table tbody tr')).toHaveCount(12);
    const march = page.locator('app-monthly-summary-table tbody tr', { hasText: 'Mar/2026' });
    await expect(march).toContainText(money('800.00'));
    await expect(march.locator('td').last()).toHaveText('—'); // fevereiro tem saldo 0: sem base
    const april = page.locator('app-monthly-summary-table tbody tr', { hasText: 'Abr/2026' });
    await expect(april.locator('td').last()).toHaveText('-100,0%');
    await expect(page.locator('app-monthly-summary-table tbody tr', { hasText: 'Jan/2026' }).locator('td').last()).toHaveText('—');

    // mês de referência diferente do atual: março, e as barras seguem somando os KPIs
    await page.getByRole('radio', { name: 'Mês' }).click();
    for (let i = 0; i < 3; i++) {
      await page.getByRole('button', { name: 'Mês anterior' }).click();
    }
    await expect(page.getByText('Período: 01/03/2026 a 31/03/2026')).toBeVisible();
    await expect(kpi(page, 'Entradas')).toContainText(money('1000.00'));
    await expect(kpi(page, 'Saídas')).toContainText(money('200.00'));
    expect(await sumColumn(1)).toBe(1000);
    expect(await sumColumn(2)).toBe(200);
  });
});

test.describe('J-12 Responsividade e teclado', () => {
  test('o fluxo de criar uma despesa funciona em 320 px e só por teclado', async ({ page, baseURL }) => {
    const user = await createUser(baseURL!, 'j12');
    await page.setViewportSize({ width: 320, height: 640 });
    await loginThroughUi(page, user);

    // sem rolagem horizontal em 320 px
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBe(true);

    // a partir daqui, nada de mouse: só Tab, Enter, setas e digitação
    await page.keyboard.press('Tab'); // o primeiro foco precisa existir (link de pular ou o menu)
    await tabTo(page, page.getByRole('button', { name: 'Abrir menu' }));
    await page.keyboard.press('Enter');
    await tabTo(page, page.getByRole('navigation').getByRole('link', { name: 'Lançamentos', exact: true }));
    await page.keyboard.press('Enter');
    await expect(page).toHaveURL(/\/lancamentos$/);

    const open = page.getByRole('button', { name: 'Novo Lançamento' });
    await tabTo(page, open);
    // o anel de foco é visível (não é "outline: none")
    expect(await open.evaluate((el) => getComputedStyle(el).outlineStyle !== 'none' && getComputedStyle(el).outlineWidth !== '0px')).toBe(true);
    await page.keyboard.press('Enter');

    const dialog = page.getByRole('dialog');
    await expect(dialog).toBeVisible();
    await tabTo(page, dialog.locator('input[formcontrolname="description"]'));
    await page.keyboard.type('Almoço por teclado');
    await tabTo(page, dialog.locator('input[formcontrolname="amount"]'));
    await page.keyboard.type('75,40');
    await tabTo(page, dialog.locator('select[formcontrolname="categoryId"]'));
    await page.keyboard.type('Alimenta'); // a seleção por digitação, como um utilizador de teclado
    await tabTo(page, dialog.getByRole('button', { name: 'Salvar' }));
    await page.keyboard.press('Enter');

    await expect(dialog).toBeHidden();
    await expect(page.getByText('Almoço por teclado').filter({ visible: true }).first()).toBeVisible();
    // o foco voltou ao botão que abriu o diálogo
    await expect(open).toBeFocused();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBe(true);

    // Esc fecha um diálogo e devolve o foco
    await page.keyboard.press('Enter');
    await expect(dialog).toBeVisible();
    await page.keyboard.press('Escape');
    await expect(dialog).toBeHidden();
    await expect(open).toBeFocused();
    await openMenuIfCollapsed(page); // o menu continua utilizável
  });
});
