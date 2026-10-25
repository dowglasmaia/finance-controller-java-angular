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
├── app-finance-controller/   Frontend: Angular 21, TypeScript estrito, Tailwind CSS
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

- JDK 25 com `JAVA_HOME` configurado.
- Node.js 20.19+ ou 22.12+ (com npm).
- Docker, apenas para testes de integração com Testcontainers (a partir da F1).

### Backend

```bash
cd api-finance-controller
./mvnw clean verify
./mvnw spring-boot:run
```

No Windows, use `.\mvnw.cmd` no lugar de `./mvnw`. A API sobe em `http://localhost:8080`; teste `GET /actuator/health` e `GET /api/v1/examples/1`. Variáveis de ambiente em [api-finance-controller/.env.example](api-finance-controller/.env.example). Detalhes em [api-finance-controller/README.md](api-finance-controller/README.md).

### Frontend

```bash
cd app-finance-controller
npm install
npm start
```

A aplicação abre em `http://localhost:4200`. Para gerar o build de produção: `npm run build`.

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
| F4–F7 | Categorias, lançamentos, metas de gastos e dashboard |
| F8–F9 | Cofrinhos e relatórios |
| F10–F11 | Preferências e recuperação de senha |
| F12 | Endurecimento: testes, segurança, E2E e observabilidade |

Deploy e hospedagem estão adiados. Backup e importação de dados foram cancelados por decisão de produto.
