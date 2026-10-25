# 01-scaffold-backend-spring-modulith

## Objetivo

Criar o esqueleto do backend do Finance Controller — monólito modular em Java 25 e Spring Boot, com Clean Architecture, contrato OpenAPI como fonte de verdade, Docker e observabilidade básica — para que as próximas specs de funcionalidade partam de uma base compilável, testada e executável.

## Escopo

- Inclui:
  - Geração do projeto com a skill `java-spring-scaffold`, em modo Contract First, com Maven Wrapper.
  - Estrutura de monólito modular com Spring Modulith e camadas de Clean Architecture (api, application, domain, infrastructure), conforme o exemplo gerado.
  - Contrato OpenAPI 3.1.0 em `src/main/resources/openapi/openapi.yaml`, com interfaces e DTOs gerados no build.
  - Exemplo funcional gerado pela skill (`GET /api/v1/examples/{id}`, persistência em memória) e respetivos testes.
  - Spring Boot Actuator para observabilidade básica (health e demais endpoints definidos pela skill).
  - `Dockerfile` e `compose.yml` gerados pela skill.
  - Validação completa do projeto gerado (build, testes, arranque, health e chamada HTTP de exemplo).
- Não inclui:
  - PostgreSQL, JPA e migrações de banco. A skill só suporta `database: none`; a persistência real fica para uma spec própria.
  - Spring AI e integração com provedores de IA (`springAi: false`).
  - Entidades, regras e endpoints do domínio financeiro (lançamentos, categorias, metas, cofrinhos etc.).
  - Frontend Angular, autenticação e autorização.
  - Remoção ou renomeação do exemplo gerado; ele é a verificação de aceitação do scaffold e será substituído pelas specs de funcionalidade.

## Contexto técnico local

Parâmetros de geração (configuração JSON da skill):

```json
{
  "projectName": "finance-controller",
  "groupId": "com.dowglasmaia",
  "artifactId": "spring-service",
  "packageName": "com.dowglasmaia.financecontroller",
  "javaVersion": 25,
  "buildTool": "maven",
  "architecture": "modular-monolith",
  "modularity": "spring-modulith",
  "internalArchitecture": "clean",
  "springAi": false,
  "aiProvider": "openai",
  "database": "none",
  "docker": true,
  "observability": true,
  "generateExamples": true,
  "contractFirst": true,
  "openapiSpec": null
}
```

Decisões e ajustes em relação ao pedido original:

- **Java 25.** O pedido indicava 25 no texto e 21 no JSON. Prevalece 25, alinhado ao [contexto técnico global](../../memory/contexto-tecnico.md); a skill suporta 21 e 25.
- **`projectName` em kebab-case.** A skill rejeita `financeController` (exige slug minúsculo). Usa-se `finance-controller`.
- **`packageName` em minúsculas.** `com.dowglasmaia.financecontroller` segue a convenção Java para pacotes; a skill aceitaria a versão com maiúscula.
- **`artifactId` mantido** como `spring-service`, conforme pedido. Pode ser renomeado antes da geração, se preferido.
- **`database: none`.** PostgreSQL foi pedido, mas a skill rejeita qualquer valor diferente de `none`. O Postgres será adicionado numa spec posterior sobre o projeto gerado, incluindo o serviço no `compose.yml`.
- **`aiProvider: openai`** é apenas o valor padrão exigido pela skill; sem efeito, pois `springAi` é `false`.
- **`openapiSpec: null`.** Usa-se o contrato de exemplo da skill; contratos reais do domínio chegam nas specs de funcionalidade.
- **Destino.** O monorepo separa frontend e backend. A geração usa como diretório pai `backend/` na raiz do repositório, resultando em `backend/finance-controller/`. O generator recusa-se a escrever sobre um diretório existente.

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)

## Regras e critérios de aceitação

- [x] O projeto `backend/finance-controller/` existe, foi gerado pela skill `java-spring-scaffold` e usa os parâmetros da seção de contexto técnico local.
- [x] O build usa Maven Wrapper (`mvnw`/`mvnw.cmd`) e compila com Java 25.
- [x] `mvnw clean verify` termina com sucesso, incluindo `ModularityTests` (verificação do Spring Modulith) e os testes do exemplo.
- [x] O contrato `openapi.yaml` é OpenAPI 3.1.0; as interfaces e DTOs são gerados em `target/generated-sources/openapi` e os controllers concretos os implementam, sem edição manual de código gerado.
- [x] A estrutura do módulo de exemplo respeita a direção de dependências do contexto técnico global: o domínio não depende de Spring, HTTP nem dos DTOs gerados.
- [x] A aplicação arranca com o perfil padrão `local`; `GET /actuator/health` responde `UP` e `GET /api/v1/examples/{id}` responde conforme o contrato.
- [x] `Dockerfile` e `compose.yml` existem; a imagem constrói e o contentor responde em `/actuator/health`, ou a verificação não executada fica registada com o motivo.
  > ✅ 2026-10-24 — Critério revisto por instrução explícita do utilizador: a verificação do build da imagem e do health do contentor não será executada aqui; o utilizador fará manualmente. Ficheiros existentes e revistos; verificação Docker **não executada**.
  > ⏳ 2026-10-24 10:46 +01:00 — Parcial: `Dockerfile` e `compose.yml` existem. Falta: build da imagem e health do contentor, não executados porque o daemon do Docker Desktop não estava ativo. Próxima ação: iniciar o Docker Desktop e executar `docker build` e `docker compose up` em `backend/finance-controller`.
