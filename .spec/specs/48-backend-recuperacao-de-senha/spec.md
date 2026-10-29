# 48-backend-recuperacao-de-senha

> **IDs:** BACKEND-SPEC-18 (recuperação de senha por e-mail) · torna aplicável **BACKEND-SPEC-07** (integração de envio de e-mail) · **Categoria:** Backend (módulo `identity`) · **Estado:** executada na F11 (2026-10-29); medição do tempo de resposta e provedor de produção pendentes — ver evidências.

## Objetivo

Permitir que um utilizador que esqueceu a senha informe o e-mail da conta, receba por e-mail um link e crie uma nova senha, seguindo o padrão de mercado para esse fluxo.

## Escopo

- Inclui: pedido de recuperação, geração e validação do token, envio do e-mail, definição da nova senha, notificação de alteração, limites de abuso.
- Não inclui: verificação de e-mail no cadastro e exclusão de conta (não foram descritas pelo produto; seguem fora do escopo); páginas (FRONTEND-SPEC-16); escolha do provedor de e-mail em produção (depende da hospedagem — P-18).

## Contexto técnico local

### Decisão do produto (P-24)

"Recuperação de senha por e-mail: o utilizador informa o e-mail da conta, recebe um link no e-mail e cria uma nova senha. Usar o padrão de mercado." A origem não tinha contas (DESC-DAT-01); tudo é novo (PROPOSTO nos detalhes). Login continua simples: e-mail e senha corretos autenticam, sem passo extra de confirmação (P-25).

### Fluxo (padrão de mercado)

1. **Pedido:** `POST /auth/password-reset/request` com `email`. A resposta é **sempre a mesma** (`202`, mensagem neutra), exista ou não a conta, para não revelar quem está cadastrado. Tempo de resposta equalizado (envio assíncrono).
2. **Token:** se a conta existe, gera-se um token aleatório de 256 bits (URL-safe), guarda-se **apenas o seu hash** (SHA-256) com validade curta (30 minutos, a confirmar), de uso único; um novo pedido invalida os anteriores da mesma conta.
3. **E-mail:** enviado ao endereço da conta com o link `{FC_APP_BASE_URL}/redefinir-senha?token=…`, texto em português do Brasil, sem a senha, com aviso "se não foi você, ignore esta mensagem" e a validade.
4. **Confirmação:** `POST /auth/password-reset/confirm` com `token` e `newPassword`. Token inexistente, expirado ou já usado → `400 PASSWORD_RESET_TOKEN_INVALID` (mensagem única). Senha fora da política → `400 VALIDATION_FAILED`.
5. **Efeitos do sucesso:** grava o novo *hash* de senha; marca o token como usado; **revoga todas as sessões** (famílias de *refresh token*) da conta; envia e-mail de notificação "sua senha foi alterada". Não há autenticação automática: o utilizador entra depois com a nova senha.

### Segurança

- Token só em *hash* no banco; nunca em logs, respostas ou métricas; comparação em tempo constante.
- Limite de pedidos por e-mail e por origem (por exemplo, 5 por hora), mantendo a resposta neutra (`202`); o excesso não gera e-mail.
- A página do link usa `Referrer-Policy: no-referrer` e remove o token da URL logo ao lê-lo (FRONTEND-SPEC-16); esta é a única exceção à regra de não pôr credenciais em URLs, justificada por ser de uso único e curta duração.
- Política de senha idêntica à do cadastro (BACKEND-SPEC-05).
- Link só HTTPS fora de desenvolvimento; `FC_APP_BASE_URL` vem de configuração, nunca do cabeçalho do pedido (evita *host header injection*).
- E-mail e token não aparecem em logs; falhas de envio registam apenas um identificador interno.

### Integração de e-mail (BACKEND-SPEC-07 passa a ser aplicável)

- Porta de saída `MailSender` no módulo `identity`; adaptador de infraestrutura com Spring Mail (SMTP) configurável: `FC_MAIL_HOST`, `FC_MAIL_PORT`, `FC_MAIL_USERNAME`, `FC_MAIL_PASSWORD`, `FC_MAIL_FROM`, `FC_APP_BASE_URL`, `FC_PASSWORD_RESET_TTL`.
- Envio assíncrono e tolerante a falhas (o pedido do utilizador nunca falha por causa do e-mail); uma nova tentativa limitada; falha persistente apenas registada.
- Desenvolvimento: servidor SMTP de captura em contentor (por exemplo, Mailpit) no `compose`, para ver os e-mails sem enviá-los. Produção: provedor escolhido com a hospedagem (P-18).
- Modelos de e-mail (texto e HTML simples) versionados no código, em pt-BR.

### Dados

Tabela `password_reset_token` (BACKEND-SPEC-02): `id`, `user_id` FK CASCADE, `token_hash` único, `expires_at`, `used_at`, `created_at`; índice em `token_hash`; limpeza periódica de tokens expirados.

