# 45-quality-testes-unitarios-integracao-contrato

> **IDs:** QUALITY-SPEC-01 (unitários), QUALITY-SPEC-02 (integração), QUALITY-SPEC-03 (contrato) · **Categoria:** Qualidade · **Estado:** proposta para revisão — não aprovada, não executada.

## Objetivo

Detalhar o que cada um dos três primeiros níveis de teste cobre e como é executado, de modo que as regras financeiras e os contratos fiquem protegidos antes dos testes de jornada.

## Escopo

- Inclui: escopo, ferramentas candidatas e critérios por nível.
- Não inclui: cenários por módulo (specs 14 e 31), E2E (QUALITY-SPEC-04).

## Contexto técnico local

### Unitários (QUALITY-SPEC-01)

| Alvo | Cobertura obrigatória |
|---|---|
| Domínio Java | `Money`, estado de meta (700/800/950/1000/1200), transições de alerta, saldo de cofrinho, intervalos de período, variação %, conversão de legados |
| Serviços de aplicação | orquestração com portas falsas; falhas propagam sem efeitos parciais |
| Validadores/mappers | mensagens da origem, `BigDecimal` com escala 2, formatos de data e mês |
| Frontend | utilitários de formatação e máscara, validadores, estado global, `ApiError` (FRONTEND-SPEC-08) |

Sem Spring nem banco nos testes de domínio; tempo injetado. Comparações monetárias por `compareTo`.

### Integração (QUALITY-SPEC-02)

| Alvo | Cobertura obrigatória |
|---|---|
| Persistência | todas as restrições do esquema (BACKEND-SPEC-02), unicidade sob concorrência, cascatas, migrações do zero |
| API | cada operação com sucesso e cada `code` de erro; paginação e `totals` |
| Atomicidade | cadastro de utilizador com semeadura; exclusão de cofrinho; `saveBudget`; saque concorrente |
| Módulos | `ModularityTests`; fronteiras e portas SPI |
| Isolamento | para cada recurso, o utilizador B não acessa dados do A (404) nem os soma em agregações |

PostgreSQL real em contentor (Testcontainers); dados sintéticos; isolamento por teste (transação ou truncamento).

### Contrato (QUALITY-SPEC-03)

- O OpenAPI de cada módulo é válido (3.1), sem `$ref` quebrados nem `operationId` duplicados.
- Controllers implementam as interfaces geradas e as respostas reais validam contra os esquemas (validação de resposta em testes de integração).
- Os tipos do frontend derivam do mesmo contrato e o CI falha se houver diferença não commitada.
- Mudança incompatível (remover campo, tornar obrigatório, mudar tipo) detetada por comparação com a versão anterior e exige aprovação.
- Erros seguem o esquema `ProblemDetail` comum.

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [QUALITY-SPEC-00](../44-quality-estrategia/spec.md)
- [BACKEND-SPEC-09](../14-backend-testes/spec.md)
- [GLOBAL-SPEC-03](../05-global-contratos-e-integracao/spec.md)

## Regras e critérios de aceitação

- [ ] Os testes de domínio rodam sem Spring e sem banco, em segundos.
- [ ] Cada restrição do esquema tem teste de integração que a viola e verifica a resposta.
- [ ] Falhas injetadas no cadastro, na exclusão de cofrinho e em `saveBudget` deixam o estado intacto.
- [ ] Existe teste de isolamento entre dois utilizadores para cada módulo de dados.
- [ ] Toda operação do índice de GLOBAL-SPEC-03 tem teste de contrato de resposta de sucesso e de erro.
- [ ] O CI falha em contrato inválido, diferença de geração ou mudança incompatível não aprovada.

### Skills de implementação

- **Skills obrigatórias:** `java-modern-engineering`, `java-spring-create-module`, `openapi-rest-contract`, `angular-module-scaffold`.
- **Caminhos:** `C:\Users\dowgl\.codex\skills\java-modern-engineering\SKILL.md`; `C:\Users\dowgl\.codex\skills\java-spring-create-module\SKILL.md`; `C:\Users\dowgl\.codex\skills\openapi-rest-contract\SKILL.md`; `.claude/skills/angular-module-scaffold/SKILL.md`.
- **Responsabilidade:** `openapi-rest-contract` valida contratos (`validate.mjs`); `java-spring-create-module` e `angular-module-scaffold` exigem testes e verificação Maven/Angular do comportamento implementado.
- **Momento de utilização:** com cada módulo.
- **Dependências:** QUALITY-SPEC-00; BACKEND-SPEC-02/03.
- **Limitações:** ferramentas de comparação de contratos e de validação de resposta ainda não escolhidas (escolha técnica a registar na implementação).

## Tasks

### Descoberta e decisão

- [ ] Escolher as ferramentas de comparação de contrato e de validação de resposta.

### Validação

- [ ] Configurar os três níveis no CI e registar a primeira execução completa.

## Resultado esperado

- Regras, persistência e contratos protegidos por testes rápidos e confiáveis.
- Preservada a regra de usar banco real em integração.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
