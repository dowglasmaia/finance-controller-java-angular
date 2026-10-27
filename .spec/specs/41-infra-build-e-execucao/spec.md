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
  > ⏳ 2026-10-27 — Parcial: `./mvnw clean verify` com Docker: 159 testes, 0 falhas, BUILD SUCCESS; `npm run build` sem erros nem avisos e validador da skill de scaffold. Falta: confirmar num ambiente limpo.
- [x] A imagem do backend constrói e `GET /actuator/health` responde `UP` no contentor com o banco.
  > ✅ 2026-10-27 — `docker compose build api` e `up`: `GET http://localhost:8080/actuator/health` → `UP` no contentor, com o PostgreSQL 18.1 do `compose` (`scripts/up-back.ps1` e `up-all.ps1`). Adicionado `api-finance-controller/.dockerignore`.
- [x] O frontend servido estaticamente funciona com navegação direta em qualquer rota (fallback SPA).
  > ✅ 2026-10-27 — Imagem `app-finance-controller/Dockerfile` (build Angular + nginx 1.29): `GET /categorias`, `/entrar` e `/lancamentos` → 200 com `index.html` (fallback SPA) e redirecionamento ao login pela aplicação; cabeçalhos `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`.
- [x] `docker compose up` sobe banco, backend e frontend e a aplicação completa funciona de ponta a ponta.
  > ✅ 2026-10-27 — `.\scripts\up-all.ps1` (compose na raiz) subiu db, api e web; percurso ponta a ponta: cadastro, 409 repetido, login, recarga com cookie `HttpOnly` e rota protegida em `http://localhost:8081`. Desvio: o servidor SMTP de captura (Mailpit) entra com a F11. Correção feita ao testar: o nginx enviava `Host` sem porta e o Spring devolvia 403 "Invalid CORS request"; agora usa `$http_host`.
- [ ] Nenhum artefacto contém segredos.
  > ⏳ 2026-10-27 — Parcial: `.env` é ignorado pelo Git; `.env.example` e o `compose.yml` só têm valores fictícios de desenvolvimento e exigem `POSTGRES_PASSWORD` e `APP_JWT_SECRET`; os `.dockerignore` excluem `.env*`. Falta: inspeção das imagens por segredos.
- [ ] Backend e frontend têm imagens Docker separadas e versionadas; cada uma constrói sem a outra.
  > ⏳ 2026-10-27 — Parcial: imagens separadas, cada uma com Dockerfile e contexto próprios, construídas de forma independente (`finance-controller-api` e `finance-controller-web`). Falta: versionar por tag (hoje `latest`), previsto com o CI (F3).

### Skills de implementação

- **Skills obrigatórias:** nenhuma confirmada para orquestração/empacotamento; `angular-frontend-scaffold` e `java-spring-scaffold` (já usada) geraram os pontos de partida.
- **Caminhos:** `.claude/skills/angular-frontend-scaffold/SKILL.md`; `C:\Users\dowgl\.codex\skills\java-spring-scaffold\SKILL.md`.
- **Responsabilidade:** definem os builds iniciais; a orquestração é trabalho manual do projeto.
- **Momento de utilização:** fundação e hardening.
- **Dependências:** INFRA-SPEC-00/02; scaffolds concluídos.
- **Limitações:** **sem skill de Docker/compose/orquestração**; a skill do Angular proíbe executar Docker ou servidores durante o scaffold.

## Tasks

### Back-end

- [x] Validar a imagem do backend com o banco real.
  > ✅ 2026-10-27 — Imagem do backend executada com o PostgreSQL do `compose`: Flyway aplicou a V1 e `/actuator/health` respondeu `UP`; cadastro e login reais via API.

### Front-end

- [x] Definir a imagem/entrega estática e o fallback SPA.
  > ✅ 2026-10-27 — `app-finance-controller/Dockerfile` (multi-stage, `node:24-alpine` → `nginx:1.29-alpine`) e `nginx/default.conf.template`: fallback para `index.html`, cache longo para arquivos com hash, proxy de `/api` para `API_UPSTREAM` (padrão `http://api:8080`) no mesmo domínio.

### Validação

- [x] Subir o conjunto por `docker compose up` e percorrer o fluxo principal; registar o resultado.
  > ✅ 2026-10-27 — Executado com `scripts/up-all.ps1`: cadastro, 409 repetido, login, recarga com cookie `HttpOnly` e rota protegida em `http://localhost:8081`. Também testados `up-back.ps1` (db+api), `up-back.ps1 -Local` (API pelo Maven), `up-front.ps1` (`npm start` na 4200), `up-front.ps1 -Docker` e `down.ps1`.

## Resultado esperado

- Builds reproduzíveis e execução integrada com um comando.
- Preservada a separação entre frontend e backend no monorepo.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