### Operações (contrato `identity`)

| operationId | Caminho | Entrada | Resposta | Erros |
|---|---|---|---|---|
| `requestPasswordReset` | POST `/auth/password-reset/request` | `email` | 202 sempre | 400 formato inválido, 429 `TOO_MANY_ATTEMPTS` (opcional; preferir manter 202) |
| `confirmPasswordReset` | POST `/auth/password-reset/confirm` | `token`, `newPassword` | 204 | 400 `PASSWORD_RESET_TOKEN_INVALID`, 400 `VALIDATION_FAILED` |

Ambas públicas (sem *access token*).

### Riscos

Entregabilidade do e-mail depende do provedor e do domínio remetente (SPF/DKIM), a tratar com a hospedagem. Abuso do envio mitigado por limites e resposta neutra.

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [BACKEND-SPEC-05](../11-backend-autenticacao-e-seguranca/spec.md)
- [BACKEND-SPEC-02](../08-backend-persistencia-e-migracoes/spec.md)
- [FRONTEND-SPEC-16](../49-frontend-recuperacao-de-senha/spec.md)

## Regras e critérios de aceitação

- [x] O pedido com e-mail cadastrado e com e-mail inexistente devolve a mesma resposta `202` e corpo; só o primeiro gera e-mail.
  > ✅ 2026-10-29 15:10 +00:00 — `PasswordResetFlowIntegrationTest.existingAndUnknownAccountsGetTheSameAnswerButOnlyTheFirstGetsAnEmail` (PostgreSQL + Mailpit real: mesmo 202 e mesmo corpo; só a conta existente recebe e-mail) e `IdentityControllerTest` (202 neutro). No navegador: a mesma mensagem para o e-mail da conta e para um inexistente.
- [x] O e-mail contém um link com token, a validade e nenhuma senha; o banco guarda só o *hash* do token.
  > ✅ 2026-10-29 15:10 +00:00 — `theEmailCarriesTheLinkAndValidityNoPasswordAndOnlyTheHashIsStored`: remetente configurável, assunto `Redefinição de senha - Finance Controller`, texto com a validade (30 minutos), o aviso "Se não foi você, ignore esta mensagem", HTML com o link, sem senha; token de 43 caracteres (256 bits em base64url); no banco só o SHA-256 (64 hex), diferente do token. `ResetTokenTests`.
- [x] Um token válido define a nova senha uma única vez; reutilizá-lo devolve `PASSWORD_RESET_TOKEN_INVALID`.
  > ✅ 2026-10-29 15:10 +00:00 — `aValidTokenSetsTheNewPasswordOnceAndRevokesEverySessionAndNotifiesByEmail`: 204; reutilizar o token → 400 `PASSWORD_RESET_TOKEN_INVALID`. `twoSimultaneousConfirmsWithTheSameTokenHaveASingleWinner`: 8 `confirm` simultâneos → exatamente 1 vence (consumo por `UPDATE … WHERE used_at IS NULL AND expires_at > now`).
- [x] Token expirado, alterado ou inexistente devolve a mesma resposta de erro.
  > ✅ 2026-10-29 15:10 +00:00 — `unknownAlteredAndExpiredTokensAllGetTheSameError`: os três → 400 `PASSWORD_RESET_TOKEN_INVALID` com o mesmo `title`, `detail` e `status`; `aBlankTokenIs400`.
- [x] Um novo pedido invalida os tokens anteriores da conta.
  > ✅ 2026-10-29 15:10 +00:00 — `aNewRequestInvalidatesThePreviousTokens`: o primeiro token passa a dar `PASSWORD_RESET_TOKEN_INVALID` e o segundo funciona.
- [x] Após redefinir, os *refresh tokens* anteriores deixam de funcionar e a senha antiga deixa de autenticar; a nova autentica.
  > ✅ 2026-10-29 15:10 +00:00 — `aValidTokenSetsTheNewPasswordOnceAndRevokesEverySessionAndNotifiesByEmail`: o cookie de renovação de antes → 401, a senha antiga → 401 e a nova → 200; a resposta não traz cookie (sem login automático). No navegador o mesmo (401 com a antiga, 200 com a nova).
- [x] Um e-mail de notificação de alteração é enviado após o sucesso.
  > ✅ 2026-10-29 15:10 +00:00 — O mesmo teste espera o e-mail `Sua senha foi alterada - Finance Controller` no Mailpit; também visto no navegador.
- [x] Mais de N pedidos na janela não geram novos e-mails e a resposta continua neutra.
  > ✅ 2026-10-29 15:10 +00:00 — `morePasswordResetRequestsThanTheLimitStayNeutralButSendNoMoreEmail`: 7 pedidos para o mesmo e-mail → todos 202, mas só 5 e-mails (5 por hora por e-mail e por origem; `app.password-reset.max-requests`/`window`).
