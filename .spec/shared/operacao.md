# Guia de operação

> **Estado:** escrito e parcialmente ensaiado na F12 (2026-10-29). Cobre INFRA-SPEC-09 (segredos e permissões), INFRA-SPEC-10 (logs, métricas e saúde) e INFRA-SPEC-12 (operação e manutenção). Hospedagem, backup do banco e provedor de e-mail de produção estão **fora do escopo** (P-18, P-27); onde dependem deles, o guia diz o que fazer quando existirem.

## Peças e portas

| Peça | O que é | Exposição |
|---|---|---|
| `db` | PostgreSQL 18.1 | **nunca** à internet; só o backend a alcança (no `compose` fica em `127.0.0.1`) |
| `api` | Spring Boot (perfil `prod`) | só atrás do proxy HTTPS; `/actuator/health` pública, o resto de `/actuator` exige autenticação |
| `web` | nginx com o Angular estático e proxy de `/api` | HTTPS na borda; sem acesso ao banco |
| `mailpit` | captura de e-mail **só em desenvolvimento** | nunca em produção |

## Segredos e variáveis (perfil `prod`)

Nada daqui entra em ficheiro versionado, imagem ou log. A API **não sobe** se faltar uma obrigatória (teste `ProductionProfileIntegrationTest`).

| Variável | Para quê | Observação |
|---|---|---|
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | utilizador da **aplicação** (`fc_app`, só DML) | obrigatórias |
| `FLYWAY_USER` / `FLYWAY_PASSWORD` (`FLYWAY_URL` opcional) | utilizador de **migração** (dono do banco, DDL) | obrigatórias, diferentes das de cima |
| `APP_JWT_SECRET` | assinatura dos access tokens (≥ 32 bytes) | obrigatória; ver rotação abaixo |
| `APP_CORS_ALLOWED_ORIGINS` | origens do frontend, sem curinga | vazio nega tudo (use vazio com o proxy no mesmo domínio) |
| `FC_APP_BASE_URL` | endereço do frontend no link do e-mail | **HTTPS obrigatório**; vem só da configuração, nunca do pedido |
| `FC_MAIL_HOST` / `FC_MAIL_PORT` / `FC_MAIL_USERNAME` / `FC_MAIL_PASSWORD` / `FC_MAIL_FROM` | SMTP do provedor | obrigatórias (host, porta, remetente) |
| `FC_PASSWORD_RESET_TTL` | validade do link de recuperação | padrão 30 minutos |
| `LOG_LEVEL_ROOT`, `LOG_LEVEL_APP`, `LOG_FORMAT` | logs | padrão INFO, JSON (ECS) |
| `OTEL_EXPORT_ENABLED` e `OTEL_EXPORTER_OTLP_*` | métricas e traços para um coletor | desligado por padrão |

### Utilizadores do banco (uma vez, antes da primeira migração)

1. Crie o banco e o utilizador **dono** (o de migração).
2. Como dono, rode `scripts/db/prod-roles.sql` trocando `__APP_PASSWORD__` por um segredo do cofre. Ele cria `fc_app` com `CONNECT`, `USAGE` no esquema e **só** `SELECT/INSERT/UPDATE/DELETE` (inclusive nas tabelas que as próximas migrações criarem) e retira `CREATE` do `public`.
3. A API conecta como `fc_app`; o Flyway, como o dono. Verificado: `fc_app` não consegue `CREATE`, `DROP`, `ALTER` nem `TRUNCATE`.

## Saúde, logs e métricas

- **Vivo:** `GET /actuator/health/liveness`. **Pronto:** `GET /actuator/health/readiness` (inclui o banco): sem banco devolve **503** e o proxy deve tirar a API de produção. O servidor de e-mail **não** entra na saúde (o envio tolera falhas).
- **Logs:** uma linha JSON por pedido (método, caminho, status, duração, `requestId`, `traceId`). Nunca há senha, token, e-mail completo, corpo ou valor financeiro nos logs (teste `SecurityHardeningIntegrationTest.logsNever…`). Para investigar um pedido, use o cabeçalho `X-Request-Id` da resposta.
- **Métricas:** `/actuator/metrics` (autenticada) e, se `OTEL_EXPORT_ENABLED=true`, o coletor OpenTelemetry.
- **Alertas (verificação periódica enquanto não houver ferramenta, P-18):** a cada 5 minutos, um verificador externo chama `/actuator/health/readiness` e avisa se não for 200; diariamente, conferir a taxa de respostas 5xx nos logs e o espaço em disco do banco. *Falha de backup não se aplica: não há backup (P-27).*

## Procedimentos

### Reiniciar
`docker compose restart api` (ou `web`). O banco só se reinicia com a API parada. A API sobe e valida o esquema (`ddl-auto=validate`); em falta, não sobe.

