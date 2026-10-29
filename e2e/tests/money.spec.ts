import { expect, test } from '@playwright/test';
import { addTransaction, categoryId, createUser, loginThroughUi, money } from './support';
import { createTransactionThroughUi, goTo, saveBudget, selectCurrency, toast } from './ui';

const kpi = (page: import('@playwright/test').Page, label: string) => page.locator('app-kpi-card', { hasText: label });

test.describe('J-01 Primeiro acesso', () => {
  test('um utilizador novo já vê as 11 categorias (7 despesa, 4 receita) com "Outros"', async ({ page, baseURL }) => {
    const user = await createUser(baseURL!, 'j01');
    await loginThroughUi(page, user);
    await goTo(page, 'Categorias');

    await expect(page.getByRole('tab', { name: 'Despesas' })).toHaveAttribute('aria-selected', 'true');
    await expect(page.getByRole('article')).toHaveCount(7);
    await page.getByRole('tab', { name: 'Receitas' }).click();
    await expect(page.getByRole('article')).toHaveCount(4);

    const categories: { name: string; subcategories: { name: string; protected: boolean }[] }[] = await user.get('/api/v1/categories');
    expect(categories).toHaveLength(11);
    for (const category of categories) {
      expect(category.subcategories.filter((s) => s.protected && s.name === 'Outros'), category.name).toHaveLength(1);
    }
  });
});

test.describe('J-02 Despesa em BRL e entrada em EUR', () => {
  test('cada lançamento só aparece na sua moeda e os saldos são separados', async ({ page, baseURL }) => {
    const user = await createUser(baseURL!, 'j02');
    await loginThroughUi(page, user);
    await goTo(page, 'Lançamentos');

    await createTransactionThroughUi(page, { kind: 'Saída', description: 'Mercado do mês', amount: '120,50', category: 'Alimentação' });
    await expect(page.getByText('Mercado do mês').first()).toBeVisible();

    // a entrada em EUR faz a interface passar a mostrar o EUR (P-15)
    await createTransactionThroughUi(page, {
      kind: 'Entrada', description: 'Freelance europeu', amount: '500', category: 'Salário', currency: 'EUR',
    });
    await expect(page.getByRole('button', { name: 'EUR, Euro' })).toHaveAttribute('aria-pressed', 'true');
    await expect(page.getByText('Freelance europeu').first()).toBeVisible();
    await expect(page.getByText('Mercado do mês')).toHaveCount(0);

    await selectCurrency(page, 'BRL');
    await expect(page.getByText('Mercado do mês').first()).toBeVisible();
    await expect(page.getByText('Freelance europeu')).toHaveCount(0);

    await goTo(page, 'Dashboard');
    await expect(kpi(page, 'Saídas')).toContainText(money('120.50'));
    await expect(kpi(page, 'Entradas')).toContainText(money('0.00'));
    await selectCurrency(page, 'EUR');
    await expect(kpi(page, 'Entradas')).toContainText(money('500.00', '€'));
    await expect(kpi(page, 'Saídas')).toContainText(money('0.00', '€'));
  });
});

test.describe('J-03 Navegar meses', () => {
  test('dashboard e lançamentos mudam com o mês; o lançamento futuro aparece no mês certo', async ({ page, baseURL }) => {
    const user = await createUser(baseURL!, 'j03');
    await addTransaction(user, { kind: 'EXPENSE', amount: '100.00', date: '2026-05-10', category: 'Alimentação', description: 'Despesa de maio' });
    await addTransaction(user, { kind: 'EXPENSE', amount: '200.00', date: '2026-06-10', category: 'Alimentação', description: 'Despesa de junho' });
    await addTransaction(user, { kind: 'EXPENSE', amount: '300.00', date: '2026-07-20', category: 'Alimentação', description: 'Despesa futura de julho' });
    await loginThroughUi(page, user);
    const switcher = page.getByRole('group', { name: 'Mês de referência' });

    await expect(switcher).toContainText('Junho 2026');
    await expect(kpi(page, 'Saídas')).toContainText(money('200.00'));
    await goTo(page, 'Lançamentos');
    await expect(page.getByText('Despesa de junho').first()).toBeVisible();
    await expect(page.getByText('Despesa de maio')).toHaveCount(0);

    await page.getByRole('button', { name: 'Mês anterior' }).click();
    await expect(switcher).toContainText('Maio 2026');
    await expect(page.getByText('Despesa de maio').first()).toBeVisible();
    await expect(page.getByText('Despesa de junho')).toHaveCount(0);

    await page.getByRole('button', { name: 'Próximo mês' }).click();
    await page.getByRole('button', { name: 'Próximo mês' }).click();
    await expect(switcher).toContainText('Julho 2026');
    await expect(page.getByText('Despesa futura de julho').first()).toBeVisible();
    await goTo(page, 'Dashboard');
    await expect(kpi(page, 'Saídas')).toContainText(money('300.00'));

    await page.getByRole('button', { name: /voltar ao mês atual/ }).click();
    await expect(switcher).toContainText('Junho 2026');
    await expect(kpi(page, 'Saídas')).toContainText(money('200.00'));
  });
});

