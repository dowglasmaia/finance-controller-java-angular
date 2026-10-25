# 39-infra-ambientes-e-execucao-local

> **IDs:** INFRA-SPEC-00 (arquitetura e ambientes), INFRA-SPEC-01 (ferramentas e execução local), INFRA-SPEC-03 (variáveis e configurações) · **Categoria:** Infraestrutura · **Estado:** proposta para revisão — não aprovada, não executada.

## Objetivo

Definir os ambientes, as ferramentas de desenvolvimento e o modelo de configuração do Finance Controller, substituindo o ambiente local de PWA da origem (portas 5599/8090, scripts PowerShell) por um fluxo reproduzível com backend, frontend e PostgreSQL.

## Escopo

- Inclui: ambientes, versões de ferramentas verificadas, execução local, variáveis e ficheiros de exemplo.
- Não inclui: provisionamento em nuvem e deploy (INFRA-SPEC-08), banco em detalhe (INFRA-SPEC-02), CI (INFRA-SPEC-07).

## Contexto técnico local

### Origem (CONFIRMADO — DESC-INF-01/02/03)

Aplicação Flutter servida como PWA em `localhost:5599` (dados reais) e `8090` (desenvolvimento); scripts `tools/*.ps1`; sem variáveis de ambiente, sem backend, sem CI. Os dados da origem vivem no navegador e **não são migrados** (P-16): o destino começa com banco novo. O Flutter não é alterado.

### Ambientes (PROPOSTO)

| Ambiente | Finalidade | Banco | Observações |
|---|---|---|---|
| `local` | desenvolvimento | PostgreSQL em contentor | perfil `local` (padrão do scaffold); frontend em 4200 com proxy para a API |
| `test` | testes automáticos | PostgreSQL efémero (Testcontainers) | sem dados reais |
| `prod` | uso real | PostgreSQL gerido ou próprio | hospedagem flexível, decidida depois; tudo roda em imagens Docker (P-02, P-18) |

### Ferramentas e versões (verificadas em 2026-10-24 na máquina de desenvolvimento)

| Ferramenta | Versão observada | Observação |
|---|---|---|
| JDK | 25.0.2 em `C:\jdk-25.0.2` (`JAVA_HOME`), `java` fora do `PATH` | o `mvnw` usa `JAVA_HOME` |
| Maven | 3.9.16 via Maven Wrapper | vem do scaffold |
| Node.js | v24.14.0 | atende ao requisito das skills (20.19+/22.12+) |
| Docker | CLI 29.3.1 | daemon pode estar desligado; é preciso para Testcontainers e compose |
| Spring Boot / Modulith | 4.1.1 / 2.1.1 | pinadas pela skill de scaffold |
| Angular | 21 | por `angular-frontend-scaffold` |
| PostgreSQL | **versão estável corrente** (P-17, decidida) | fixar a versão exata na data da implementação, no `compose` e no CI |
| Flyway | versão compatível com o PostgreSQL escolhido | migrações (P-17) |

Sistema operacional de desenvolvimento: Windows 11 com PowerShell; os comandos documentados devem funcionar em Windows (`mvnw.cmd`) e ser indicados também para shell POSIX.

### Execução local (alvo)

Um único comando sobe o banco; backend e frontend sobem em terminais separados ou via orquestração (INFRA-SPEC-06). Documentar em `README` raiz: pré-requisitos, `JAVA_HOME`, portas (backend 8080 por `PORT`, frontend 4200), como aplicar migrações, como importar um backup sintético.

### Variáveis e configuração (INFRA-SPEC-03)

| Variável | Uso | Obrigatória em prod |
|---|---|---|
| `PORT` | porta HTTP do backend (scaffold) | não |
| `SPRING_PROFILES_ACTIVE` | `local`/`test`/`prod` | sim |
| `SPRING_DATASOURCE_URL`, `_USERNAME`, `_PASSWORD` | banco | sim |
| `FC_CORS_ALLOWED_ORIGINS` | origens do frontend | sim |
| `FC_JWT_SECRET` | segredo de assinatura dos tokens | sim |
| `FC_JWT_ACCESS_TTL`, `FC_JWT_REFRESH_TTL` | duração dos tokens | não (padrão) |
| `FC_LOGIN_MAX_ATTEMPTS` | limite de tentativas de entrada | não (padrão) |
| `FC_APP_BASE_URL` | URL pública do frontend usada nos links de e-mail (nunca derivada do pedido) | sim |
| `FC_MAIL_HOST`, `FC_MAIL_PORT`, `FC_MAIL_USERNAME`, `FC_MAIL_PASSWORD`, `FC_MAIL_FROM` | servidor SMTP e remetente | sim em `prod` |
| `FC_PASSWORD_RESET_TTL` | validade do link de recuperação | não (padrão 30 min) |
| `API_BASE_URL` (frontend) | URL da API, injetada no build/entrega | sim |

Regras: nenhum segredo em ficheiros versionados; `.env.example` com nomes e valores fictícios (já existe no scaffold do backend); `.env` real ignorado pelo Git.

### Decisões e riscos

Risco: `java` fora do `PATH` e Docker desligado atrapalharam a spec 01; documentar. Dados reais nunca em `local` ou `test`.

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [Scaffold do backend](../01-scaffold-backend-spring-modulith/spec.md)
- [GLOBAL-SPEC-01](../03-global-arquitetura-e-requisitos-transversais/spec.md)

## Regras e critérios de aceitação

- [ ] Um desenvolvedor novo executa backend, frontend e banco só com o `README` e sem alterar ficheiros versionados.
- [ ] Todas as variáveis da tabela estão documentadas no `.env.example` sem valores reais.
- [ ] Nenhum ficheiro versionado contém credencial, token ou dado financeiro real.
- [ ] A aplicação falha com mensagem clara se uma variável obrigatória de `prod` faltar.
- [ ] A versão de cada ferramenta usada é registada no `README`.

### Skills de implementação

- **Skills obrigatórias:** `java-modern-engineering` (configuração Java); `angular-frontend-scaffold` (configuração do frontend).
- **Caminhos:** `C:\Users\dowgl\.codex\skills\java-modern-engineering\SKILL.md`; `.claude/skills/angular-frontend-scaffold/SKILL.md`.
- **Responsabilidade:** propriedades externalizadas e perfis; porta e proxy do frontend.
- **Momento de utilização:** fase de fundação.
- **Dependências:** P-17 (decidida), P-18 (hospedagem adiada).
- **Limitações:** **não há skill de infraestrutura/ambientes nos diretórios indicados.**

## Tasks

### Descoberta e decisão

- [ ] Fixar a versão estável do PostgreSQL na data da implementação e documentar `JAVA_HOME`/PATH.

### Back-end

- [ ] Externalizar propriedades por perfil e atualizar `.env.example`.

### Front-end

- [ ] Configurar URL da API e proxy de desenvolvimento.

### Validação

- [ ] Reproduzir o arranque completo numa máquina limpa seguindo o `README` e registar o resultado.

## Resultado esperado

- Ambientes e configuração claros, sem segredos, reproduzíveis por qualquer desenvolvedor.
- Preservada a regra de não tocar nos dados reais da origem.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
