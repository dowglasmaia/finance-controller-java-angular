# 11-backend-autenticacao-e-seguranca

> **ID:** BACKEND-SPEC-05 · **Categoria:** Backend (módulo `identity`) · **Estado:** política aprovada (P-01); detalhes técnicos delegados (P-25) — não executada.

## Objetivo

Especificar autenticação por e-mail e senha e o isolamento dos dados por utilizador, mais a segurança mínima da API, que a origem não tinha.

## Escopo

- Inclui: cadastro, entrada, renovação, saída, utilizador atual; isolamento por proprietário; segurança mínima; semeadura de categorias no cadastro.
- Não inclui: recuperação de senha por e-mail (especificada na [spec 48](../48-backend-recuperacao-de-senha/spec.md)); verificação de e-mail e exclusão de conta (não solicitadas); partilha de dados; autenticação social.

## Contexto técnico local

### Decisão do produto (P-01)

"Autenticação por e-mail e senha; cada utilizador só tem acesso aos seus dados." Na origem não havia utilizadores (DESC-DAT-01); tudo aqui é novo (PROPOSTO nos detalhes).

### Operações (módulo `identity`)

| operationId | Caminho | Entrada | Resposta | Erros |
|---|---|---|---|---|
| `registerUser` | POST `/auth/register` | `email`, `password` | 201 `{user, accessToken}` + cookie de renovação | 400 `VALIDATION_FAILED`, 409 `EMAIL_ALREADY_REGISTERED` |
| `login` | POST `/auth/login` | `email`, `password` | 200 `{user, accessToken}` + cookie | 401 `INVALID_CREDENTIALS` (mensagem genérica), 429 `TOO_MANY_ATTEMPTS` |
| `refreshToken` | POST `/auth/refresh` | cookie de renovação | 200 `{accessToken}` com rotação do cookie | 401 |
| `logout` | POST `/auth/logout` | cookie | 204 e revoga a renovação | — |
| `getCurrentUser` | GET `/auth/me` | token | 200 `{id, email}` | 401 |

Todas as demais rotas exigem `Authorization: Bearer <accessToken>`.

### Regras

- E-mail normalizado em minúsculas, único, validado por formato; senha com mínimo de 8 caracteres e tamanho máximo razoável (sem regras de composição obrigatórias; verificar contra lista de senhas comuns se simples).
- Senha guardada só com *hash* adaptativo (padrão do Spring Security ou Argon2id); nunca devolvida, registada nem enviada em URL.
- *Access token* JWT de ~15 min; *refresh token* aleatório, rotativo, guardado com *hash* e prazo de validade, enviado em cookie `HttpOnly`, `Secure`, `SameSite` (ADR-012). Reuso de um *refresh token* já rotativo revoga a família.
- Limitação de tentativas de entrada por conta e por origem; mensagens genéricas para não revelar se o e-mail existe.
- **Isolamento:** o proprietário vem do token; repositórios filtram por `owner_id`; recurso de outro utilizador devolve 404 (não 403, para não revelar existência); identificadores no corpo ou na URL nunca definem o proprietário.
- **Semeadura:** ao cadastrar, cria as 11 categorias padrão do novo utilizador na mesma transação (BACKEND-SPEC-10).

### Segurança mínima da API

HTTPS fora de desenvolvimento; CORS restrito à origem do frontend com credenciais só para as rotas `/auth`; cabeçalhos de segurança; validação de entrada e limite de tamanho do corpo; Actuator público só em `health`; sem dados financeiros em logs; segredo de assinatura do JWT por variável de ambiente, fora do repositório; dependências verificadas (QUALITY-SPEC-05).

### Pendências

Recuperação de senha: BACKEND-SPEC-18 (esta spec fornece a política de senha e a revogação de sessões que ela usa). P-25 (decidida): e-mail e senha corretos autenticam, sem passo extra de confirmação; o desenho de token de acesso curto + renovação rotativa segue como recomendação (ADR-012).

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [BACKEND-SPEC-02](../08-backend-persistencia-e-migracoes/spec.md)
- [FRONTEND-SPEC-06](../29-frontend-autenticacao-e-protecao-de-rotas/spec.md)
- [Decisões](../00-engenharia-reversa/decisoes-adr.md)