### Aplicar migração
Migrações Flyway são **imutáveis** depois de aplicadas: nunca edite uma `V<n>__*.sql` já publicada, crie a `V<n+1>`. Ao subir a nova versão da API, o Flyway aplica o que falta como o utilizador de migração; `fc_app` já recebe DML nas tabelas novas pelos privilégios padrão. Antes de produção, rode a migração do zero (`IdentityMigrationTests`) e num banco com dados de teste.

### Publicar e reverter uma versão
- Imagens têm **tag versionada** (`FC_IMAGE_TAG`: versão ou SHA do commit), nunca `latest`.
- Publicar: `FC_IMAGE_TAG=<nova> docker compose up -d --build api web`.
- Reverter **sem mudança de esquema**: `FC_IMAGE_TAG=<anterior> docker compose up -d api web`.
- Reverter **com migração já aplicada**: não existe "desmigrar". Escolha entre (a) corrigir adiante com uma nova migração, ou (b) restaurar o banco do último backup — que **não existe** enquanto P-27 estiver fora do escopo. Por isso, migrações destrutivas (remover coluna/tabela) só com a versão anterior da API já compatível com o esquema novo (duas etapas).

### Rotacionar o segredo de assinatura (`APP_JWT_SECRET`)
1. Gere um novo segredo (`openssl rand -base64 48`) no cofre e atualize a variável.
2. Reinicie a API. **Efeito:** todos os access tokens em circulação (15 minutos) deixam de valer (401); como os refresh tokens são opacos e ficam no banco, o frontend renova sozinho e o utilizador não percebe, a menos que a renovação também falhe. Ensaiado: `rotatingTheSigningSecretInvalidatesTokensSignedWithTheOldOne`.
3. Para forçar a saída de todos (suspeita de vazamento), revogue também os refresh tokens: `UPDATE refresh_token SET revoked_at = now() WHERE revoked_at IS NULL;`.

### Rotacionar a senha do banco
Troque a senha de `fc_app` (`ALTER ROLE fc_app PASSWORD '…'`), atualize `SPRING_DATASOURCE_PASSWORD` e reinicie a API. O mesmo para o dono/Flyway. Os pools antigos caem sozinhos.

### Recuperação de senha em produção
Configure o SMTP do provedor escolhido e o domínio remetente com **SPF e DKIM** (entregabilidade, P-18); monitore a taxa de falha: o log traz `mail not sent ref=<id> cause=<tipo>` (sem destinatário). Tokens vencidos são removidos todos os dias às 03:30.

## Rotina mensal (INFRA-SPEC-12)

1. Atualizar dependências e imagens base; rodar `./mvnw clean verify`, `npm test` e `npm run build`.
2. Rodar as varreduras (também rodam no CI, `security-and-contract.yml`, e toda segunda-feira):
   - Segredos: `docker run --rm -v "$PWD:/repo" ghcr.io/gitleaks/gitleaks:latest git /repo --config /repo/.gitleaks.toml --redact`
   - Dependências: `docker run --rm -v "$PWD:/src" ghcr.io/google/osv-scanner:latest scan source -r /src/app-finance-controller/package-lock.json /src/api-finance-controller/pom.xml` e `npm audit`
   - Imagens: `docker run --rm -v /var/run/docker.sock:/var/run/docker.sock aquasec/trivy:latest image --scanners vuln,secret --ignore-unfixed finance-controller-api:<tag>`
3. Rever os logs de erro (5xx) e a taxa de falha de e-mail.
4. Conferir o espaço do banco e o último backup restaurável (quando existir).
5. Registrar cada mudança de esquema ou configuração com a referência à spec.

## O que foi ensaiado na F12 e o que não foi

| Procedimento | Ensaio | Resultado |
|---|---|---|
| Subir em `prod` só com as variáveis, e recusar sem elas | `ProductionProfileIntegrationTest` (PostgreSQL real) | ✅ |
| Utilizador da aplicação sem DDL; Flyway como dono | idem, com `prod-roles.sql` | ✅ |
| Saúde `DOWN` (503) sem banco | idem (o banco é apagado com a API de pé) | ✅ |
| Migração do zero até a versão 7 | `IdentityMigrationTests` | ✅ |
| Rotação do segredo de assinatura | `SecurityHardeningIntegrationTest` | ✅ (token do segredo antigo → 401) |
| Imagem `web`: fallback da SPA e cabeçalhos | contentor real | ✅ |
| Varreduras de segredos, dependências e imagens | gitleaks, OSV-Scanner, npm audit, Trivy | ✅ (achados corrigidos, ver spec 47) |
| Reversão com restauração do banco | — | ⛔ não ensaiável: não há backup (P-27) |
| Alerta externo de indisponibilidade | — | ⛔ depende da hospedagem (P-18); só o procedimento está descrito |
| Rotação da senha do banco numa instância real | — | ⏳ procedimento descrito, não ensaiado |
