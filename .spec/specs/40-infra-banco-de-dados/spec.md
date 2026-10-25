# 40-infra-banco-de-dados

> **IDs:** INFRA-SPEC-02 (banco de dados e persistência), INFRA-SPEC-11 (backup e recuperação — **fora do escopo**, P-27) · **Categoria:** Infraestrutura · **Estado:** proposta para revisão — não aprovada, não executada.

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

- [ ] O banco `local` sobe por um comando e persiste dados entre reinícios.
- [ ] Os testes de integração usam PostgreSQL real em contentor efémero.
- [ ] Em `prod` o utilizador da aplicação não tem privilégios de DDL.
- (Fora do escopo — P-27) Rotina de backup e teste de restauração do banco não são exigidos.
- [ ] O banco não aceita ligações da internet pública.

### Skills de implementação

- **Skills obrigatórias:** nenhuma confirmada.
- **Caminhos:** —
- **Responsabilidade:** —
- **Momento de utilização:** —
- **Dependências:** P-17 (decidida); P-18 (hospedagem adiada); BACKEND-SPEC-02.
- **Limitações:** **não existe skill de banco de dados/PostgreSQL/backup nos diretórios indicados.** Não simular.

## Tasks

### Descoberta e decisão

- [ ] Fixar a versão estável do PostgreSQL na data da implementação.

### Contrato e dados

- [ ] Definir utilizadores, permissões e parâmetros do banco por ambiente.

### Validação

- [ ] Subir o banco local e rodar as migrações do zero; registar o resultado.

## Resultado esperado

- Banco reproduzível localmente e em testes, com recuperação comprovada em produção.
- Dados no servidor, a salvo da limpeza do navegador; sem cópia de segurança por decisão do produto (P-27).

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