- [x] O projeto não contém credenciais nem segredos; `.env.example` e `.gitignore` gerados estão presentes.
- [x] Spring AI e persistência real não estão presentes no projeto.
  > ✅ 2026-10-24 10:46 +01:00 — Dependências do pom: webmvc, actuator, modulith core/test, boot-starter-test e openapi-generator-maven-plugin; nenhuma de Spring AI, JPA ou driver de banco. Revisão do `pom.xml`.

## Tasks

### Descoberta e decisão

- [x] Confirmar que `backend/finance-controller/` não existe e que Node.js 20+, JDK 25 e (para a verificação Docker) Docker estão disponíveis.
  > ✅ 2026-10-24 10:44 +01:00 — Pasta inexistente; Node v24.14.0, JDK 25.0.2 (via `JAVA_HOME`, `java` fora do PATH) e Docker CLI 29.3.1 presentes. Desvio: o daemon do Docker não estava ativo (ver verificação Docker).
- [x] Confirmar com o utilizador os ajustes listados em "Decisões e ajustes", em especial o destino `backend/finance-controller/` e o `artifactId`, antes de gerar.
  > ✅ 2026-10-24 10:44 +01:00 — Ajustes aprovados pelo utilizador no chat ("Aprovado. Execute a spec 01").

### Contrato e dados

- [x] Gravar a configuração JSON da skill num ficheiro de trabalho fora do projeto gerado e executar `scaffold.mjs --config <ficheiro> --output backend`, conforme as instruções da skill.
  > ✅ 2026-10-24 10:45 +01:00 — Projeto gerado em `backend/finance-controller/` (Spring Boot 4.1.1, Modulith 2.1.1). Desvio: a validação interna da skill falhou no build (ver correções na seção Back-end); a pasta de logs temporária foi removida após a correção.
- [x] Rever `src/main/resources/openapi/openapi.yaml` e confirmar a versão 3.1.0 e a geração de interfaces e DTOs.
  > ✅ 2026-10-24 10:46 +01:00 — `openapi: 3.1.0`; interfaces e DTOs gerados em `com.dowglasmaia.financecontroller.generated.api` e `.model`; `validate.mjs` reporta `openapi: PASS`.

### Back-end

- [x] Rever a estrutura gerada (módulo `example` com `api`, `application`, `domain`, `infrastructure`) e confirmar o cumprimento das regras de dependência do contexto técnico global.
  > ✅ 2026-10-24 10:46 +01:00 — Estrutura conforme; `ModularityTests` e verificação `architecture` do `validate.mjs` passam. Desvio (duas correções locais no projeto gerado, a skill não foi alterada): (1) removido `final` de `ExampleController`, pois o Spring criava um proxy CGLIB (falha em `ApplicationTests`); (2) criado `generated/package-info.java` com `@ApplicationModule(type = OPEN)`, porque o Modulith tratava o pacote `generated` como módulo e rejeitava a dependência de `example`. Ambos os defeitos vêm do template Contract First da skill e se repetirão em novas gerações.
- [x] Confirmar a configuração do Actuator em `application.yml` e os endpoints expostos.
  > ✅ 2026-10-24 10:46 +01:00 — Expostos `health`, `info` e `metrics` em `/actuator`; `/actuator/health` verificado pelo `validate.mjs`.
- [x] Rever `Dockerfile`, `compose.yml`, `.env.example` e `.gitignore` gerados.
  > ✅ 2026-10-24 10:46 +01:00 — Os quatro ficheiros existem. `Dockerfile` multi-stage com `eclipse-temurin:25`; `compose.yml` só com o serviço `api` (sem Postgres, conforme o escopo). Revisão de conteúdo do `Dockerfile` e presença dos restantes.

### Validação

- [x] Executar `validate.mjs` da skill sobre o projeto gerado e registar o resultado.
  > ✅ 2026-10-24 10:46 +01:00 — structure, build, tests (4), architecture, startup, health, example e openapi: todos PASS. Verificação executada.
- [x] Executar `mvnw clean verify` no projeto e registar o resultado dos testes, incluindo `ModularityTests`.
  > ✅ 2026-10-24 10:45 +01:00 — `mvnw.cmd -B -q clean verify` com exit 0, JDK 25.0.2; 4 testes, incluindo `ModularityTests` e `ApplicationTests`. Avisos do Mockito sobre agente dinâmico, sem impacto.
- [x] Arrancar a aplicação e verificar `/actuator/health` e `/api/v1/examples/{id}`.
  > ✅ 2026-10-24 10:46 +01:00 — Verificado pelo `validate.mjs` (startup, health e example PASS).
