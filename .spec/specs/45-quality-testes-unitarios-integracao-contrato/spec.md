# 45-quality-testes-unitarios-integracao-contrato

> **IDs:** QUALITY-SPEC-01 (unitários), QUALITY-SPEC-02 (integração), QUALITY-SPEC-03 (contrato) · **Categoria:** Qualidade · **Estado:** executada na F12 (2026-10-29); restrições do esquema uma a uma, falhas injetadas e execução do CI pendentes.

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

- [x] Os testes de domínio rodam sem Spring e sem banco, em segundos.
  > ✅ 2026-10-29 16:20 +00:00 — `MoneyBoxDomainTests`, `ReportCalculationsTests`, `DomainGuardTests`, `ResetTokenTests` etc. rodam sem Spring nem banco em milissegundos (a suíte de domínio inteira roda em menos de 2 s).
- [ ] Cada restrição do esquema tem teste de integração que a viola e verifica a resposta.
  > ⏳ 2026-10-29 16:20 +00:00 — Cobertas indiretamente pelos fluxos (unicidade de categoria e de e-mail sob concorrência, `CURRENCY_LOCKED`, `INSUFFICIENT_BALANCE`, cascata do cofrinho, utilizador sem DDL, CHECKs de moeda e valor pela validação do domínio). Falta um teste que viole cada CHECK/FK **direto no SQL**, restrição por restrição.
- [ ] Falhas injetadas no cadastro, na exclusão de cofrinho e em `saveBudget` deixam o estado intacto.
  > ⏳ 2026-10-29 16:20 +00:00 — As operações são atómicas por `@Transactional` e a corrida de cofrinho/meta/limiar está testada com concorrência real; não há teste que **injete uma falha no meio** do cadastro, da exclusão do cofrinho e de `saveBudget` para provar o rollback (só `saveBudget` com categoria inexistente deixa tudo como estava).
- [x] Existe teste de isolamento entre dois utilizadores para cada módulo de dados.
  > ✅ 2026-10-29 16:20 +00:00 — Um por módulo de dados: categorias, lançamentos, metas, cofrinhos e movimentações, relatórios, dashboard e preferências (`…FlowIntegrationTest`), e `everyDataRouteNeedsASessionAndAForgedTokenIsRefused` (401 em 14 rotas) mais J-09 no navegador.
- [x] Toda operação do índice de GLOBAL-SPEC-03 tem teste de contrato de resposta de sucesso e de erro.
  > ✅ 2026-10-29 16:20 +00:00 — `ApiContractConformanceIntegrationTest`: as 37 operações dos 9 contratos são chamadas de verdade; o status precisa estar declarado no OpenAPI, o corpo de sucesso é lido no modelo gerado (campo desconhecido reprova) e validado por Bean Validation, e o erro confere com o `ProblemDetail` de `common/responses.yaml`; erro de domínio em 23 das 37 (as demais só têm o 401 global). O teste reprova se alguma `operationId` de qualquer contrato ficar sem verificação.
- [ ] O CI falha em contrato inválido, diferença de geração ou mudança incompatível não aprovada.
  > ⏳ 2026-10-29 16:20 +00:00 — Contrato inválido ou `operationId` repetido já quebra o `generate-sources` do Maven; `security-and-contract.yml` acrescenta a falha por tipos do frontend desatualizados (`npm run generate:api` + `git diff --exit-code`) e por mudança incompatível (oasdiff, em PRs). Testado localmente: `oasdiff breaking` entre o commit e a árvore atual → sem mudança incompatível (as operações novas só acrescentam). **Criado, não executado no GitHub Actions.**

### Skills de implementação

- **Skills obrigatórias:** `java-modern-engineering`, `java-spring-create-module`, `openapi-rest-contract`, `angular-module-scaffold`.
- **Caminhos:** `C:\Users\dowgl\.codex\skills\java-modern-engineering\SKILL.md`; `C:\Users\dowgl\.codex\skills\java-spring-create-module\SKILL.md`; `C:\Users\dowgl\.codex\skills\openapi-rest-contract\SKILL.md`; `.claude/skills/angular-module-scaffold/SKILL.md`.
- **Responsabilidade:** `openapi-rest-contract` valida contratos (`validate.mjs`); `java-spring-create-module` e `angular-module-scaffold` exigem testes e verificação Maven/Angular do comportamento implementado.
- **Momento de utilização:** com cada módulo.
- **Dependências:** QUALITY-SPEC-00; BACKEND-SPEC-02/03.
- **Limitações:** ferramentas de comparação de contratos e de validação de resposta ainda não escolhidas (escolha técnica a registar na implementação).

## Tasks

### Descoberta e decisão

- [x] Escolher as ferramentas de comparação de contrato e de validação de resposta.
  > ✅ 2026-10-29 16:20 +00:00 — Escolha final: validação de resposta **sem biblioteca de terceiros** (o próprio OpenAPI + os modelos gerados + SnakeYAML) em vez de swagger-request-validator/networknt (o 3.1 e as referências entre arquivos não se resolvem bem neles); comparação de versões com **oasdiff** (imagem Docker) só no CI.

### Validação

- [ ] Configurar os três níveis no CI e registar a primeira execução completa.
  > ⏳ 2026-10-29 16:20 +00:00 — Fluxos de CI criados (`backend-ci`, `frontend-ci`, `e2e`, `security-and-contract`); **nenhum rodou no GitHub Actions ainda** (sem repositório remoto verificado, F3). A primeira execução completa fica registrada quando o remoto existir.

## Resultado esperado

- Regras, persistência e contratos protegidos por testes rápidos e confiáveis.
- Preservada a regra de usar banco real em integração.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
