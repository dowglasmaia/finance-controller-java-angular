# 22-backend-modulo-preferencias

> **ID:** BACKEND-SPEC-17 · **Categoria:** Backend (módulo `preferences`) · **Estado:** executada na F10 (2026-10-29), com a decisão P-19 (servidor, por utilizador).

## Objetivo

Guardar no servidor, por utilizador, a moeda selecionada e o tema, para que o acompanhem entre dispositivos.

## Escopo

- Inclui: `getPreferences`, `savePreferences`, padrões e validação.
- Não inclui: mês de referência e período do relatório (não persistidos na origem — DESC-MES-01, DESC-REP-08).

## Contexto técnico local

### Origem e decisão

Na origem a moeda (`selected_currency`, padrão BRL) e o tema (`theme_mode`, padrão `system`) viviam no navegador (DESC-CUR-03, DESC-PRF-01). Como há login (P-01), a escolha da análise (P-19, "escolha a melhor opção") é guardá-las no servidor por utilizador (ADR-008); antes de entrar, a interface usa o tema escuro (padrão desde 2026-10-27) ou a escolha guardada no navegador.

### Contrato

`GET /preferences` → `{currency: BRL|EUR, theme: LIGHT|DARK|SYSTEM}` (padrões `BRL`, `DARK` se nunca gravado; decisão de 2026-10-27); `PUT /preferences` com os dois campos; valor desconhecido → 400 `VALIDATION_FAILED`. Tabela `preference` com `owner_id` único (BACKEND-SPEC-02).

### Comportamento relacionado

Alternar o tema segue DESC-PRF-01 (dark→light, qualquer outro→dark). A troca de moeda grava a preferência. A mudança de moeda causada por salvar um lançamento noutra moeda (P-15) também grava.

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [FRONTEND-SPEC-04](../27-frontend-gerenciamento-de-estado/spec.md)

## Regras e critérios de aceitação

- [x] Um utilizador novo recebe `BRL` e `DARK`.
  > ✅ 2026-10-29 15:10 +00:00 — `PreferencesFlowIntegrationTest.aNewUserGetsBrlAndDarkWithoutAnythingBeingWritten` (PostgreSQL: GET devolve `BRL` e `DARK` e não cria linha em `preference`).
- [x] Gravar `EUR` e `DARK` e voltar a ler devolve os mesmos valores, também noutro dispositivo.
  > ✅ 2026-10-29 15:10 +00:00 — `savedValuesComeBackTheSameOnAnotherRead` (EUR/LIGHT e depois BRL/SYSTEM voltam iguais; duas gravações, uma única linha). Noutro dispositivo: no navegador, com o `localStorage` apagado, a sessão restaurada aplicou EUR e tema claro vindos do servidor.
- [x] O utilizador A nunca lê nem altera as preferências do B.
  > ✅ 2026-10-29 15:10 +00:00 — `oneUserNeverReadsOrChangesAnothers` (B recebe os padrões enquanto A tem EUR/LIGHT; gravar em B não muda A) e `requiresAuthentication` (401 sem token em GET e PUT).
- [x] Moeda ou tema desconhecidos são rejeitados.
  > ✅ 2026-10-29 15:10 +00:00 — `unknownOrMissingValuesAreRejected`: `USD`, `NEON`, campo ausente, `{}` e corpo que não é JSON → 400 `VALIDATION_FAILED`, e nada muda. `simultaneousSavesNeverCollideOnTheUniqueKey`: 8 PUT simultâneos → todos 200 (`INSERT … ON CONFLICT`).

### Skills de implementação

- **Skills obrigatórias:** `java-modern-engineering`, `java-spring-create-module`, `openapi-rest-contract`, `java-persistence-performance`.
- **Caminhos:** `C:\Users\dowgl\.codex\skills\java-modern-engineering\SKILL.md`; `C:\Users\dowgl\.codex\skills\java-spring-create-module\SKILL.md`; `C:\Users\dowgl\.codex\skills\openapi-rest-contract\SKILL.md`; `.claude/skills/java-persistence-performance/SKILL.md`.
- **Responsabilidade:** contrato `preferences` e implementação.
- **Momento de utilização:** depois de `identity`.
- **Dependências:** BACKEND-SPEC-05, 02.
- **Limitações:** nenhuma específica.

## Tasks

### Contrato e dados

- [x] Prévia aprovada e contrato `preferences`; migração `preference`.
  > ✅ 2026-10-29 15:10 +00:00 — Prévia aprovada pelo utilizador em conversa (2 operações, `GET`/`PUT /api/v1/preferences`, padrões `BRL`/`DARK` sem gravar na leitura, upsert, limpeza do dispositivo no logout) e gerada em `openapi/modules/preferences.yaml` + execução `generate-preferences-api`; `V6__preference.sql` (`owner_id` único, FK em cascata, CHECKs de moeda e tema). Desvio: YAML escrito à mão.

### Back-end

- [x] Implementar domínio, caso de uso, adaptador e controller.
  > ✅ 2026-10-29 15:10 +00:00 — Módulo `preferences` (domínio `Preference`/`ThemeMode`, `GetPreferencesUseCase`, `SavePreferencesUseCase`, porta `PreferenceRepository`, adaptador JPA com upsert nativo, `PreferencesController`). `ModularityTests` verde. `./mvnw clean verify` com Docker: 365 testes, 0 falhas (inclui a F11).

### Validação

- [x] Testar padrões, persistência e isolamento.
  > ✅ 2026-10-29 15:10 +00:00 — 6 testes em `PreferencesFlowIntegrationTest` (padrões sem gravar, persistência, isolamento, validação, corrida, autenticação).

## Resultado esperado

- Preferências persistentes e privadas por utilizador.
- Preservados os padrões e a persistência de moeda e tema da origem.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
