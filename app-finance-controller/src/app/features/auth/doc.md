# Auth

## Propósito

Entrada, cadastro e saída por e-mail e senha, restauração da sessão ao carregar a página e proteção das rotas (FRONTEND-SPEC-06). Entrega `data-access`, guards, estado e as páginas `/entrar` (`pages/login`) e `/cadastrar` (`pages/register`), declaradas em `auth.routes.ts` (`AUTH_ROUTES`, espalhadas em `app.routes.ts` fora do shell). A F11 acrescentou as páginas públicas `/esqueci-senha` (`pages/forgot-password`) e `/redefinir-senha` (`pages/reset-password`).

## Jornadas e rotas

| Jornada | Rota | Resultado esperado |
|---|---|---|
| Entrar | `/entrar` (página pendente) | Sessão gravada em memória; segue para `returnUrl` seguro ou `/`. |
| Cadastrar | `/cadastrar` (página pendente) | Utilizador criado e sessão aberta. |
| Restaurar sessão | arranque da aplicação | Recarregar com cookie válido mantém o utilizador autenticado. |
| Proteger rota | qualquer rota de funcionalidade | Sem sessão: redireciona a `/entrar?returnUrl=<caminho>`. |
| Sair | ação do shell | Chama `logout`, limpa o estado (sessão, mês, moeda e tema do dispositivo) e vai a `/entrar`. |
| Esqueci minha senha | `/esqueci-senha` | E-mail da conta; sempre a mesma confirmação neutra (`Se o e-mail estiver cadastrado, enviaremos um link para redefinir a senha.`), também no limite de pedidos (429); erro de formato no campo; botão desabilitado durante o envio. Sem `guestGuard`. |
| Redefinir senha | `/redefinir-senha?token=…` | Lê o token e o **remove da barra de endereço na hora** (`history.replaceState`); nova senha e confirmação (política do cadastro); sucesso → toast `Senha alterada. Entre com a nova senha.` e `/entrar`; sem token ou `PASSWORD_RESET_TOKEN_INVALID` → `Este link é inválido ou expirou. Peça um novo.` com botão para `/esqueci-senha`. Sem `guestGuard`. |

## Contrato consumido

| Operação | Finalidade | Estados tratados |
|---|---|---|
| `registerUser` (`POST /auth/register`) | Cadastro; devolve utilizador e access token. | Sucesso grava a sessão; 400/409 propagados como `ApiError` para a página. |
| `login` (`POST /auth/login`) | Entrada. | Sucesso grava a sessão; 401/429 propagados como `ApiError`. |
| `refreshToken` (`POST /auth/refresh`) | Renova o access token pelo cookie `HttpOnly`. | 401 limpa a sessão. |
| `logout` (`POST /auth/logout`) | Revoga a renovação. | Best-effort: falha não impede a limpeza local. |
| `getCurrentUser` (`GET /auth/me`) | Obtém o utilizador após a renovação. | 401 resulta em "sem sessão". |
| `requestPasswordReset` (`POST /auth/password-reset/request`) | Pede o link por e-mail; pública. | 202 neutro sempre; 400 formato → erro no campo; 429 → mesma mensagem neutra; outros erros → "tente novamente". |
| `confirmPasswordReset` (`POST /auth/password-reset/confirm`) | Define a nova senha com o token; pública. | 204; 400 `PASSWORD_RESET_TOKEN_INVALID` → link inválido; 400 `VALIDATION_FAILED` → política da senha. O token só vai no corpo. |

O OpenAPI (`identity.yaml`) é a fonte de verdade; os tipos vêm de `core/api/generated/identity.ts`. `withCredentials: true` só é usado em register, login, refresh e logout (as que dependem do cookie).

## Estado e responsabilidades

- `SessionStore` (`core/state`) é o único dono de `user`, `accessToken` e `initialized`. O token fica só em memória: nunca em `localStorage`, `sessionStorage` ou URL.
- `AuthService` (fachada): `register`, `login`, `refresh` (single-flight), `restoreSession`, `ensureInitialized`, `logout`.
- `AuthApiService`: cliente HTTP do contrato, sem estado.
- `AppStateReset` (`core/state`) limpa sessão e mês e executa callbacks registrados por outras funcionalidades.
- `authGuard` / `guestGuard` / `safeReturnUrl` em `guards/auth.guard.ts`.
- `provideSessionInitializer()` restaura a sessão antes do primeiro roteamento.

