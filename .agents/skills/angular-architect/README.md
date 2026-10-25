# angular-architect — como usar com uma IA

Skill de **arquitetura e qualidade Angular 21**: decisões de estrutura, componentes, estado, formulários, integração HTTP, testes, revisão e refatoração. É a skill "consultiva" do frontend: escolhe a menor solução que proteja as fronteiras reais do sistema. As instruções que a IA segue estão em [SKILL.md](SKILL.md) e [references/](references/).

## 1. Para que serve

| Faz | Não faz |
|---|---|
| Revisa código Angular e aponta problemas demonstráveis (arquivo, impacto, correção) | Criar o projeto (use `angular-frontend-scaffold`) |
| Apoia decisões: onde fica o estado, signals vs RxJS, formulários, fronteiras HTTP | Criar ou alterar contratos OpenAPI |
| Implementa/refatora com Angular 21, `strict`, `OnPush`, `inject()`, `@if`/`@for` | Migrar versão ou iniciar scaffold sem escopo autorizado |
| Orienta testes e verificação (lint, testes, build de produção) | Inventar endpoints, regras financeiras ou requisitos offline |
| Lê `.spec/` e respeita specs aprovadas | Alterar o app Flutter de referência |

Para criar um módulo completo a partir do contrato, prefira `angular-module-scaffold`; para o visual, `angular-tailwind-ui-specialist`.

## 2. Pré-requisitos

- Projeto Angular existente (a IA inspeciona `package.json`, `angular.json`, `tsconfig*.json`, rotas e testes). Sem projeto, ela **não** executa scaffold por conta própria.
- Contexto: `.spec/memory/produto.md`, `.spec/memory/contexto-tecnico.md` e a spec relevante, quando houver.

## 3. Dados a informar

| Informação | Obrigatória | Observação |
|---|---|---|
| Objetivo | **Sim** | Revisar, decidir, implementar ou refatorar |
| Alvo | **Sim** | Arquivo, pasta ou funcionalidade |
| Critérios de aceitação | Recomendado | Comportamento esperado |
| Restrições | Opcional | Ex.: "sem novas dependências", "não alterar rotas" |

## 4. Como acionar

### Exemplos de pedido

Revisão:

```text
Use a skill angular-architect para revisar app-finance-controller/src/app/features/categorias.
Aponte só problemas reais (arquivo, impacto, correção). Não altere código.
```

Decisão:

```text
Use a skill angular-architect: onde deve ficar o estado da lista de lançamentos
(componente, feature ou compartilhado)? Compare as opções e recomende uma.
```

Implementação:

```text
Use a skill angular-architect para refatorar o formulário de lançamento para
Reactive Forms tipados, mantendo o comportamento. Rode lint, testes e build.
```

### Claude Code / Codex / outras

Claude Code: `/angular-architect` seguido do pedido. Codex: `$angular-architect`. Sem suporte a skills: "Leia `.agents/skills/angular-architect/SKILL.md` e siga as instruções para: ...".

## 5. Como a IA trabalha

1. Lê instruções locais e specs aprovadas; separa análise, revisão, implementação e migração.
2. Inspeciona o projeto antes de propor mudanças.
3. Escolhe uma fatia pequena: modela entradas, saídas e erros; implementa no limite de responsabilidade certo.
4. Verifica com os scripts existentes (lint, testes, build de produção). `tsc` sozinho não comprova templates.
5. Entrega: comportamento, motivo, evidências e limitações; se uma decisão bloqueia, apresenta a alternativa concreta.

## 6. Diretrizes principais

- Organização por funcionalidade; regras reutilizáveis fora da UI; HTTP e armazenamento nas fronteiras.
- Standalone, `input()`/`output()`, `@if`/`@for`, `inject()` e `ChangeDetectionStrategy.OnPush` em código novo.
- Sem `any`, `$any()`, `!` ou `@ts-ignore` como solução habitual.
- Signals para estado síncrono, RxJS para assíncrono; nunca duas fontes mutáveis do mesmo dado.
- Carregamento, vazio e erro explícitos; não transformar falha HTTP em sucesso vazio.

## 7. Referências sob demanda

| Necessidade | Arquivo |
|---|---|
| APIs, versões, zoneless, estado e formulários | [angular-21.md](references/angular-21.md) |
| Fronteiras, SOLID, tipagem, segurança, testes | [architecture-quality.md](references/architecture-quality.md) |
| Código concreto e tradeoffs | [examples.md](references/examples.md) |

## 8. Erros comuns

| Sintoma | Causa e solução |
|---|---|
| A IA não cria o projeto | Esperado: use `angular-frontend-scaffold` |
| Pede decisão antes de seguir | Há tradeoff duradouro ou requisito ausente: responda ou aprove a alternativa |
| Sugere mudança em massa | Peça escopo mínimo; migrações cosméticas não são o padrão |
