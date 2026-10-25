# 23-frontend-arquitetura-angular

> **ID:** FRONTEND-SPEC-00 · **Categoria:** Frontend · **Estado:** proposta para revisão — não aprovada, não executada.

## Objetivo

Definir a arquitetura e a estrutura do frontend Angular 21 e o passo de criação do projeto com o scaffold determinístico, de modo que as páginas e módulos seguintes tenham uma base única.

## Escopo

- Inclui: criação do projeto via skill de scaffold, estrutura de pastas, padrões de componentes, estado, estilo e fronteiras.
- Não inclui: páginas (FRONTEND-SPEC-09…15), contratos (GLOBAL-SPEC-03), testes (FRONTEND-SPEC-08).

## Contexto técnico local

### Scaffold (regras da skill)

A skill `angular-frontend-scaffold` cria o projeto com valores fixos: Angular CLI 21, TypeScript estrito, componentes standalone com rotas de funcionalidade lazy, SCSS + Tailwind CSS via PostCSS, renderização no cliente (sem SSR), **zoneless**, **sem ficheiros nem comando de teste**, porta de desenvolvimento 4200, apenas `HttpClient` e uma fronteira de erro tipada, e verificação inicial só por build de produção. Exige o **nome do projeto**, que ainda não foi definido; o agente deve perguntar exatamente "Qual é o nome do projeto Angular que devo criar?" antes de gerar (P-17). Nunca sobrescreve um diretório existente. O destino segue o monorepo com frontend e backend separados.

### Estrutura de código (PROPOSTO)

```
src/app/
  core/              shell, layout, interceptors, providers globais (config da API, erros)
  shared/            componentes e utilitários realmente compartilhados (money, formatação, ui/)
  features/<modulo>/ <modulo>.routes.ts · doc.md · pages/ · ui/ · data-access/
```

Módulos: `auth`, `dashboard`, `transactions`, `categories`, `money-boxes`, `budget`, `reports` (nomes de pasta em kebab-case, conforme `angular-module-scaffold`; rotas em português — FRONTEND-SPEC-01). Não há módulo `backup` (P-03).

### Padrões

- Standalone, `ChangeDetectionStrategy.OnPush`, `inject()`, `@if/@for/@switch`, `input()`/`output()`.
- `strict` e `strictTemplates`; sem `any`, `!` nem `@ts-ignore`.
- Signals para estado síncrono e derivados; RxJS para chamadas HTTP, cancelamento e concorrência (troca de mês cancela a consulta anterior).
- Reactive Forms tipados (FRONTEND-SPEC-05). Signal Forms e Resource APIs: tratados como experimentais no 21, não adotados sem decisão.
- Sem biblioteca de estado global, UI kit nem gerador novo sem justificativa (skills). Gráficos: **ApexCharts** via `ng-apexcharts` (preferência do responsável); ícones: **Lucide** (`lucide-angular`) — ADR-013, a confirmar a compatibilidade com Angular 21 antes de instalar. A origem usa `fl_chart` e ícones Material.
- Componentes só apresentam e emitem intenções; HTTP em `data-access/`; regras oficiais no backend (RT-01).
- Cada módulo mantém um `doc.md` atualizado (propósito, jornadas, operações consumidas, estado, validação).

### Configuração

URL da API por ambiente (`environment`/token de injeção), sem segredos; proxy de desenvolvimento para `/api` (a skill não o cria — fica como tarefa desta spec); porta 4200. Entrega como **imagem Docker** estática (P-02, ADR-014); sem PWA nem service worker.

### Decisões e riscos

ADR-010, ADR-013, ADR-014. O scaffold não inclui testes: introduzir o executor de testes (o padrão do Angular 21 no momento) é tarefa do projeto depois do scaffold; a skill proíbe adicionar bibliotecas sem necessidade e proíbe `npm test` durante o scaffold. Risco: perder a paridade funcional por não haver testes de UI na origem (DESC-TST-01).

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [GLOBAL-SPEC-01](../03-global-arquitetura-e-requisitos-transversais/spec.md)

## Regras e critérios de aceitação

- [ ] O projeto foi gerado por `angular-frontend-scaffold` com o nome aprovado e `node validate.mjs` da skill passa (build de produção).
- [ ] `strict` e `strictTemplates` ativos; não há `any` nem `ts-ignore` no código gerado ou novo.
- [ ] Toda funcionalidade vive em `features/<modulo>` com `routes`, `doc.md` e, quando aplicável, `pages`, `ui`, `data-access`.
- [ ] Nenhum componente faz HTTP ou calcula valores financeiros oficiais.
- [ ] A URL da API vem de configuração; `ng serve` usa a porta 4200 e o proxy para `/api`.

### Skills de implementação

- **Skills obrigatórias:** `angular-frontend-scaffold`, `angular-architect`.
- **Caminhos:** `.claude/skills/angular-frontend-scaffold/SKILL.md`; `.claude/skills/angular-architect/SKILL.md` (cópias em `.agents/skills/`).
- **Responsabilidade:** `angular-frontend-scaffold` cria o projeto; `angular-architect` orienta fronteiras, tipagem e padrões.
- **Momento de utilização:** primeiro passo do frontend, antes de qualquer módulo.
- **Dependências:** nome do projeto Angular (P-28, a perguntar ao executar); contrato aprovado para módulos.
- **Limitações:** `angular-frontend-scaffold` não cria funcionalidades além do exemplo inicial nem testes.

## Tasks

### Descoberta e decisão

- [ ] Obter o nome do projeto Angular (P-28) e confirmar a compatibilidade de ApexCharts/`ng-apexcharts` e Lucide com Angular 21.

### Front-end

- [ ] Executar o scaffold com o nome `app-finance-controller`, na raiz do projeto (ao lado de `.claude`) e rodar o validador da skill.
- [ ] Criar `core/` e `shared/` mínimos e a configuração de ambiente e proxy.

### Validação

- [ ] Build de produção sem erros e sem avisos de template estrito; registar o resultado.

## Resultado esperado

- Projeto Angular 21 base, estrito, zoneless, com estrutura por funcionalidade pronta para os módulos.
- Preservadas as restrições do scaffold e a decisão de não introduzir bibliotecas sem necessidade.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
