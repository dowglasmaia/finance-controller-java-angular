# 26-frontend-integracao-http

> **ID:** FRONTEND-SPEC-03 · **Categoria:** Frontend · **Estado:** proposta para revisão — não aprovada, não executada.

## Objetivo

Especificar como o frontend consome a API REST: clientes tipados a partir do OpenAPI, tratamento de erros, cancelamento e representação de dinheiro e datas.

## Escopo

- Inclui: camada `data-access`, interceptors, mapeamento de erros, tipos, formatos.
- Não inclui: definição do contrato (GLOBAL-SPEC-03), estado de tela (FRONTEND-SPEC-04).

## Contexto técnico local

### Cliente

- Cada módulo consome apenas as operações do seu contrato aprovado, via serviços em `features/<modulo>/data-access/`.
- Os tipos vêm do OpenAPI; não se replicam DTOs à mão. A ferramenta de geração de tipos/cliente é escolha técnica a registar ao implementar (qualquer uma que gere a partir do mesmo YAML); tipos escritos à mão são proibidos para DTOs de contrato.
- `HttpClient` com `provideHttpClient(withInterceptors([...]))`; URL base de configuração.
- Os tipos genéricos do `HttpClient` não validam o JSON em runtime; validar na fronteira o que for incerto (por exemplo, respostas de importação) — recomendação da skill.

### Interceptors (PROPOSTO)

| Interceptor | Função |
|---|---|
| Erros | converte `application/problem+json` em `ApiError{code, status, message, fieldErrors}`; falha de rede → `NETWORK_ERROR` |
| Autenticação | anexa `Authorization: Bearer`; em 401 tenta uma renovação e repete a chamada, senão leva a `/entrar` (FRONTEND-SPEC-06) |
| (Opcional) Correlação | envia id de correlação para o log do backend |

### Mapeamento de erros para UX

| `code` | Apresentação |
|---|---|
| `VALIDATION_FAILED` | mensagens por campo no formulário |
| `CATEGORY_NAME_DUPLICATED` | erro no campo Nome: `Já existe uma categoria com este nome.` |
| `CATEGORY_IN_USE`, `SUBCATEGORY_PROTECTED`, `CURRENCY_LOCKED`, `CURRENCY_MISMATCH` | toast de erro com a mensagem do servidor |
| `INSUFFICIENT_BALANCE` | toast de erro; o saldo do cofrinho permanece |
| `CATEGORY_TYPE_CONFLICT` | mensagem no campo Tipo |
| `INVALID_CREDENTIALS`, `EMAIL_ALREADY_REGISTERED`, `TOO_MANY_ATTEMPTS` | mensagem no formulário de entrada/cadastro (sem revelar se o e-mail existe) |
| `UNAUTHENTICATED` | renovação silenciosa; se falhar, `/entrar` |
| `PASSWORD_RESET_TOKEN_INVALID` | mensagem "Este link é inválido ou expirou. Peça um novo." com atalho para `/esqueci-senha` |
| `NETWORK_ERROR`, `INTERNAL_ERROR` | estado de erro com "Tentar novamente"; nenhum detalhe interno |

Falha HTTP nunca vira lista vazia nem sucesso (skill `angular-architect`).

### Dinheiro, datas e mês

- Dinheiro trafega como **string decimal**; só se converte para exibição via `Intl`. Pré-visualizações no cliente (por exemplo, "resta X") usam a resposta do servidor, não aritmética em `number`.
- Datas `YYYY-MM-DD` mantidas como string/`Date` local sem fuso; meses `YYYY-MM`.
- `currency` e `month` enviados em toda consulta que os exige; `referenceMonth` enviado ao criar/editar lançamentos (alerta de meta — P-20).

### Cancelamento e concorrência

Troca de mês ou moeda cancela a consulta em curso (`switchMap`/`takeUntilDestroyed`); mutações desativam o botão enquanto pendentes; não há tentativas automáticas em `POST`.

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [GLOBAL-SPEC-03](../05-global-contratos-e-integracao/spec.md)
- [BACKEND-SPEC-04](../10-backend-validacoes-erros-e-excecoes/spec.md)

## Regras e critérios de aceitação

- [x] Nenhum serviço usa operação ou campo fora do contrato aprovado.
  > ✅ 2026-10-27 — `AuthApiService` usa só `registerUser`, `login`, `refreshToken`, `logout`, `getCurrentUser`, com tipos gerados de `identity.yaml` por `openapi-typescript` (`npm run generate:api`).
- [ ] Cada resposta de erro vira `ApiError` e é apresentada conforme a tabela, sem mostrar `detail` técnico.
  > ⏳ 2026-10-27 — Parcial: `api-error.interceptor` converte problem+json em `ApiError` sem `detail` (`api-error.spec.ts`); códigos de autenticação apresentados nas páginas. Falta: demais códigos da tabela (nas fatias dos módulos).
- [ ] Trocar o mês durante uma consulta pendente descarta a resposta antiga.
  > ⏳ 2026-10-27 — Parcial: ainda não há consultas dependentes de mês. Falta: implementar com `switchMap` nas fatias de dados.
- [x] Valores monetários não passam por `parseFloat` nem aritmética de ponto flutuante no cliente.
  > ✅ 2026-10-27 — Verificação: busca por `parseFloat` e `Number(` no código não-teste sem ocorrências; formatação por `Intl` sobre string decimal.
- [ ] Falha de rede resulta em estado de erro com repetição, não em tela vazia.
  > ⏳ 2026-10-27 — Parcial: `NETWORK_ERROR` mapeado e testado; formulários de auth mostram mensagem e mantêm os dados. Falta: estados de erro com "Tentar novamente" nas páginas de dados.

### Skills de implementação

- **Skills obrigatórias:** `angular-module-scaffold`, `angular-architect`.
- **Caminhos:** `.claude/skills/angular-module-scaffold/SKILL.md`; `.claude/skills/angular-architect/SKILL.md`.
- **Responsabilidade:** cliente de dados e mapeamento em `data-access/` por módulo (primeira); tipagem e tratamento de erros (segunda).
- **Momento de utilização:** em cada módulo, depois de o contrato estar aprovado.
- **Dependências:** GLOBAL-SPEC-03, BACKEND-SPEC-03.
- **Limitações:** a skill não cria nem altera contratos.

## Tasks

### Front-end

- [x] Implementar o interceptor de erros e o tipo `ApiError`.
  > ✅ 2026-10-27 — `apiErrorInterceptor`, `ApiError`, `isApiError`, `NETWORK_ERROR`/`INTERNAL_ERROR`; interceptor de autenticação com renovação única. Verificação: `api-error.spec.ts` e `auth.interceptor.spec.ts` (10 testes) verdes.
- [ ] Criar os serviços de dados por módulo com tipos do contrato.
  > ⏳ 2026-10-27 — Parcial: serviço do módulo `identity` criado. Falta: serviços dos demais módulos (F4–F10).

### Validação

- [ ] Testar o mapeamento de cada `code` e o cancelamento por troca de mês.
  > ⏳ 2026-10-27 — Parcial: testados `problem+json`, rede, 401 com renovação. Falta: códigos dos módulos financeiros e o cancelamento por troca de mês.

## Resultado esperado

- Integração tipada, consistente e sem perda de precisão, com falhas sempre visíveis ao utilizador.
- Preservada a regra de que o backend decide e o cliente apresenta.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
