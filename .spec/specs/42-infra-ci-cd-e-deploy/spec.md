# 42-infra-ci-cd-e-deploy

> **IDs:** INFRA-SPEC-07 (CI/CD e qualidade automatizada), INFRA-SPEC-08 (deploy) · **Categoria:** Infraestrutura · **Estado:** CI mínimo decidido (P-18); deploy adiado — não executada.

## Objetivo

Preparar uma integração contínua mínima no GitHub Actions, com esteiras separadas para backend e frontend, e deixar a entrega pronta para quando a hospedagem for escolhida.

## Escopo

- Inclui: duas esteiras independentes (backend e frontend), portões de qualidade, imagens Docker construídas no CI, estrutura do deploy sem destino.
- Não inclui: criar o repositório Git (o responsável o faz manualmente — P-23), escolha da hospedagem e execução de deploys (P-18).

## Contexto técnico local

### Decisão do produto (P-18, P-23)

"Hospedagem flexível; depois vejo onde hospedar. Vamos trabalhar com Docker. Deixar já o GitHub Actions com CI mínimo, para o back e para o front, cada um com uma esteira de deploy e build separadas." O repositório é criado manualmente pelo responsável.

### Origem e estado atual

A origem não tem CI/CD (DESC-INF-03). O scaffold do backend gerou `.github/workflows/build.yml` dentro do projeto do backend (Ubuntu, JDK 25 Temurin, `./mvnw clean verify`) — **CONFIRMADO por leitura** (spec 01). Esse ficheiro está numa subpasta (agora em `api-finance-controller/.github/workflows/build.yml`, movido pela spec 01 — P-28); o GitHub só executa fluxos de `.github/workflows` na **raiz do repositório**. O repositório Git que contém o destino hoje tem raiz em `C:/Users/dowgl` (verificado com `git rev-parse --show-toplevel`), por isso o novo repositório do projeto (P-23, manual) deve nascer com os fluxos na raiz.

### Esteiras (PROPOSTO, na raiz do repositório)

| Ficheiro | Disparo | Passos |
|---|---|---|
| `.github/workflows/backend-ci.yml` | alterações em `api-finance-controller/**` e no próprio fluxo | JDK 25; cache Maven; `./mvnw clean verify` (inclui `ModularityTests` e testes com Testcontainers — o executor tem Docker); validação do contrato OpenAPI e geração sem diferenças; publicar relatórios; construir a imagem Docker (sem enviá-la) |
| `.github/workflows/frontend-ci.yml` | alterações em `app-finance-controller/**` e no próprio fluxo | Node pinado; instalação reproduzível por lockfile; build de produção com templates estritos; testes do frontend quando existirem (FRONTEND-SPEC-08); construir a imagem Docker (sem enviá-la) |
| `.github/workflows/backend-deploy.yml` e `frontend-deploy.yml` | manual (`workflow_dispatch`) | **esqueletos desabilitados**: constroem a imagem versionada e param no envio ao registo/ao destino, que ainda não existe; cada esteira tem o seu deploy independente |

Regras: filtros por caminho para que uma mudança no backend não rode a esteira do frontend e vice-versa; versões de JDK, Node e PostgreSQL iguais às do desenvolvimento; nenhum segredo no repositório; permissões mínimas do `GITHUB_TOKEN`; nenhum envio a registo enquanto P-18 não definir o destino.

### Portões de qualidade

Nenhum merge com a esteira correspondente vermelha; relatórios de testes e cobertura publicados como artefactos. Verificações adicionais (dependências, segredos, E2E) entram conforme QUALITY-SPEC-05, depois do mínimo.

### Deploy (INFRA-SPEC-08) — adiado

Decisão de onde e como hospedar: depois. Requisitos independentes da hospedagem, já preparados pelo uso de Docker: imagens versionadas e imutáveis para backend e frontend; migrações Flyway executadas antes de o novo backend receber tráfego; verificação de *readiness* (`/actuator/health`); plano de reversão por imagem anterior; configuração só por variáveis de ambiente (INFRA-SPEC-00).

### Riscos

Sem repositório próprio o CI não roda (P-23, responsabilidade do utilizador). Testes com Testcontainers exigem executor com Docker.

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [QUALITY-SPEC-00](../44-quality-estrategia/spec.md)
- [INFRA-SPEC-04…06](../41-infra-build-e-execucao/spec.md)
- [Pendências](../00-engenharia-reversa/pendencias.md)

## Regras e critérios de aceitação

- [ ] Existem duas esteiras de CI independentes, uma para o backend e outra para o frontend, ambas na raiz do repositório e com filtro por caminho.
- [ ] Uma alteração só em `api-finance-controller/**` não dispara a esteira do frontend, e vice-versa.
- [ ] A esteira do backend roda `mvnw clean verify` e constrói a imagem; a do frontend faz o build de produção e constrói a imagem; ambas ficam verdes.
- [ ] Os esqueletos de deploy existem desabilitados e nenhum deles envia imagem alguma.
- [ ] Nenhum segredo está nos fluxos; o `GITHUB_TOKEN` tem permissões mínimas.
- [ ] Quando a hospedagem for escolhida, o deploy passa a incluir migração antes do tráfego, *readiness* e reversão, com ensaio registado.

### Skills de implementação

- **Skills obrigatórias:** nenhuma confirmada.
- **Caminhos:** —
- **Responsabilidade:** —
- **Momento de utilização:** —
- **Dependências:** repositório criado pelo responsável (P-23); INFRA-SPEC-04/05.
- **Limitações:** **não existe skill de CI/CD ou deploy nos diretórios indicados.** O fluxo mínimo do backend vem do scaffold (`java-spring-scaffold`); estendê-lo e criar o do frontend é trabalho manual desta spec.

## Tasks

### Descoberta e decisão

- [ ] Confirmar que o repositório do projeto existe (P-23) e que `api-finance-controller/` e `app-finance-controller/` estão na raiz do repositório.

### Back-end

- [ ] Mover/adaptar o fluxo do scaffold para a raiz como `backend-ci.yml` e acrescentar a construção da imagem.

### Front-end

- [ ] Criar `frontend-ci.yml` depois de o projeto Angular existir.

### Validação

- [ ] Executar cada esteira num ramo de teste e registar os resultados.

## Resultado esperado

- Build e testes automáticos separados para backend e frontend, e entrega pronta para ser ligada a uma hospedagem futura.
- Preservada a regra de não executar deploy sem decisão explícita.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
