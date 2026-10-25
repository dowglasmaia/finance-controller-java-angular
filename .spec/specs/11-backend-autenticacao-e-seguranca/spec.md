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
- [ ] Entrada com senha errada e com e-mail inexistente devolvem a mesma resposta 401.
- [ ] Qualquer rota de dados sem token válido devolve 401.
- [ ] O utilizador A não lê, altera nem apaga recurso do utilizador B em **nenhum** módulo (404), nem por listagem ou agregação.
- [ ] O *refresh token* roda a cada uso; o reuso de um token antigo é recusado e revoga a família.
- [ ] Após N tentativas falhas a entrada é limitada (429).
- [ ] Nenhuma senha, *hash* ou token aparece em respostas de erro ou logs.

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

- [ ] Registar a escolha final do mecanismo de token (recomendação: ADR-012) ao implementar.

### Contrato e dados

- [ ] Prévia aprovada e contrato `identity`; migrações `app_user` e `refresh_token`.

### Back-end

- [ ] Implementar o módulo `identity`, o filtro de autenticação e o contexto de proprietário usado por todos os módulos.

### Validação

- [ ] Executar os testes de segurança e de isolamento com PostgreSQL real.

## Resultado esperado

- Contas por e-mail e senha com dados totalmente separados por utilizador.
- Preservada a regra de não expor detalhes internos nem segredos.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