- [x] Construir a imagem Docker e verificar o health do contentor; se Docker estiver indisponível, registar a verificação como não executada.
  > ✅ 2026-10-24 — Dispensada pelo utilizador no chat ("não precisa testar o docker, faço isso manualmente"). Verificação não executada por Claude; fica a cargo do utilizador (`docker build -t finance-controller:scaffold .` e `docker compose up` em `backend/finance-controller`).
  > ⛔ 2026-10-24 10:46 +01:00 — Bloqueio: `docker build` falhou ao ligar à API do Docker (pipe `dockerDesktopLinuxEngine` inexistente); verificação não executada. Para desbloquear: iniciar o Docker Desktop e executar `docker build -t finance-controller:scaffold .` e `docker compose up` em `backend/finance-controller`.
- [x] Documentar no `README.md` gerado, ou em `.spec/shared/`, apenas os comandos de build, execução e teste do backend, se ainda não estiverem cobertos.
  > ✅ 2026-10-24 — Revisto `backend/finance-controller/README.md`: já cobre `mvnw clean verify`, `spring-boot:run`, endpoints de exemplo e health. Nenhuma alteração necessária.

### Atualização por decisão do produto (2026-10-24) — nome e local do projeto (P-28)

Decisão: o backend chama-se **`api-finance-controller`** e o frontend **`app-finance-controller`**; ambos ficam na **raiz do projeto**, ao lado de `.claude` e `.spec`, e não mais em `backend/`. O projeto já gerado (`backend/finance-controller/`) foi concluído com o nome anterior; os registos acima são históricos e não foram alterados. Esta spec é **reaberta** apenas para a renomeação abaixo. Nada foi movido ou executado nesta atualização.

- Novos valores de geração: `projectName` = `api-finance-controller` (nome da pasta e `spring.application.name`); `groupId`, `packageName` (`com.dowglasmaia.financecontroller`), `javaVersion` 25 e demais parâmetros inalterados; `artifactId` permanece `spring-service` salvo nova instrução.
- Diretório pai da geração: a raiz do repositório (o gerador cria `<pai>/<projectName>`).
- Duas formas de cumprir a decisão, a escolher na execução: (A) **mover e renomear** `backend/finance-controller/` para `api-finance-controller/` e ajustar nomes — preserva as duas correções locais já feitas (controller não `final`; módulo `generated` aberto); (B) **regenerar** com a skill e reaplicar essas duas correções.
- Itens a ajustar em qualquer das formas: nome do projeto e descrição no `pom.xml`, `spring.application.name` em `application.yml`, `README.md`, nome da imagem no `Dockerfile`/`compose.yml`, caminhos nos fluxos de CI e nas specs seguintes.

- [x] Mover/renomear (ou regenerar) o projeto para `api-finance-controller/` na raiz do projeto, conforme P-28, e atualizar os nomes listados.
  > ✅ 2026-10-24 13:11 +01:00 — Forma A (mover): `backend/finance-controller/` movido para `api-finance-controller/` na raiz; pasta `backend/` vazia removida. Nomes atualizados: `<name>` no `pom.xml`, `spring.application.name` em `application.yml`, título do `README.md` e `info.title` do `openapi.yaml`. As duas correções locais (controller não `final`; módulo `generated` aberto) foram preservadas. `artifactId` (`spring-service`), `groupId` e `packageName` inalterados. Verificação: busca por `finance-controller` sem ocorrências restantes fora de `target/`. Desvio: o `Dockerfile` e o `compose.yml` não continham o nome antigo, logo não precisaram de alteração.
- [x] Reexecutar `validate.mjs` e `mvnw clean verify` no novo local e registar a evidência.
  > ✅ 2026-10-24 13:11 +01:00 — `mvnw.cmd -B -q clean verify` com exit 0 (JDK 25.0.2 via `JAVA_HOME`); `validate.mjs`: structure, build, tests (4), architecture, startup, health, example e openapi — todos PASS. Limitação mantida: build da imagem Docker e health do contentor continuam **não executados** (dispensados pelo utilizador).

## Resultado esperado

- Um backend Spring Boot 4 / Java 25 que compila, passa nos testes, arranca e responde ao health e ao endpoint de exemplo, pronto para receber os módulos do domínio financeiro.
- Contrato OpenAPI como origem dos endpoints e DTOs, com domínio independente de DTOs gerados.
- Preservados: o frontend, os documentos em `.spec/memory/` e qualquer ficheiro existente fora de `backend/finance-controller/`.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).

### Resumo de conclusão (2026-10-24)

Scaffold gerado em `backend/finance-controller/` com a skill `java-spring-scaffold` (Java 25, Spring Boot 4.1.1, Modulith 2.1.1, Contract First, Actuator, Docker). `validate.mjs` e `mvnw clean verify` passaram (4 testes). Limitações aceites: (1) a verificação da imagem Docker e do health do contentor não foi executada e fica a cargo do utilizador; (2) o template Contract First da skill exigiu duas correções locais (`final` no controller; módulo `generated` aberto), que se repetirão em novas gerações.
