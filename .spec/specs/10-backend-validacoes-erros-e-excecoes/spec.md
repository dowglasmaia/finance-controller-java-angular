# 10-backend-validacoes-erros-e-excecoes

> **ID:** BACKEND-SPEC-04 · **Categoria:** Backend · **Estado:** proposta para revisão — não aprovada, não executada.

## Objetivo

Padronizar as validações, os códigos de erro e o tratamento de exceções do backend, preservando as mensagens da origem e fechando as lacunas em que ela só validava na interface.

## Escopo

- Inclui: níveis de validação, catálogo de erros com estado HTTP e mensagem, regras do tratador global.
- Não inclui: telas de erro (FRONTEND-SPEC-03/05), autenticação (BACKEND-SPEC-05).

## Contexto técnico local

### Níveis de validação (origem × destino)

| Nível | Origem | Destino proposto |
|---|---|---|
| Formato | Validators no formulário (`required`, `positiveAmount`) — DESC-TXN-02/03 | Bean Validation no DTO de entrada **e** repetição no cliente |
| Invariante | `assert` (só debug) e exceções (`CategoryException`, `ArgumentError`, `StateError`) — DESC-TXN-02, DESC-CAT-03, DESC-BOX-04/05 | Construtores do domínio lançam exceções de domínio tipadas |
| Integridade | Ausente no armazenamento — DESC-DAT-02 | Restrições do banco como última defesa (BACKEND-SPEC-02) |

### Catálogo de erros (PROPOSTO; mensagens preservam a origem)

| `code` | HTTP | Mensagem (pt-BR) | Origem |
|---|---|---|---|
| `VALIDATION_FAILED` | 400 | Mensagem geral + `errors[{field, message}]`: `Campo obrigatório`, `Informe um valor`, `Valor inválido`, `O valor deve ser maior que zero`, `Selecione uma categoria` | `validators.dart` |
| `RESOURCE_NOT_FOUND` | 404 | Recurso não encontrado. | — |
| `CATEGORY_NAME_DUPLICATED` | 409 | Já existe uma categoria com este nome. | DESC-CAT-02 |
| `CATEGORY_IN_USE` | 409 | Categoria possui lançamentos vinculados e não pode ser excluída. | DESC-CAT-03 |
| `CATEGORY_TYPE_MISMATCH` | 409 | A categoria não aceita este tipo de lançamento. | DESC-TXN-04 (novo no servidor) |
| `SUBCATEGORY_PROTECTED` | 409 | A subcategoria "Outros" não pode ser excluída. | DESC-SUB-01 |
| `SUBCATEGORY_NAME_DUPLICATED` | 409 | Já existe uma subcategoria com este nome nesta categoria. | DESC-SUB-02 (novo) |
| `SUBCATEGORY_NOT_IN_CATEGORY` | 409 | A subcategoria não pertence à categoria escolhida. | DESC-TXN-05 (novo) |
| `CATEGORY_TYPE_CONFLICT` | 409 | Existem lançamentos incompatíveis com o novo tipo desta categoria. | P-13 (delegada) |
| `UNAUTHENTICATED` | 401 | Sessão inválida ou expirada. | P-01 |
| `INVALID_CREDENTIALS` | 401 | E-mail ou senha inválidos. (mesma mensagem para e-mail inexistente) | P-01 |
| `EMAIL_ALREADY_REGISTERED` | 409 | Este e-mail já está cadastrado. | P-01 |
| `TOO_MANY_ATTEMPTS` | 429 | Muitas tentativas. Aguarde e tente novamente. | P-01 |
| `PASSWORD_RESET_TOKEN_INVALID` | 400 | Este link é inválido ou expirou. Peça um novo. (mesma resposta para token inexistente, expirado ou usado) | P-24, BACKEND-SPEC-18 |
| `CURRENCY_UNSUPPORTED` | 400 | Moeda não suportada: {code}. | DESC-CUR-06 |
| `CURRENCY_MISMATCH` | 409 | A moeda do aporte ({a}) deve ser igual à moeda do cofrinho ({b}). | DESC-BOX-04 |
| `CURRENCY_LOCKED` | 409 | Não é possível alterar a moeda de um cofrinho com aportes. | DESC-BOX-05 |
| `MONEY_BOX_NOT_FOUND` | 404 | Cofrinho não encontrado. | DESC-BOX-04 |
| `INSUFFICIENT_BALANCE` | 409 | Saldo insuficiente: o saque excede o saldo do cofrinho (ou o saldo é zero). A mesma regra recusa editar/excluir movimentação que deixaria o saldo negativo. | P-04 (decidida) |
| `INTERNAL_ERROR` | 500 | Erro inesperado. Tente novamente. (sem detalhes internos) | contexto técnico global |

Todas as mensagens ficam em português do Brasil e podem vir de um catálogo de mensagens (`messages_pt_BR`).

### Regras do tratador global

- Um `@RestControllerAdvice` converte exceções de domínio e de validação em `application/problem+json` (RFC 9457) com `type`, `title`, `status`, `detail`, `code`, `errors`.
- Violações de restrição do banco (unicidade, FK) são traduzidas para o `code` correspondente, sem expor SQL.
- Falhas inesperadas registam o rastro no log com identificador de correlação e devolvem `INTERNAL_ERROR` genérico.
- Recurso de outro utilizador responde `RESOURCE_NOT_FOUND` (404), nunca 403 (P-01).

### Riscos

Mensagens de erro em português ficam no backend; o frontend precisa de um mapeamento por `code` para cenários que exigem texto contextual (ex.: nome da categoria nos alertas).

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [BACKEND-SPEC-01](../07-backend-dominio-e-regras-de-negocio/spec.md)
- [BACKEND-SPEC-03](../09-backend-api-rest-e-openapi/spec.md)

## Regras e critérios de aceitação

- [ ] Toda resposta de erro validada contra `ProblemDetail` com `code` do catálogo.
- [ ] Cada código tem pelo menos um teste que o provoca e confere estado HTTP e mensagem.
- [ ] Nenhuma resposta 4xx/5xx expõe pilha, SQL ou nomes internos.
- [ ] Pedido sobre recurso de outro utilizador devolve 404 `RESOURCE_NOT_FOUND`.
- [ ] Violação concorrente de unicidade de categoria resulta em `CATEGORY_NAME_DUPLICATED`, não em 500.

### Skills de implementação

- **Skills obrigatórias:** `java-modern-engineering`, `java-spring-create-module`; `openapi-rest-contract` para o esquema `ProblemDetail` comum.
- **Caminhos:** `C:\Users\dowgl\.codex\skills\java-modern-engineering\SKILL.md`; `C:\Users\dowgl\.codex\skills\java-spring-create-module\SKILL.md`; `C:\Users\dowgl\.codex\skills\openapi-rest-contract\SKILL.md`.
- **Responsabilidade:** exceções tipadas e tratador (Java); respostas comuns no contrato (`openapi/common/responses.yaml`).
- **Momento de utilização:** antes do primeiro módulo funcional.
- **Dependências:** BACKEND-SPEC-03.
- **Limitações:** nenhuma skill de tratamento de erros dedicada.

## Tasks

### Contrato e dados

- [ ] Incluir `ProblemDetail` e as respostas comuns no contrato (prévia aprovada).

### Back-end

- [ ] Implementar exceções de domínio e o tratador global com o catálogo acima.

### Validação

- [ ] Testar cada código de erro e a tradução de violações do banco.

## Resultado esperado

- Erros previsíveis, estáveis e legíveis, com mensagens idênticas às da origem onde existiam.
- Preservada a ausência de detalhes internos nas respostas.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
