import { expect, test } from '@playwright/test';
import { Api, FIXED_NOW, PASSWORD, addTransaction, createUser, loginThroughUi, mailsTo, newEmail } from './support';
import { goTo, logoutThroughUi, selectCurrency } from './ui';

test.describe('J-00 Cadastro e entrada', () => {
  test('cadastrar, sair, entrar de novo; rota de dados sem sessão leva a /entrar', async ({ page }) => {
    const email = newEmail('j00');
    await page.clock.install({ time: FIXED_NOW });

    await page.goto('/lancamentos');
    await expect(page).toHaveURL(/\/entrar\?returnUrl=%2Flancamentos/);

    await page.getByRole('link', { name: 'Cadastre-se' }).click();
    await page.getByLabel('E-mail').fill(email);
    await page.locator('input[formcontrolname="password"]').fill(PASSWORD);
    await page.locator('input[formcontrolname="confirmPassword"]').fill(PASSWORD);
    await page.getByRole('button', { name: 'Cadastrar' }).click();
    await expect(page).toHaveURL(/\/$/, { timeout: 30_000 }); // o hash bcrypt do cadastro demora sob carga
    await expect(page.getByText(`Resumo de Junho 2026`)).toBeVisible();

    await logoutThroughUi(page, email);
    await page.goto('/categorias');
    await expect(page).toHaveURL(/\/entrar/);

    await page.getByLabel('E-mail').fill(email);
    await page.getByLabel('Senha').fill(PASSWORD);
    await page.getByRole('button', { name: 'Entrar' }).click();
    await expect(page).toHaveURL(/\/categorias$/); // volta à rota pedida
  });

  test('senha errada mostra a mesma mensagem para conta existente e inexistente', async ({ page }) => {
    const user = await createUser(test.info().project.use.baseURL!, 'j00bad');
    const messages: string[] = [];
    for (const email of [user.email, newEmail('naoexiste')]) {
      await page.goto('/entrar');
      await page.getByLabel('E-mail').fill(email);
      await page.getByLabel('Senha').fill('Senha-Errada-Qualquer-1');
      await page.getByRole('button', { name: 'Entrar' }).click();
      const alert = page.getByRole('alert');
      await expect(alert).toBeVisible();
      messages.push((await alert.textContent())!.trim());
    }
    expect(messages[0]).toBe('E-mail ou senha inválidos.');
    expect(messages[1]).toBe(messages[0]);
  });
});

test.describe('J-00b Recuperação de senha', () => {
  test('pedir o link, abrir, definir a nova senha e entrar; antiga e link usado deixam de funcionar', async ({ page, baseURL }) => {
    const user = await createUser(baseURL!, 'j00b');
    await page.clock.install({ time: FIXED_NOW });

    await page.goto('/entrar');
    await page.getByRole('link', { name: 'Esqueci minha senha' }).click();
    await page.getByLabel('E-mail').fill(user.email);
    await page.getByRole('button', { name: 'Enviar link' }).click();
    const neutral = 'Se o e-mail estiver cadastrado, enviaremos um link para redefinir a senha.';
    await expect(page.getByRole('status')).toHaveText(neutral);

    // e-mail real capturado pelo SMTP de captura
    await expect.poll(async () => (await mailsTo(user.email)).length, { timeout: 20_000 }).toBeGreaterThan(0);
    const mail = (await mailsTo(user.email))[0];
    expect(mail.subject).toContain('Redefinição de senha');
    // o link usa FC_APP_BASE_URL da API: o endereço do frontend em teste
    const origin = new URL(baseURL!).origin.replace(/[.*+?^${}()|[\]\\/]/g, '\\$&');
    const link = mail.text.match(new RegExp(`${origin}(/redefinir-senha\\?token=[A-Za-z0-9_-]+)`));
    expect(link, mail.text).toBeTruthy();

    // pedido para e-mail inexistente: mesma resposta e nenhum e-mail
    const other = newEmail('inexistente');
    await page.goto('/esqueci-senha');
    await page.getByLabel('E-mail').fill(other);
    await page.getByRole('button', { name: 'Enviar link' }).click();
    await expect(page.getByRole('status')).toHaveText(neutral);
    await page.waitForTimeout(1500);
    expect(await mailsTo(other)).toHaveLength(0);

    await page.goto(link![1]);
    await expect(page).toHaveURL(/\/redefinir-senha$/); // o token saiu da barra de endereço
    const nova = 'E2e-Nova-Senha-Segura-2026';
    await page.locator('input[formcontrolname="password"]').fill(nova);
    await page.locator('input[formcontrolname="confirmPassword"]').fill(nova);
    await page.getByRole('button', { name: 'Redefinir senha' }).click();
    await expect(page).toHaveURL(/\/entrar/);
    await expect(page.getByText('Senha alterada. Entre com a nova senha.')).toBeVisible();

    await page.getByLabel('E-mail').fill(user.email);
    await page.getByLabel('Senha').fill(PASSWORD);
    await page.getByRole('button', { name: 'Entrar' }).click();
    await expect(page.getByRole('alert')).toContainText('E-mail ou senha inválidos.');

    await page.getByLabel('Senha').fill(nova);
    await page.getByRole('button', { name: 'Entrar' }).click();
    await expect(page).toHaveURL(/\/$/);

    // o link usado não funciona mais
    await logoutThroughUi(page, user.email);
    await page.goto(link![1]);
    await page.locator('input[formcontrolname="password"]').fill('E2e-Outra-Senha-Qualquer-2026');
    await page.locator('input[formcontrolname="confirmPassword"]').fill('E2e-Outra-Senha-Qualquer-2026');
    await page.getByRole('button', { name: 'Redefinir senha' }).click();
    await expect(page.getByText('Este link é inválido ou expirou. Peça um novo.')).toBeVisible();
    await expect(page.getByRole('link', { name: 'Pedir um novo link' })).toBeVisible();
  });
});