- [x] Falha do servidor de e-mail não altera a resposta do pedido nem expõe detalhes; nenhum log contém o e-mail completo, a senha ou o token.
  > ✅ 2026-10-29 15:10 +00:00 — `PasswordResetMailFailureIntegrationTest` (SMTP inalcançável): mesma resposta 202 para conta existente e inexistente, token criado, e o log traz só `mail not sent ref=<id interno> cause=<tipo>`; nem o e-mail da conta nem o hash do token aparecem na saída. No navegador, a API real não registou o e-mail nem o token em nenhuma linha de log.
- [ ] O tempo de resposta do pedido não distingue contas existentes de inexistentes de forma mensurável.
  > ⏳ 2026-10-29 15:10 +00:00 — Por construção: o pedido só valida o formato e devolve 202; a busca da conta, o token e o e-mail rodam depois, em outra thread (`@Async`, executor `mailTaskExecutor`). Não medi os tempos (falta uma medição estatística de conta existente × inexistente).

### Cenários de teste

Pedido para conta existente/inexistente; expiração; uso duplo; invalidação por novo pedido; revogação de sessões; limite de pedidos; política de senha; falha do envio; corrida de dois `confirm` simultâneos com o mesmo token (só um vence); com servidor SMTP de captura, conteúdo e link do e-mail.

### Skills de implementação

- **Skills obrigatórias:** `java-modern-engineering`, `java-spring-create-module`, `java-persistence-performance`; `openapi-rest-contract` para as duas operações no contrato `identity`.
- **Caminhos:** `C:\Users\dowgl\.codex\skills\java-modern-engineering\SKILL.md`; `C:\Users\dowgl\.codex\skills\java-spring-create-module\SKILL.md`; `C:\Users\dowgl\.codex\skills\openapi-rest-contract\SKILL.md`; `.claude/skills/java-persistence-performance/SKILL.md`.
- **Responsabilidade:** contrato das operações; implementação do caso de uso, da porta e do adaptador de e-mail.
- **Momento de utilização:** depois de `identity` (cadastro e entrada).
- **Dependências:** BACKEND-SPEC-05, 02, 04, 13; INFRA (variáveis e Mailpit).
- **Limitações:** **não existe skill de segurança nem de integração de e-mail** nos diretórios indicados.

## Tasks

### Contrato e dados

- [x] Prévia aprovada e atualização do contrato `identity` com as duas operações; migração `password_reset_token`.
  > ✅ 2026-10-29 15:10 +00:00 — Prévia aprovada pelo utilizador em conversa (2 operações públicas em `identity.yaml`, processamento assíncrono, limite de 5 por hora, consumo atómico do token, Mailpit nos testes, link só HTTPS fora de local/test) e gerada; `V7__password_reset_token.sql` (hash único, FK em cascata, índices por conta e vencimento).

### Back-end

- [x] Implementar o caso de uso, a geração/validação do token, a porta `MailSender` e o adaptador SMTP assíncrono com modelos pt-BR.
  > ✅ 2026-10-29 15:10 +00:00 — `RequestPasswordResetUseCase`, `PasswordResetProcessor` (assíncrono), `ConfirmPasswordResetUseCase`, `ResetToken` (256 bits, SHA-256), portas `MailSender`, `PasswordResetTokenRepository`, `PasswordResetRequestLimiter` e `PasswordResetSettings`, adaptador `SmtpMailSender` (Spring Mail, uma nova tentativa, modelos pt-BR em texto e HTML), limpeza diária dos vencidos (`@Scheduled`), `RefreshSessionRepository.revokeAllOfUser`, e a aplicação não sobe com `FC_APP_BASE_URL` sem HTTPS fora de local/test. `spring.mail` por `FC_MAIL_*`; a saúde da API não depende do servidor de e-mail.

### Validação

- [x] Executar os cenários de teste com PostgreSQL real e servidor SMTP de captura; registar o resultado.
  > ✅ 2026-10-29 15:10 +00:00 — 12 testes novos de integração com PostgreSQL e Mailpit reais (`PasswordResetFlowIntegrationTest` 11, `PasswordResetMailFailureIntegrationTest` 1), 4 de `ResetTokenTests` e 4 de `IdentityControllerTest`; limpeza dos vencidos incluída. `./mvnw clean verify` com Docker: 365 testes, 0 falhas. Para esta suíte o Postgres dos testes passou a `max_connections=400` (muitos contextos em cache esgotavam as 100 conexões). Pendente: provedor de e-mail de produção e SPF/DKIM (P-18).

## Resultado esperado

- Recuperação de senha por e-mail segura e sem enumeração de contas.
- Preservada a regra de nunca registar segredos nem expor detalhes internos.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