test.describe('J-04 Meta com alerta', () => {
  test('com meta 1000: 700 sem aviso, 850 avisa 80%, 1100 estoura, a próxima não repete', async ({ page, baseURL }) => {
    const user = await createUser(baseURL!, 'j04');
    await saveBudget(user, '2026-06', '1000');
    await loginThroughUi(page, user);
    await goTo(page, 'Lançamentos');

    await createTransactionThroughUi(page, { kind: 'Saída', description: 'Gasto 700', amount: '700', category: 'Lazer' });
    await expect(toast(page, 'Lançamento salvo.')).toBeVisible();
    await expect(page.getByText('Você já usou 80% da sua meta mensal.')).toHaveCount(0);

    await createTransactionThroughUi(page, { kind: 'Saída', description: 'Gasto 150', amount: '150', category: 'Lazer' });
    await expect(page.getByText('Você já usou 80% da sua meta mensal.')).toBeVisible();

    await createTransactionThroughUi(page, { kind: 'Saída', description: 'Gasto 250', amount: '250', category: 'Lazer' });
    await expect(page.getByText('Meta mensal estourada!')).toBeVisible();

    await page.getByRole('button', { name: /Fechar|Dispensar/ }).first().click({ trial: true }).catch(() => undefined);
    await page.waitForTimeout(300);
    const before = await page.getByText('Meta mensal estourada!').count();
    await createTransactionThroughUi(page, { kind: 'Saída', description: 'Gasto 10', amount: '10', category: 'Lazer' });
    await expect(toast(page, 'Lançamento salvo.')).toBeVisible();
    expect(await page.getByText('Meta mensal estourada!').count()).toBeLessThanOrEqual(before); // nenhum aviso novo
  });
});

test.describe('J-05 Meta por categoria prioriza a global', () => {
  test('só o alerta da categoria aparece', async ({ page, baseURL }) => {
    const user = await createUser(baseURL!, 'j05');
    await saveBudget(user, '2026-06', '1000', { Alimentação: '100' });
    await addTransaction(user, { kind: 'EXPENSE', amount: '700.00', date: '2026-06-02', category: 'Lazer' });
    await loginThroughUi(page, user);
    await goTo(page, 'Lançamentos');

    // 90 em Alimentação: cruza 80% da categoria (100) e leva o total a 790, abaixo do alerta global
    await createTransactionThroughUi(page, { kind: 'Saída', description: 'Restaurante', amount: '90', category: 'Alimentação' });
    await expect(page.getByText('Você está perto da meta de Alimentação.')).toBeVisible();
    await expect(page.getByText('Você já usou 80% da sua meta mensal.')).toHaveCount(0);

    // 20 mais: a categoria estoura (110) e o total (810) cruza 80% da global; o aviso é o da categoria
    await createTransactionThroughUi(page, { kind: 'Saída', description: 'Sobremesa', amount: '20', category: 'Alimentação' });
    await expect(page.getByText('Meta de Alimentação estourada!')).toBeVisible();
    await expect(page.getByText('Você já usou 80% da sua meta mensal.')).toHaveCount(0);
  });
});

test.describe('J-06 Categoria com lançamentos', () => {
  test('a exclusão é bloqueada com a mensagem; sem lançamentos, exclui com as subcategorias', async ({ page, baseURL }) => {
    const user = await createUser(baseURL!, 'j06');
    await addTransaction(user, { kind: 'EXPENSE', amount: '50.00', date: '2026-06-02', category: 'Moradia' });
    await user.post('/api/v1/categories', {
      name: 'Descartável', type: 'EXPENSE', icon: 'sell', color: '#FF9800', subcategories: ['Sub A', 'Sub B'],
    });
    await loginThroughUi(page, user);
    await goTo(page, 'Categorias');

    await page.getByRole('button', { name: 'Excluir Moradia' }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'Excluir' }).click();
    await expect(page.getByText('Categoria possui lançamentos vinculados e não pode ser excluída.')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Excluir Moradia' })).toBeVisible();

    await page.getByRole('button', { name: 'Excluir Descartável' }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'Excluir' }).click();
    await expect(page.getByText('Categoria excluída.')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Excluir Descartável' })).toHaveCount(0);
    const names = ((await user.get('/api/v1/categories')) as { name: string }[]).map((c) => c.name);
    expect(names).not.toContain('Descartável');
    expect(names).toContain('Moradia');
  });
});

test.describe('J-10 Metas flexíveis e dashboard', () => {
  test('"Sem meta neste mês" desativa só aquele mês e o dashboard mostra o progresso', async ({ page, baseURL }) => {
    const user = await createUser(baseURL!, 'j10');
    await addTransaction(user, { kind: 'EXPENSE', amount: '250.00', date: '2026-06-03', category: 'Lazer' });
    await loginThroughUi(page, user);
    await goTo(page, 'Meta');

    await page.getByLabel('Meta padrão').fill('1000');
    await page.getByRole('button', { name: 'Salvar meta' }).click();
    await expect(toast(page, 'Meta salva.')).toBeVisible();
    await expect(page.getByText('Dentro da meta')).toBeVisible();

    await goTo(page, 'Dashboard');
    const card = page.locator('app-budget-progress-card');
    await expect(card).toContainText('Meta de Junho 2026');
    await expect(card).toContainText('25%');
    await expect(card).toContainText('Dentro da meta');

    // julho: desativa só aquele mês
    await page.getByRole('button', { name: 'Próximo mês' }).click();
    await expect(card).toContainText('Meta de Julho 2026');
    await goTo(page, 'Meta');
    await page.getByLabel('Sem meta neste mês').check();
    await page.getByRole('button', { name: 'Salvar meta' }).click();
    await expect(toast(page, 'Meta salva.')).toBeVisible();
    await goTo(page, 'Dashboard');
    await expect(page.getByText('Sem meta definida para Julho 2026')).toBeVisible();

    await page.getByRole('button', { name: 'Mês anterior' }).click();
    await expect(page.locator('app-budget-progress-card')).toContainText('Meta de Junho 2026');
    await page.getByRole('button', { name: 'Próximo mês' }).click();
    await page.getByRole('button', { name: 'Próximo mês' }).click();
    await expect(page.locator('app-budget-progress-card')).toContainText('Meta de Agosto 2026');
  });
});

// mantém o import usado em outras jornadas
void categoryId;