test.describe('J-09 Isolamento entre utilizadores', () => {
  test('o utilizador B não vê categorias, lançamentos, metas, cofrinhos nem totais do A', async ({ page, baseURL }) => {
    const a = await createUser(baseURL!, 'j09a');
    const b = await createUser(baseURL!, 'j09b');
    await addTransaction(a, { kind: 'INCOME', amount: '3000.00', date: '2026-06-10', category: 'Salário', description: 'Salário do A' });
    await addTransaction(a, { kind: 'EXPENSE', amount: '800.00', date: '2026-06-11', category: 'Moradia', description: 'Aluguel do A' });
    await a.put('/api/v1/budgets/BRL', { month: '2026-06', monthlyLimit: '2000', categoryLimits: [] });
    const box = await a.post('/api/v1/money-boxes', { name: 'Cofre do A', icon: 'savings', color: '#22C55E', currency: 'BRL' });
    await a.post(`/api/v1/money-boxes/${box.id}/entries`, { kind: 'DEPOSIT', amount: '500', date: '2026-06-12' });
    const aCategory = (await a.get('/api/v1/categories'))[0].id;
    const aTransaction = (await a.get('/api/v1/transactions?month=2026-06&currency=BRL')).items[0].id;

    await loginThroughUi(page, b);
    const dashboard = page.locator('main');
    await expect(dashboard).not.toContainText('3.000,00');
    await expect(dashboard).not.toContainText('800,00');
    await expect(dashboard).not.toContainText('500,00');

    await goTo(page, 'Lançamentos');
    await expect(page.getByText('Salário do A')).toHaveCount(0);
    await expect(page.getByText('Aluguel do A')).toHaveCount(0);
    await goTo(page, 'Cofrinhos');
    await expect(page.getByText('Cofre do A')).toHaveCount(0);
    await expect(page.getByText('Nenhum cofrinho ainda.')).toBeVisible();
    await goTo(page, 'Meta');
    await expect(page.getByLabel('Meta padrão')).toHaveValue('');
    await goTo(page, 'Categorias');
    await expect(page.getByRole('article')).toHaveCount(7); // só as suas 7 de despesa

    // por API: os ids do A respondem 404 para o B
    for (const path of [`/api/v1/categories/${aCategory}`, `/api/v1/transactions/${aTransaction}`, `/api/v1/money-boxes/${box.id}`]) {
      const r = await b.ctx.get(path, { headers: { Authorization: `Bearer ${b.token}` } });
      expect(r.status(), path).toBe(404);
    }
  });
});

test.describe('J-11 Preferências', () => {
  test('tema e moeda persistem ao recarregar e noutro dispositivo; o mês volta ao atual', async ({ page, browser, baseURL }) => {
    const user = await createUser(baseURL!, 'j11');
    await loginThroughUi(page, user);

    await selectCurrency(page, 'EUR');
    await page.getByRole('button', { name: /Tema/ }).click(); // escuro → claro
    await expect(page.locator('html')).not.toHaveClass(/dark/);
    await page.getByRole('button', { name: 'Mês anterior' }).click();
    await expect(page.getByRole('group', { name: 'Mês de referência' })).toContainText('Maio 2026');

    await page.reload();
    await expect(page.getByRole('button', { name: 'EUR, Euro' })).toHaveAttribute('aria-pressed', 'true');
    await expect(page.locator('html')).not.toHaveClass(/dark/);
    await expect(page.getByRole('group', { name: 'Mês de referência' })).toContainText(/Junho 2026|Outubro 2026/); // volta ao mês atual

    // outro dispositivo: contexto novo, sem nada guardado, só o login
    const other = await browser.newContext();
    const second = await other.newPage();
    await second.clock.install({ time: FIXED_NOW });
    await second.goto('/entrar');
    await second.getByLabel('E-mail').fill(user.email);
    await second.getByLabel('Senha').fill(PASSWORD);
    await second.getByRole('button', { name: 'Entrar' }).click();
    await expect(second.getByRole('button', { name: 'EUR, Euro' })).toHaveAttribute('aria-pressed', 'true');
    await expect(second.locator('html')).not.toHaveClass(/dark/);
    await other.close();
  });
});
