import { defineConfig, devices } from '@playwright/test';

/**
 * Jornadas ponta a ponta (QUALITY-SPEC-04) e acessibilidade (axe) contra o conjunto em execução.
 *
 * O conjunto precisa estar no ar: frontend com proxy de /api (padrão http://localhost:4200) + API + PostgreSQL, e o
 * Mailpit (SMTP 1025, API 8025) para a recuperação de senha. Sem `webServer` aqui de propósito: quem sobe o conjunto
 * (o `compose`, o CI ou o desenvolvedor) é o dono dele. Ver e2e/README.md.
 */
export default defineConfig({
  testDir: './tests',
  // cada jornada cria o seu utilizador sintético: nada depende de ordem ou de dados de outra jornada
  fullyParallel: true,
  workers: process.env.CI ? 2 : 3,
  retries: process.env.CI ? 1 : 0,
  timeout: 60_000,
  expect: { timeout: 15_000 },
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:4200',
    locale: 'pt-BR',
    timezoneId: 'America/Sao_Paulo',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