## Regras e critérios de aceitação

- [ ] Cadastrar com e-mail novo cria o utilizador, as 11 categorias padrão e devolve tokens; repetir o e-mail (em qualquer caixa) devolve 409.
  > ⏳ 2026-10-27 — Parcial: cadastro, tokens e 409 para e-mail repetido em outra caixa verificados (`IdentityFlowIntegrationTest`; `./mvnw clean verify` com Docker: 159 testes, 0 falhas, BUILD SUCCESS; também na API real pelo navegador). Falta: as 11 categorias padrão (semeadura na F4).
- [x] Entrada com senha errada e com e-mail inexistente devolvem a mesma resposta 401.
  > ✅ 2026-10-27 — Mesma resposta 401 `INVALID_CREDENTIALS` (`unknownEmailAndWrongPasswordGiveTheSameInvalidCredentialsResponse`). Verificação: `./mvnw clean verify` com Docker: 159 testes, 0 falhas, BUILD SUCCESS.
- [x] Qualquer rota de dados sem token válido devolve 401.
  > ✅ 2026-10-27 — 401 `application/problem+json` sem token (`protectedRouteWithoutTokenIs401ProblemJson`). Verificação: `./mvnw clean verify` com Docker: 159 testes, 0 falhas, BUILD SUCCESS. Os módulos de dados ainda não existem; cada fatia repete a verificação.
- [ ] O utilizador A não lê, altera nem apaga recurso do utilizador B em **nenhum** módulo (404), nem por listagem ou agregação.
  > ⏳ 2026-10-27 — Parcial: no módulo `identity`, cada token só vê o seu utilizador (`eachTokenOnlySeesItsOwnUser`; `./mvnw clean verify` com Docker: 159 testes, 0 falhas, BUILD SUCCESS). Falta: um teste de isolamento por módulo de dados (F4–F10).
- [x] O *refresh token* roda a cada uso; o reuso de um token antigo é recusado e revoga a família.
  > ✅ 2026-10-27 — Rotação e reuso de token antigo recusado com revogação da família (`fullFlowRegisterMeLoginRefreshRotationReuseAndLogout`). Verificação: `./mvnw clean verify` com Docker: 159 testes, 0 falhas, BUILD SUCCESS; recarga e saída na API real pelo navegador.
- [x] Após N tentativas falhas a entrada é limitada (429).
  > ✅ 2026-10-27 — Cinco falhas e a sexta devolve 429 `TOO_MANY_ATTEMPTS` (`repeatedFailuresAreThrottledWith429`; `./mvnw clean verify` com Docker: 159 testes, 0 falhas, BUILD SUCCESS); confirmado na API real. Limitação: limitador em memória, não compartilhado entre instâncias.
- [x] Nenhuma senha, *hash* ou token aparece em respostas de erro ou logs.
  > ✅ 2026-10-27 — Respostas: `invalidRegistrationIs400WithoutEchoingThePassword` e `GlobalExceptionHandlerTest`. Logs: busca por senha, e-mail, JWT, cookie de renovação e segredo nos logs de uma execução real (cadastro, login, falhas, 429) e do `verify` sem ocorrências. Limitação: auditoria manual, sem teste automatizado de logs (spec 13).
  > ⏳ 2026-10-27 — Parcial: respostas de erro não ecoam a senha (`invalidRegistrationIs400WithoutEchoingThePassword`; `GlobalExceptionHandlerTest`, 36 testes). Falta: auditoria dos logs da aplicação.

### Cenários de teste

Cadastro e duplicidade; entrada válida e inválida; expiração do *access token*; rotação e reuso do *refresh*; limitação; isolamento por recurso (categoria, lançamento, meta, cofrinho, movimentação, relatório, preferências); semeadura por utilizador; CORS e cabeçalhos.

### Skills de implementação

