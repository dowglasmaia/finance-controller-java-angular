# 41-infra-build-e-execucao

> **IDs:** INFRA-SPEC-04 (build e execução do backend), INFRA-SPEC-05 (build e execução do frontend), INFRA-SPEC-06 (execução integrada e orquestração) · **Categoria:** Infraestrutura · **Estado:** proposta para revisão — não aprovada, não executada.

## Objetivo

Especificar como construir, empacotar e executar o backend, o frontend e o conjunto integrado, substituindo `build-pwa.ps1`/`reconstruir.ps1` da origem por builds reproduzíveis.

## Escopo

- Inclui: comandos de build, artefactos, imagens, orquestração local de todos os serviços.
- Não inclui: CI (INFRA-SPEC-07), deploy (INFRA-SPEC-08), configuração do banco (INFRA-SPEC-02).

## Contexto técnico local

### Origem (DESC-INF-02)

`build-pwa.ps1` faz limpeza, dependências, geração de adapters, análise, testes e build web; `serve-build.ps1` serve `build/web` com fallback SPA; `reconstruir.ps1` publica em `app_desktop/web` e cria atalho. Nada disso é reproduzido (PWA/atalho desktop são específicos da origem).

### Backend (INFRA-SPEC-04) — CONFIRMADO no scaffold

- Build: `mvnw clean verify` (compila, gera interfaces OpenAPI, roda testes e `ModularityTests`).
- Artefacto: JAR executável; execução `mvnw spring-boot:run` ou `java -jar`.
- Imagem: `Dockerfile` multi-stage já gerado (`eclipse-temurin:25` JDK para build, JRE para execução; expõe 8080; `PORT` configurável).
- A verificação da imagem Docker ficou **não executada** na spec 01 por escolha do utilizador; deve ser feita na implementação.

### Frontend (INFRA-SPEC-05) — PROPOSTO

- Build: `ng build` de produção (o validador da skill roda só isto); verificação de templates por compilação.
- Artefacto: ficheiros estáticos servidos por servidor HTTP com fallback para `index.html` (SPA), empacotados numa **imagem Docker** própria e independente da do backend (P-02, ADR-014); a escolha do servidor estático dentro da imagem é técnica e não depende da hospedagem.
- Desenvolvimento: `npm start` na porta 4200 com proxy para a API.
- URL da API injetada na entrega (INFRA-SPEC-00).

### Execução integrada (INFRA-SPEC-06) — PROPOSTO

`compose.yml` na raiz do monorepo com `postgres`, um servidor SMTP de captura (por exemplo, Mailpit, apenas em desenvolvimento, para ver os e-mails de recuperação de senha), `backend` (build da imagem, espera pelo banco saudável) e `frontend` (estático). Perfil `dev` expõe portas locais; perfil de teste de integração para E2E (QUALITY-SPEC-04). Volume nomeado para o banco; segredos por `.env`. O `compose.yml` do scaffold (só `api`) é substituído/estendido sem remover o original antes de o novo estar validado.

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [Scaffold do backend](../01-scaffold-backend-spring-modulith/spec.md)
- [INFRA-SPEC-00](../39-infra-ambientes-e-execucao-local/spec.md)

## Regras e critérios de aceitação

- [ ] `mvnw clean verify` e o build de produção do frontend terminam sem erros num ambiente limpo.
- [ ] A imagem do backend constrói e `GET /actuator/health` responde `UP` no contentor com o banco.
- [ ] O frontend servido estaticamente funciona com navegação direta em qualquer rota (fallback SPA).
- [ ] `docker compose up` sobe banco, backend e frontend e a aplicação completa funciona de ponta a ponta.
- [ ] Nenhum artefacto contém segredos.
- [ ] Backend e frontend têm imagens Docker separadas e versionadas; cada uma constrói sem a outra.

### Skills de implementação

- **Skills obrigatórias:** nenhuma confirmada para orquestração/empacotamento; `angular-frontend-scaffold` e `java-spring-scaffold` (já usada) geraram os pontos de partida.
- **Caminhos:** `.claude/skills/angular-frontend-scaffold/SKILL.md`; `C:\Users\dowgl\.codex\skills\java-spring-scaffold\SKILL.md`.
- **Responsabilidade:** definem os builds iniciais; a orquestração é trabalho manual do projeto.
- **Momento de utilização:** fundação e hardening.
- **Dependências:** INFRA-SPEC-00/02; scaffolds concluídos.
- **Limitações:** **sem skill de Docker/compose/orquestração**; a skill do Angular proíbe executar Docker ou servidores durante o scaffold.

## Tasks

### Back-end

- [ ] Validar a imagem do backend com o banco real.

### Front-end

- [ ] Definir a imagem/entrega estática e o fallback SPA.

### Validação

- [ ] Subir o conjunto por `docker compose up` e percorrer o fluxo principal; registar o resultado.

## Resultado esperado

- Builds reproduzíveis e execução integrada com um comando.
- Preservada a separação entre frontend e backend no monorepo.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
