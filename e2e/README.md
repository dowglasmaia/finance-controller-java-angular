# E2E e acessibilidade

Jornadas ponta a ponta (QUALITY-SPEC-04, J-00 a J-12) e verificação automática de acessibilidade (axe-core, WCAG 2.0/2.1 A e AA, incluindo contraste) com [Playwright](https://playwright.dev). Dados sintéticos, um utilizador novo por jornada (nada depende de ordem) e o relógio do navegador fixado em **15/06/2026**.

## O que roda

| Arquivo | Jornadas |
|---|---|
| `tests/auth.spec.ts` | J-00 cadastro e entrada, J-00b recuperação de senha (com o Mailpit real), J-09 isolamento entre utilizadores, J-11 preferências |
| `tests/money.spec.ts` | J-01 primeiro acesso, J-02 BRL e EUR, J-03 meses, J-04 e J-05 alertas de meta, J-06 categorias, J-10 metas flexíveis e dashboard |
| `tests/boxes-reports.spec.ts` | J-07 cofrinho completo, J-08 relatórios, J-12 320 px e só por teclado |
| `tests/a11y.spec.ts` | axe em todas as páginas e diálogos, nos temas escuro e claro, em 320, 768 e 1280 px |

## Rodar contra o `compose` (como no CI)

O conjunto precisa estar no ar: PostgreSQL, API, frontend (nginx, com o proxy de `/api`) e Mailpit. Em um projeto `compose` separado, para não mexer no seu ambiente:

```bash
export API_PORT=18080 WEB_PORT=18081 DB_PORT=15432 MAILPIT_UI_PORT=18025 MAILPIT_SMTP_PORT=11025 \
       FC_IMAGE_TAG=e2e FC_APP_BASE_URL=http://localhost:18081 FC_PASSWORD_RESET_MAX_REQUESTS=1000
docker compose -p fc-e2e up -d --build --wait
cd e2e && npm ci && npx playwright install chromium
E2E_BASE_URL=http://localhost:18081 E2E_MAILPIT=http://localhost:18025 npx playwright test
docker compose -p fc-e2e down -v
```

`FC_PASSWORD_RESET_MAX_REQUESTS` sobe o limite de pedidos de recuperação por hora só para o teste repetir a jornada; em produção o padrão é 5.

## Rodar contra o servidor de desenvolvimento

`ng serve` (porta 4200) com o proxy apontando para a API, a API com `FC_MAIL_HOST=localhost` e um Mailpit em `localhost:1025/8025`. Sem variáveis, os testes usam `http://localhost:4200` e `http://localhost:8025`.

## Escrever uma jornada nova

Crie o utilizador e os dados pela API (`createUser`, `addTransaction`… em `tests/support.ts`) e faça pela tela só o que a jornada quer provar. `loginThroughUi` já fixa o relógio. Prefira `formcontrolname` a rótulos para campos obrigatórios (o rótulo leva um `*`).