- **Skills obrigatórias:** `java-modern-engineering`, `java-spring-create-module`; `openapi-rest-contract` para o contrato `identity`.
- **Caminhos:** `C:\Users\dowgl\.codex\skills\java-modern-engineering\SKILL.md`; `C:\Users\dowgl\.codex\skills\java-spring-create-module\SKILL.md`; `C:\Users\dowgl\.codex\skills\openapi-rest-contract\SKILL.md`.
- **Responsabilidade:** contrato `identity`; código Java e posicionamento do módulo.
- **Momento de utilização:** na fundação, antes de qualquer módulo de dados.
- **Dependências:** BACKEND-SPEC-02 (tabelas `app_user` e `refresh_token`), BACKEND-SPEC-04.
- **Limitações:** **não existe skill de segurança/autenticação** nos diretórios indicados; a escolha de biblioteca (Spring Security) é decisão técnica registada em ADR-012, não uma skill.

## Tasks

### Descoberta e decisão

- [x] Registar a escolha final do mecanismo de token (recomendação: ADR-012) ao implementar.
  > ✅ 2026-10-27 — Registada no ADR-012 de `decisoes-adr.md` (JWT HS256 + *refresh* opaco rotativo com SHA-256, BCrypt custo 12, limitador, CORS, cookie).

### Contrato e dados

- [ ] Prévia aprovada e contrato `identity`; migrações `app_user` e `refresh_token`.
  > ⏳ 2026-10-27 — Parcial: contrato e migração V1 (`app_user`, `refresh_token`) aplicados: `IdentityMigrationTests` (7) e migração aplicada ao PostgreSQL 18.1 do `compose`. Verificação: `./mvnw clean verify` com Docker: 159 testes, 0 falhas, BUILD SUCCESS. Desvios: `requestPasswordReset`/`confirmPasswordReset` na F11; semeadura na F4.
  > ⏳ 2026-10-27 — Parcial: prévia aprovada pelo utilizador e contrato gerado em `openapi/modules/identity.yaml` (+ `common/responses.yaml`, `common/security.yaml`, execution `generate-identity-api` no `pom.xml`). Verificação: `redocly lint` válido nos dois contratos; `mvnw generate-sources` gera `IdentityApi` e modelos em `generated.identity`; `mvnw clean verify` verde. Desvios: `requestPasswordReset`/`confirmPasswordReset` ficam para a F11; semeadura das categorias fica para a F4; `ProblemDetail` ganhou `code` obrigatório. Falta: migrações `app_user` e `refresh_token`. Próxima ação: migração V1 (spec 08).

### Back-end

- [ ] Implementar o módulo `identity`, o filtro de autenticação e o contexto de proprietário usado por todos os módulos.
  > ⏳ 2026-10-27 — Parcial: módulo implementado e verificado com PostgreSQL real (contexto Spring, persistência, fluxo completo; `./mvnw clean verify` com Docker: 159 testes, 0 falhas, BUILD SUCCESS). Falta: semeadura das categorias (F4). Desvio: `GET /api/v1/examples/{id}` passa a exigir token; `InMemoryExampleRepository` deixou de ser `final` (proxy de `@Repository` com JPA).
  > ⏳ 2026-10-27 — Parcial: implementados domínio, 5 casos de uso, adaptadores JPA, segurança (JWT HS256, BCrypt, limitador em memória, `SecurityFilterChain`, CORS, `CurrentOwner`) e `IdentityController` com cookie de renovação. Verificação: `mvnw test` sem os testes que exigem Docker — 135 testes verdes, incluindo `ModularityTests`. Falta: arranque do contexto Spring e testes com PostgreSQL (migração, persistência, `IdentityFlowIntegrationTest` com isolamento), todos escritos mas não executados por falta de Docker; semeadura das categorias (F4). Desvios: `GET /api/v1/examples/{id}` passa a exigir token. Mecanismo de token registado para o ADR-012: JWT HS256 (15 min) + refresh opaco rotativo com hash SHA-256. Próxima ação: subir o Docker e rodar `mvnw clean verify`.

### Validação

- [ ] Executar os testes de segurança e de isolamento com PostgreSQL real.
  > ⏳ 2026-10-27 — Parcial: executados com Testcontainers (PostgreSQL 18.1): `./mvnw clean verify` com Docker: 159 testes, 0 falhas, BUILD SUCCESS. Falta: isolamento nos módulos de dados (F4–F10) e verificador de dependências (QUALITY-SPEC-05).

## Resultado esperado

- Contas por e-mail e senha com dados totalmente separados por utilizador.
- Preservada a regra de não expor detalhes internos nem segredos.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
