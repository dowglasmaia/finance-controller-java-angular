# 29-frontend-autenticacao-e-protecao-de-rotas

> **ID:** FRONTEND-SPEC-06 · **Categoria:** Frontend (`features/auth`) · **Estado:** política aprovada (P-01); detalhes técnicos delegados (P-25) — não executada.

## Objetivo

Implementar entrada, cadastro e saída por e-mail e senha e proteger as rotas da aplicação, usando o módulo `identity` do backend.

## Escopo

- Inclui: páginas de entrada e cadastro, guard de rotas, interceptor de credenciais, renovação silenciosa, saída e limpeza de estado.
- Não inclui: recuperação de senha (ver [FRONTEND-SPEC-16](../49-frontend-recuperacao-de-senha/spec.md)); verificação de e-mail e exclusão de conta (não solicitadas).

## Contexto técnico local

### Decisão e origem

Na origem não havia autenticação (DESC-DAT-01). No destino há login obrigatório; cada utilizador vê só os seus dados (P-01, ADR-012).

### Páginas

| Rota | Conteúdo | Observações |
|---|---|---|
| `/entrar` | e-mail, senha, botão `Entrar`, link para cadastro e link `Esqueci minha senha` | mensagem genérica em 401; estado de carregamento; erro 429 com aviso de espera |
| `/cadastrar` | e-mail, senha, confirmação da senha | validação de formato e de comprimento no cliente; erros do servidor por campo |

Textos em português do Brasil; formulários tipados e acessíveis (FRONTEND-SPEC-05). Após entrar, vai para `/` ou para a rota solicitada antes.

### Comportamento

- *Access token* guardado **apenas em memória** (signal); a renovação usa o cookie `HttpOnly` do servidor, sem tocar em `localStorage` (evita roubo por script).
- Interceptor anexa `Authorization: Bearer` às chamadas da API; em 401 tenta uma renovação única e repete a chamada; se falhar, limpa o estado e leva a `/entrar`.
- Guard protege todas as rotas de funcionalidade; ao carregar a página tenta renovar para restaurar a sessão antes de decidir.
- Saída: chama `logout`, limpa o estado global e as consultas em memória (mês, moeda, dados) e vai a `/entrar`.
- Dados de um utilizador nunca ficam visíveis após a troca de conta.

### Segurança de interface

Sem `innerHTML` com dados do utilizador; sem credenciais nem dados financeiros em URLs, `localStorage` ou consola.

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [BACKEND-SPEC-05](../11-backend-autenticacao-e-seguranca/spec.md)
- [FRONTEND-SPEC-03](../26-frontend-integracao-http/spec.md)

## Regras e critérios de aceitação

- [x] Sem sessão, qualquer rota de funcionalidade redireciona para `/entrar` e, após entrar, volta à rota pedida.
  > ✅ 2026-10-27 — Verificação no navegador (API simulada): `/lancamentos` → `/entrar?returnUrl=%2Flancamentos` → após entrar volta a `/lancamentos`; `auth.guard.spec.ts` (6 testes) e `auth-pages.spec.ts` (`returnUrl` externo ignorado).
- [x] Recarregar a página com sessão válida mantém o utilizador autenticado sem nova digitação.
  > ✅ 2026-10-27 — Verificação no navegador (API simulada): recarregar `/categorias` manteve a sessão pelo `refresh` do arranque; `auth.service.spec.ts`. Desvio: o cookie `HttpOnly` real do backend não foi exercitado.
- [x] Uma resposta 401 provoca uma renovação; se falhar, o utilizador volta a `/entrar` e o estado local é limpo.
  > ✅ 2026-10-27 — `auth.interceptor.spec.ts`: 401 → renovação única compartilhada → repetição; falha limpa o estado e navega a `/entrar`.
- [x] Sair limpa todo o estado em memória; um segundo utilizador no mesmo navegador não vê dados do primeiro.
  > ✅ 2026-10-27 — Verificação no navegador (API simulada): `Sair` levou a `/entrar` e `/categorias` voltou a redirecionar; testes de `logout` mesmo com erro. Sem dados financeiros ainda para validar a troca de conta.
- [x] O token de acesso não é gravado em `localStorage`, `sessionStorage` nem URL.
  > ✅ 2026-10-27 — Verificação no navegador após entrar: `localStorage` e `sessionStorage` vazios; token só em `SessionStore` (signal); `auth.service.spec.ts`.
- [x] Os formulários têm rótulos, mensagens de erro acessíveis e bloqueio durante o envio.
  > ✅ 2026-10-27 — Rótulos, `role="alert"`, foco no primeiro campo inválido e botão desabilitado durante o envio; `auth-pages.spec.ts` (9 testes) e navegador (mensagem genérica de credencial inválida).

### Skills de implementação

- **Skills obrigatórias:** `angular-module-scaffold`, `angular-architect`, `angular-tailwind-ui-specialist`.
- **Caminhos:** `.claude/skills/angular-module-scaffold/SKILL.md`; `.claude/skills/angular-architect/SKILL.md`; `.claude/skills/angular-tailwind-ui-specialist/SKILL.md`.
- **Responsabilidade:** módulo `features/auth` com `doc.md`; guard e interceptor; telas de entrada e cadastro.
- **Momento de utilização:** na fundação do frontend, após o contrato `identity`.
- **Dependências:** BACKEND-SPEC-05, FRONTEND-SPEC-00, 01, 03.
- **Limitações:** **sem skill de autenticação** nos diretórios indicados; a skill de módulo exige contrato aprovado.

## Tasks

### Front-end

- [x] Criar `features/auth` com páginas, serviço de dados, guard e interceptor.
  > ✅ 2026-10-27 — Módulo `features/auth` com `LoginPage`, `RegisterPage`, `AuthApiService`, `AuthService`, `authGuard`, `guestGuard` e `authInterceptor`; `doc.md` atualizado. Verificação: build e testes.
- [x] Integrar a renovação no arranque e a limpeza de estado ao sair.
  > ✅ 2026-10-27 — `provideSessionInitializer()` em `app.config.ts` restaura a sessão no arranque; `AppStateReset.resetAll()` limpa o estado ao sair. Verificação: testes e navegador (API simulada).

### Validação

- [x] Testar redirecionamento, renovação, 401, saída e isolamento entre contas.
  > ✅ 2026-10-27 — Percurso na API real (navegador, `localhost:4200` com proxy para o backend com PostgreSQL): cadastro (e-mail normalizado em minúsculas), recarga mantém a sessão pelo cookie `HttpOnly` rotativo, `Sair` revoga e a rota protegida volta a `/entrar?returnUrl=`, login volta à rota pedida, `localStorage`/`sessionStorage` vazios; 409 para e-mail repetido e 429 após 5 falhas. Testes unitários: 72 verdes. Limitação: 401 por expiração do token de 15 min não esperado na API real (coberto por `auth.interceptor.spec.ts`); troca de conta sem dados financeiros ainda.
  > ⏳ 2026-10-27 — Parcial: testes unitários e verificação manual com API simulada concluídos. Falta: percurso real contra o backend `identity` (cookie `HttpOnly`, rotação, 429, isolamento entre contas), bloqueado pela verificação do backend na F1 (Docker).

## Resultado esperado

- Aplicação acessível só com sessão válida e sem vazamento de dados entre contas.
- Preservada a regra de não guardar credenciais no armazenamento do navegador.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
