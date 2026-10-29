# 40-infra-banco-de-dados

> **IDs:** INFRA-SPEC-02 (banco de dados e persistência), INFRA-SPEC-11 (backup e recuperação — **fora do escopo**, P-27) · **Categoria:** Infraestrutura · **Estado:** executada ao longo das fatias (migrações V1–V7) e fechada na F12 (2026-10-29); isolamento de rede do banco em produção pendente (P-18).

## Objetivo

Especificar a execução do PostgreSQL por ambiente e a política de backup e recuperação do banco, que passa a substituir o armazenamento local do navegador e o backup em pasta da origem.

## Escopo

- Inclui: provisionamento local e de testes, utilizadores e permissões, parâmetros essenciais; registo do backup do banco como tema adiado.
- Não inclui: esquema e migrações (BACKEND-SPEC-02), qualquer backup de aplicação (cancelado — P-03), escolha de provedor em nuvem (P-18).

## Contexto técnico local

### Origem

Persistência em IndexedDB por origem (DESC-DAT-01); risco de perda se o navegador limpar dados — mitigado na origem com `navigator.storage.persist()` e backup em pasta (DESC-BKP-06/07). No destino o banco no servidor elimina esse risco, mas introduz a necessidade de backup do servidor.

### Execução por ambiente (PROPOSTO)

| Ambiente | Forma |
|---|---|
| `local` | serviço PostgreSQL no `compose.yml` raiz, com volume nomeado e credenciais de desenvolvimento via `.env` |
| `test` | contentor efémero por Testcontainers (Docker necessário); nunca H2 |
| `prod` | PostgreSQL gerido ou próprio, não exposto à internet; TLS na conexão |

Versão do PostgreSQL: a estável corrente na data da implementação (P-17); fixar a versão exata no `compose` e no CI. Migrações com Flyway.

### Utilizadores e permissões

- Utilizador de migração (DDL) separado do utilizador da aplicação (DML) em `prod`; mínimo privilégio.
- Senhas apenas por variável/segredo (INFRA-SPEC-09).
- Parâmetros: fuso `UTC`, codificação UTF-8, collation fixa para ordenação sem distinguir caixa (BACKEND-SPEC-10).

### Backup e recuperação (INFRA-SPEC-11) — fora do escopo

O produto decidiu **não ter funcionalidade de backup** (P-03): nem exportação, nem backup automático em pasta (DESC-BKP-06). Decisão P-27: a cópia de segurança do PostgreSQL está **fora do escopo** — o responsável não tem isso em mente por ora. Nenhuma tarefa desta spec a cobre. Fica apenas a recomendação, caso o tema seja retomado (depende também da hospedagem, P-18):

| Item | Recomendação para quando for tratado |
|---|---|
| Método | `pg_dump` lógico agendado ou backup do provedor; retenção a definir |
| Local | fora do servidor do banco, com acesso restrito |
| Teste | restauração periódica para um banco de teste |
| RPO/RTO | a definir pelo produto |

Enquanto isso, não há perda de dados por limpeza do navegador (os dados estão no servidor), mas também **não há cópia de segurança** — risco aceito até a hospedagem ser decidida.

### Riscos

Sem política de backup o servidor torna-se ponto único de falha para dados financeiros pessoais. Testes de restauração são obrigatórios, não opcionais.

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [BACKEND-SPEC-02](../08-backend-persistencia-e-migracoes/spec.md)
- [INFRA-SPEC-00](../39-infra-ambientes-e-execucao-local/spec.md)

## Regras e critérios de aceitação

- [x] O banco `local` sobe por um comando e persiste dados entre reinícios.
  > ✅ 2026-10-27 — O utilizador criado numa execução continuou a existir depois de `down.ps1` (contentores removidos, volume `db-data` mantido) e novo `up`: o cadastro repetido devolveu 409. O `compose.yml` agora está na raiz e o volume pertence ao projeto `finance-controller` (o volume do compose anterior, `api-finance-controller_db-data`, não é reaproveitado).
  > ⏳ 2026-10-27 — Parcial: `docker compose up -d db` sobe o PostgreSQL 18.1 com *healthcheck* e volume; o utilizador criado numa execução continuou a existir após reiniciar a API (volume do `compose`). Falta: confirmar a persistência após reiniciar o contentor do banco.
- [x] Os testes de integração usam PostgreSQL real em contentor efémero.
  > ✅ 2026-10-27 — Testcontainers com `postgres:18.1` (`PostgresContainerConfiguration`); migração, persistência e fluxo executados. Verificação: `./mvnw clean verify` com Docker: 159 testes, 0 falhas, BUILD SUCCESS.
- [x] Em `prod` o utilizador da aplicação não tem privilégios de DDL.
  > ✅ 2026-10-29 16:20 +00:00 — `scripts/db/prod-roles.sql` (papel `fc_app`: só `CONNECT`, `USAGE` e `SELECT/INSERT/UPDATE/DELETE`, também nas tabelas futuras; `CREATE` retirado do `public`) e perfil `prod` com `spring.flyway.user` separado. `ProductionProfileIntegrationTest.runsAsADmlOnlyUserWhileFlywayMigratesAsTheOwner…` (PostgreSQL real): a API sobe como `fc_app`, o Flyway migra como o dono, e `fc_app` recebe erro em `CREATE TABLE`, `DROP TABLE`, `ALTER TABLE` e `TRUNCATE` mas cadastra e lê normalmente.
- (Fora do escopo — P-27) Rotina de backup e teste de restauração do banco não são exigidos.
- [ ] O banco não aceita ligações da internet pública.
  > ⏳ 2026-10-29 16:20 +00:00 — No `compose` a porta do banco só é publicada em `127.0.0.1` e o `guia de operação` (`.spec/shared/operacao.md`) exige o mesmo em produção (acessível só ao backend). A verificação em um ambiente de produção real depende da hospedagem (P-18) e não foi feita.

### Skills de implementação

- **Skills obrigatórias:** nenhuma confirmada.
- **Caminhos:** —
- **Responsabilidade:** —
- **Momento de utilização:** —
- **Dependências:** P-17 (decidida); P-18 (hospedagem adiada); BACKEND-SPEC-02.
- **Limitações:** **não existe skill de banco de dados/PostgreSQL/backup nos diretórios indicados.** Não simular.

## Tasks

### Descoberta e decisão

- [x] Fixar a versão estável do PostgreSQL na data da implementação.
  > ✅ 2026-10-27 — PostgreSQL 18.1 em `compose.yml` e no Testcontainers (2026-10-27).

### Contrato e dados

- [x] Definir utilizadores, permissões e parâmetros do banco por ambiente.
  > ✅ 2026-10-29 16:20 +00:00 — Local: `compose` com volume e senha do `.env`; testes: contentor efémero (Testcontainers, `max_connections=400` para a suíte); prod: dois utilizadores (migração e aplicação) por `prod-roles.sql` e variáveis `FLYWAY_*`/`SPRING_DATASOURCE_*` obrigatórias; fuso UTC e UTF-8 no `compose`. Descrito em `.spec/shared/operacao.md`.

### Validação

- [x] Subir o banco local e rodar as migrações do zero; registar o resultado.
  > ✅ 2026-10-27 — Banco novo pelo `compose` e API em `local`: Flyway migrou o esquema `public` para a versão 1 (`identity`) e a aplicação arrancou. Verificação: log do arranque e uso do cadastro/login reais.

## Resultado esperado

- Banco reproduzível localmente e em testes, com recuperação comprovada em produção.
- Dados no servidor, a salvo da limpeza do navegador; sem cópia de segurança por decisão do produto (P-27).

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
