# Finance Controller

Aplicação web responsiva de controle financeiro pessoal. Permite registrar receitas e despesas, acompanhar o saldo de cada mês, definir limites de gastos e organizar reservas (cofrinhos), em **reais (BRL) e euros (EUR)**, sem conversão entre moedas.

O projeto é uma reconstrução de um app Flutter existente, feita por **desenvolvimento orientado a especificações (SDD)**: primeiro a spec é escrita e aprovada, depois a implementação. Este repositório é um monorepo com backend, frontend e as especificações.

> **Estado atual:** backend gerado e validado (scaffold com um módulo de exemplo) e frontend gerado (shell Angular com build verde). Nenhuma fatia de negócio foi implementada ainda; a próxima é a F1 (backend base + `identity`). Ver [roadmap](.spec/shared/roadmap.md).

## Funcionalidades previstas

| Funcionalidade | Objetivo |
|---|---|
| Autenticação | Cadastro, entrada, saída e recuperação de senha, com dados isolados por usuário |
| Categorias e subcategorias | Organizar lançamentos, com ícone e cor |
| Lançamentos | Registrar, filtrar, editar e excluir receitas e despesas, por mês de referência |
| Metas de gastos | Limites recorrentes e por categoria, com alerta ao ultrapassar |
| Dashboard | Resumo do mês, progresso das metas e total em reservas |
| Cofrinhos | Reservas com depósitos, retiradas e metas; saldo nunca negativo |
| Relatórios | Receitas, despesas e distribuição de gastos por período |
| Preferências | Moeda e tema claro ou escuro |

Regras completas em [.spec/memory/produto.md](.spec/memory/produto.md).

## Estrutura do repositório

```text
.
├── api-finance-controller/   Backend: Java 25, Spring Boot 4.1, Spring Modulith, Maven Wrapper
├── app-finance-controller/   Frontend: Angular 21, TypeScript estrito, Tailwind CSS (Dockerfile + nginx)
├── scripts/                  Scripts PowerShell para subir backend, frontend ou tudo
├── compose.yml               Orquestração local: db, mailpit, api e web
├── .spec/                    Specs, decisões, templates e roadmap (fonte de verdade do que construir)
├── .docs/                    Diagrama de arquitetura (projeto.excalidraw)
├── .claude/skills/           Skills de IA do projeto (Angular e análise do app Flutter)
├── .agents/skills/           Cópia das mesmas skills para outras ferramentas de IA
├── CLAUDE.md / AGENTS.md     Instruções para assistentes de IA
└── .github/                  Instruções do Copilot (e CI, a partir da F3)
```

## Stack

| Área | Tecnologia |
|---|---|
| Backend | Java 25, Spring Boot 4.1.1, Spring Modulith 2.1.1 (monólito modular), Clean Architecture |
| Banco de dados | PostgreSQL + Flyway + JPA (ainda por entrar, a partir da F1) |
| Frontend | Angular 21+, standalone, zoneless, Tailwind CSS, SCSS |
| Contrato da API | OpenAPI 3.1 por módulo (`openapi/modules/`), com interfaces e DTOs gerados no backend |
| Observabilidade | Logs JSON estruturados, OpenTelemetry nativo do Spring Boot (exportação desligada por padrão) |
| Testes | JUnit e ArchUnit/Modulith; integração com PostgreSQL via Testcontainers (sem H2) |