## Regras e dependências

- Registro em `app.config.ts`: `provideHttpClient(withInterceptors([authInterceptor, apiErrorInterceptor]))` e `provideSessionInitializer()`.
- Ordem dos interceptors: `authInterceptor` é o mais externo e `apiErrorInterceptor` o mais interno. Assim, o auth interceptor vê o erro já como `ApiError` e o `apiErrorInterceptor` normaliza tudo para o resto da aplicação. O auth interceptor aceita `HttpErrorResponse` ou `ApiError` com `status` 401, portanto a ordem inversa também funciona.
- Anexa `Authorization: Bearer` apenas a URLs que começam com `API_BASE_URL`, exceto `/auth/login`, `/auth/register`, `/auth/refresh` e `/auth/logout`.
- Em 401 de chamada protegida: uma renovação compartilhada por todas as chamadas concorrentes (`AuthService.refresh`), depois repete a chamada uma vez. Se outra chamada já renovou o token enquanto esta estava em voo, apenas repete. Se a renovação falhar: `AppStateReset.resetAll()` e navegação para `/entrar`; o erro original segue para quem chamou.
- `safeReturnUrl(raw, fallback = '/')` só aceita caminhos internos que começam com `/` (recusa `//`, `/\`, esquemas e caracteres de controle). O guard grava no `returnUrl` apenas o caminho, sem query nem fragmento (podem conter dados financeiros). As páginas devem usá-lo ao redirecionar após entrar.
- `core/http/auth.interceptor.ts` depende de `AuthService` desta feature (dependência intencional core → auth, necessária para a renovação).

## Decisões e pendências

- Moeda e tema: o servidor é o dono por utilizador (F10, `PreferencesSync` em `core/state`). `AuthService` chama `PreferencesSync.load()` depois de entrar, cadastrar e restaurar a sessão (o que o servidor tem prevalece; falha ao ler mantém o do dispositivo). `localStorage` (`fc.currency`, `fc.theme`, em `try/catch`) é só cópia local para antes do login e é **limpo no logout** (`AppStateReset` devolve BRL e tema escuro).
- Recuperação de senha: o token do link nunca vai a `localStorage`/`sessionStorage`, a sinais, a logs nem à URL de chamadas; fica num campo privado da página e é descartado ao concluir. `Referrer-Policy: no-referrer` no `index.html` e no nginx.
- Mês de referência não é persistido e volta ao mês atual no logout.
- Mensagens por `code` (`INVALID_CREDENTIALS`, `EMAIL_ALREADY_REGISTERED`, `TOO_MANY_ATTEMPTS`, `VALIDATION_FAILED` por campo) são tratadas nas páginas, usando `ApiError.code` e `fieldErrors`. No cadastro, `EMAIL_ALREADY_REGISTERED` mostra uma mensagem neutra no formulário (não no campo).
- Páginas: formulários tipados com `FormField`, botão bloqueado durante o envio, foco no primeiro campo inválido; após entrar vai para `returnUrl` (via `safeReturnUrl`) ou `/`.

## Validação

- Testes Vitest ao lado dos arquivos: `api-error`, `auth.interceptor` (bearer, 401 → refresh único → repetição, falha de renovação, concorrência), `stores` (mês, moeda com storage indisponível, tema, reset), `auth.service` (restore sucesso/falha, logout com erro, token fora do storage), `auth.guard` (redirecionamento, guest, `safeReturnUrl`).
- `pages/auth-pages.spec.ts`: validação sem chamar a API, `returnUrl` interno/externo, `INVALID_CREDENTIALS`, `TOO_MANY_ATTEMPTS`, erros do servidor por campo, cadastro.
- Executado em 2026-10-27: `npm test` (Vitest) e `npm run build`; verificação manual no navegador contra uma API simulada (a API real não foi usada).
