import { APIRequestContext, Page, expect, request as playwrightRequest } from '@playwright/test';

/** "Hoje" de todas as jornadas: o relógio do navegador é fixado aqui, então o mês de referência é sempre junho/2026. */
export const FIXED_NOW = new Date('2026-06-15T12:00:00-03:00');
export const PASSWORD = 'E2e-Senha-Sintetica-2026';
export const MAILPIT = process.env.E2E_MAILPIT ?? 'http://localhost:8025';

let counter = 0;

/** E-mail sintético único (a suíte roda em paralelo e repetidamente sobre o mesmo banco). */
export function newEmail(prefix = 'e2e'): string {
  counter += 1;
  return `${prefix}.${Date.now().toString(36)}${counter}.${Math.random().toString(36).slice(2, 8)}@example.com`;
}

export interface Api {
  readonly ctx: APIRequestContext;
  readonly email: string;
  token: string;
  get(path: string): Promise<any>;
  post(path: string, body?: unknown): Promise<any>;
  put(path: string, body?: unknown): Promise<any>;
}

/** Cadastra um utilizador sintético pela API e devolve um cliente autenticado dele (para montar dados sem passar pela tela). */
export async function createUser(baseURL: string, prefix = 'e2e'): Promise<Api> {
  const ctx = await playwrightRequest.newContext({ baseURL, extraHTTPHeaders: { 'X-Forwarded-For': '10.0.0.1' } });
  const email = newEmail(prefix);
  const registered = await ctx.post('/api/v1/auth/register', { data: { email, password: PASSWORD } });
  expect(registered.status(), await registered.text()).toBe(201);
  const api: Api = {
    ctx,
    email,
    token: (await registered.json()).accessToken,
    async get(path) {
      const r = await ctx.get(path, { headers: { Authorization: `Bearer ${api.token}` } });
      expect(r.ok(), `GET ${path}: ${await r.text()}`).toBeTruthy();
      return r.status() === 204 ? null : r.json();
    },
    async post(path, body) {
      const r = await ctx.post(path, { data: body, headers: { Authorization: `Bearer ${api.token}` } });
      expect(r.ok(), `POST ${path}: ${await r.text()}`).toBeTruthy();
      return r.status() === 204 ? null : r.json();
    },
    async put(path, body) {
      const r = await ctx.put(path, { data: body, headers: { Authorization: `Bearer ${api.token}` } });
      expect(r.ok(), `PUT ${path}: ${await r.text()}`).toBeTruthy();
      return r.json();
    },
  };
  return api;
}

export async function categoryId(api: Api, name: string): Promise<string> {
  const list: { id: string; name: string }[] = await api.get('/api/v1/categories');
  const found = list.find((c) => c.name === name);
  expect(found, `categoria ${name}`).toBeTruthy();
  return found!.id;
}

export async function addTransaction(
  api: Api,
  t: { kind: 'INCOME' | 'EXPENSE'; amount: string; date: string; category: string; currency?: 'BRL' | 'EUR'; description?: string },
): Promise<void> {
  await api.post('/api/v1/transactions', {
    kind: t.kind,
    description: t.description ?? `${t.kind === 'INCOME' ? 'Entrada' : 'Saída'} sintética ${t.amount}`,
    amount: t.amount,
    currency: t.currency ?? 'BRL',
    date: t.date,
    categoryId: await categoryId(api, t.category),
    referenceMonth: t.date.slice(0, 7),
  });
}

/** Abre a aplicação com o relógio do navegador fixo em 15/06/2026 e entra pela tela com o utilizador dado. */
export async function loginThroughUi(page: Page, api: Pick<Api, 'email'>): Promise<void> {
  await page.clock.install({ time: FIXED_NOW });
  await page.goto('/entrar');
  await page.getByLabel('E-mail').fill(api.email);
  await page.getByLabel('Senha').fill(PASSWORD);
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page).toHaveURL(/\/$/);
}

/** Dinheiro como o Intl pt-BR formata (com espaço inseparável entre o símbolo e o valor). */
export function money(value: string, symbol: 'R$' | '€' = 'R$'): RegExp {
  const [int, dec = '00'] = value.split('.');
  const grouped = int.replace(/\B(?=(\d{3})+(?!\d))/g, '\\.');
  return new RegExp(`${symbol.replace('$', '\\$')}\\s${grouped},${dec.padEnd(2, '0')}`);
}

export function navLink(page: Page, name: string) {
  return page.getByRole('navigation').getByRole('link', { name });
}

/** Mensagens enviadas ao Mailpit para um destinatário (a API real de captura, nada é simulado). */
export async function mailsTo(email: string): Promise<{ subject: string; text: string }[]> {
  const ctx = await playwrightRequest.newContext();
  try {
    const list = await (await ctx.get(`${MAILPIT}/api/v1/messages?limit=200`)).json();
    const mine = (list.messages as any[]).filter((m) => m.To?.[0]?.Address === email.toLowerCase());
    const out: { subject: string; text: string }[] = [];
    for (const m of mine.reverse()) {
      const full = await (await ctx.get(`${MAILPIT}/api/v1/message/${m.ID}`)).json();
      out.push({ subject: full.Subject, text: full.Text });
    }
    return out;
  } finally {
    await ctx.dispose();
  }
}