O diagrama da solução está em [.docs/projeto.excalidraw](.docs/projeto.excalidraw) (abra no [Excalidraw](https://excalidraw.com) ou na extensão do VS Code). Decisões técnicas em [.spec/memory/contexto-tecnico.md](.spec/memory/contexto-tecnico.md).

## Como executar

### Pré-requisitos

- Docker (Docker Desktop no Windows): banco de dados, execução em contentores e testes de integração (Testcontainers).
- JDK 25 com `JAVA_HOME` configurado (só para rodar o backend fora do Docker ou os testes).
- Node.js 20.19+, 22.12+ ou 24 (com npm), só para rodar o frontend fora do Docker.

### Scripts (Windows PowerShell)

Os scripts ficam em [scripts/](scripts/) e criam o `.env` a partir de [.env.example](.env.example) na primeira execução (valores fictícios de desenvolvimento; ajuste ao seu ambiente). O [compose.yml](compose.yml) na raiz define os serviços `db` (PostgreSQL 18.1), `mailpit` (servidor SMTP de captura: os e-mails da recuperação de senha aparecem em `http://localhost:8025`, nada é enviado de verdade), `api` (backend) e `web` (frontend estático com nginx). Com a API fora do contentor (`mvnw spring-boot:run`), suba o `mailpit` (`docker compose up -d mailpit`) para ver os e-mails; sem ele o envio falha em silêncio e o pedido continua respondendo igual.

| Quero | Comando | Resultado |
|---|---|---|
| Só o backend | `.\scripts\up-back.ps1` | Banco + API em Docker: API em `http://localhost:8080` |
| Só o backend, API na minha máquina | `.\scripts\up-back.ps1 -Local` | Banco em Docker; API por `mvnw spring-boot:run` (depuração) |
| Só o frontend (desenvolvimento) | `.\scripts\up-front.ps1` | `npm start` em `http://localhost:4200`, com proxy de `/api` para `localhost:8080` |
| Só o frontend em Docker | `.\scripts\up-front.ps1 -Docker` | Imagem estática em `http://localhost:8081` (a API precisa estar no ar) |
| Tudo junto | `.\scripts\up-all.ps1` | Banco + API + frontend em Docker: aplicação em `http://localhost:8081` |
| Parar | `.\scripts\down.ps1` (`-Volumes` apaga os dados) | Para os contentores; os dados do banco ficam no volume |
| Ver logs | `.\scripts\logs.ps1 [db, api ou web]` | Acompanha os logs |

Use `-NoBuild` nos scripts de Docker para reaproveitar as imagens já construídas. Se mudar `WEB_PORT` para 4200, não rode ao mesmo tempo o servidor de desenvolvimento do frontend.

No frontend em Docker, o nginx serve os arquivos com fallback para `index.html` e encaminha `/api` para a API no mesmo domínio (o cookie de renovação funciona sem CORS). A imagem do frontend aceita `API_UPSTREAM` para apontar para outra API.

### Comandos do Docker Compose (equivalentes aos scripts)

Execute na **raiz** do repositório. Antes da primeira vez, crie o `.env` (os scripts fazem isso sozinhos):

```powershell
Copy-Item .env.example .env
```

Subir:

```bash
# Tudo junto (banco + API + frontend); aplicação em http://localhost:8081
docker compose up -d --build --wait

# Só o backend (banco + API); API em http://localhost:8080
docker compose up -d --build --wait db api

# Só o banco (para rodar a API pelo Maven ou pela IDE)
docker compose up -d --wait db

# Só o frontend em Docker (a API precisa estar no ar)
docker compose up -d --build --no-deps web
```

Acompanhar e conferir:

```bash
docker compose ps                  # estado dos serviços
docker compose logs -f api         # logs de um serviço (db, api ou web)
curl http://localhost:8080/actuator/health
```

Reconstruir e parar:

```bash
docker compose build api           # reconstrói só a imagem da API (ou web)
docker compose up -d --force-recreate api   # recria um serviço
docker compose down                # para e remove os contentores; os dados do banco ficam no volume
docker compose down -v             # também apaga os dados do banco
```

Sem `-d` os logs ficam presos ao terminal (`Ctrl+C` para parar). Sem o `.env` o Compose recusa subir e pede `POSTGRES_PASSWORD` e `APP_JWT_SECRET`.

### Sem scripts

Backend: `cd api-finance-controller` e `.\mvnw.cmd clean verify` (Windows) ou `./mvnw clean verify`; o banco precisa estar no ar (`docker compose up -d db` na raiz). Frontend: `cd app-finance-controller`, `npm install`, `npm start`; build de produção com `npm run build` e testes com `npm test`. Variáveis do backend em [api-finance-controller/.env.example](api-finance-controller/.env.example).

Cobertura: backend em `api-finance-controller/target/site/jacoco` depois do `verify`; frontend com `npm test -- --coverage`. Testes de ponta a ponta e acessibilidade (Playwright + axe) em [e2e/README.md](e2e/README.md). Operação em produção: [.spec/shared/operacao.md](.spec/shared/operacao.md).

## Como o trabalho é conduzido (SDD)

1. Cada entrega tem uma spec em [.spec/specs/](.spec/specs/), com critérios de aceitação e tarefas.
2. O comportamento é especificado e **aprovado antes** de implementar; mudanças de contrato ou plano exigem nova aprovação.
3. A entrega segue **fatias verticais** (F0 a F12): backend do módulo, depois a página, terminando em um ponto de verificação testável. Ordem e dependências no [roadmap](.spec/shared/roadmap.md) e na [spec 47](.spec/specs/47-implementation-plano-e-sequencia/spec.md).
4. Uma tarefa só é marcada como concluída com evidência verificada (build, testes, chamadas reais). Método em [como-executar.md](.spec/shared/como-executar.md).
5. O contrato OpenAPI de cada módulo é revisado e aprovado antes de gerar código.

## Convenções principais

- Todo repositório filtra por `owner_id`, e cada módulo tem teste de isolamento entre usuários.
- Dinheiro sempre em `BigDecimal` (decimal), nunca ponto flutuante.
- Um caso de uso por operação: `<Verbo><Substantivo>UseCase`, `@Service`, método `execute`. Interfaces apenas em portas de saída e fronteiras entre módulos.
- Logs estruturados, sem dados pessoais ou financeiros.
- Sem segredos no repositório: use variáveis de ambiente; `.env` é ignorado pelo Git.

## Skills de IA

O projeto usa skills (instruções reutilizáveis) para assistentes de IA como Claude Code e Codex. Cada uma tem um `README.md` com uso e exemplos.

| Skill | Uso |
|---|---|
| [angular-frontend-scaffold](.claude/skills/angular-frontend-scaffold/README.md) | Criar o projeto Angular (já usada) |
| [angular-module-scaffold](.claude/skills/angular-module-scaffold/README.md) | Implementar um módulo Angular a partir do contrato |
| [angular-architect](.claude/skills/angular-architect/README.md) | Arquitetura, revisão e refatoração Angular |
| [angular-tailwind-ui-specialist](.claude/skills/angular-tailwind-ui-specialist/README.md) | Interface com Tailwind, acessibilidade e responsividade |
| [flutter-analyzer](.claude/skills/flutter-analyzer/README.md) | Levantar regras do app Flutter original |

As skills de backend (`java-spring-scaffold`, `java-spring-create-module`, `openapi-rest-contract`) ficam na pasta de skills do usuário (`~/.codex/skills`, `~/.claude/skills`), fora deste repositório.

## Roadmap

| Fatia | Entrega |
|---|---|
| F0 | Aprovação das specs e primeiro commit |
| F1 | Backend base e módulo `identity` |
| F2 | Frontend base e autenticação |
| F3 | CI mínimo |
| F4–F11 | Categorias, lançamentos, metas de gastos, dashboard, cofrinhos, relatórios, preferências e recuperação de senha |
| F12 | Endurecimento: testes, segurança, E2E e observabilidade (✅ implementada; pendências na [spec 47](.spec/specs/47-implementation-plano-e-sequencia/spec.md)) |

Deploy e hospedagem estão adiados. Backup e importação de dados foram cancelados por decisão de produto.
