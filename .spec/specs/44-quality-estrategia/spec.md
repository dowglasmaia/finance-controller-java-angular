# 44-quality-estrategia

> **ID:** QUALITY-SPEC-00 · **Categoria:** Qualidade · **Estado:** metas aprovadas e medidas na F12 (2026-10-29); rastreio RN → teste e medição no CI pendentes.

## Objetivo

Definir a estratégia de qualidade da reconstrução: níveis de teste, rastreabilidade entre regra, critério e teste, dados de teste, portões e critérios de conclusão.

## Escopo

- Inclui: pirâmide de testes, metas, dados, evidências, portões e tratamento de defeitos.
- Não inclui: detalhe de cada nível (QUALITY-SPEC-01…05) nem testes por módulo (BACKEND-SPEC-09, FRONTEND-SPEC-08).

## Contexto técnico local

### Ponto de partida (DESC-TST-01)

Sete testes de lógica pura na origem; sem testes de repositórios, backup, providers, widgets ou ponta a ponta; execução não verificada. A paridade de comportamento, portanto, não pode ser garantida só pelos testes existentes: precisa dos cenários desta estratégia.

### Níveis (PROPOSTO)

| Nível | Foco | Onde |
|---|---|---|
| Unitário | domínio, validadores, utilitários, estado | QUALITY-SPEC-01 |
| Integração | persistência real, API, módulos, atomicidade | QUALITY-SPEC-02 |
| Contrato | OpenAPI × controllers × tipos do cliente | QUALITY-SPEC-03 |
| Ponta a ponta | jornadas do utilizador sobre o conjunto | QUALITY-SPEC-04 |
| Segurança, desempenho, regressão | dependências, limites, cargas, defeitos conhecidos | QUALITY-SPEC-05 |

### Rastreabilidade

Cadeia `evidência (DESC) → regra (RN) → critério de aceitação → teste`. Cada critério de aceitação das specs de módulo tem pelo menos um teste identificável; a [matriz](../00-engenharia-reversa/matriz-rastreabilidade.md) é atualizada ao concluir cada módulo.

### Metas (a aprovar)

| Alvo | Meta |
|---|---|
| Regras de domínio financeiro (`RN-*`) | 100% das regras P/E/N com teste; ramificações de domínio ≥ 90% |
| Backend global | linhas ≥ 80% |
| Frontend (utilitários, validadores, estado) | ramificações ≥ 90%; páginas cobertas por estados e fluxos principais |
| Jornadas E2E | todas as de QUALITY-SPEC-04 passando |

Metas servem de guia; a cobertura nunca substitui cenários de risco (limites monetários, datas, falhas).

### Dados de teste

Somente sintéticos e determinísticos, criados por fixtures e scripts de teste (sem importação de backup — P-26); números de referência da origem (750/400, 1200, 700/800/950/1000/1200…) são fixtures canónicas (o −150 da origem passa a ser um caso de recusa — P-04). Cada teste cria os seus próprios utilizadores para exercitar o isolamento. Nunca backups ou valores reais (RT-10). Datas de teste com relógio injetável, não `now()` direto.

### Evidência e portões

Segue [Como executar](../../shared/como-executar.md): distinguir teste executado, apenas escrito e verificação não executada. Portão por alteração: build, testes do módulo, contrato e `ModularityTests`. Portão de entrega: todas as jornadas E2E e a revisão dos itens de regressão.

### Tratamento de defeitos

Todo defeito encontrado ganha um teste de regressão antes da correção. Os defeitos candidatos e as regras mudadas por decisão do produto: DESC-REP-03, DESC-CAT-06, DESC-SUB-02/03, DESC-BOX-06/07 e o isolamento por utilizador (P-01).

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [Matriz de rastreabilidade](../00-engenharia-reversa/matriz-rastreabilidade.md)
- [BACKEND-SPEC-09](../14-backend-testes/spec.md)
- [FRONTEND-SPEC-08](../31-frontend-testes/spec.md)

## Regras e critérios de aceitação

- [ ] Cada `RN-*` e cada critério de aceitação tem teste identificável (verificado pela matriz).
  > ⏳ 2026-10-29 16:20 +00:00 — Cada critério de aceitação das specs de módulo fechadas (15–20, 22, 32–37, 48, 49) traz, na evidência, o teste que o cobre. Falta a conferência automatizada pela matriz (RN × teste) e das regras de fatias com critérios ainda abertos.
- [x] Nenhum teste usa dados reais; datas dependem de relógio injetado.
  > ✅ 2026-10-29 16:20 +00:00 — Dados sintéticos em todos os níveis (fixtures, `generate_series`, utilizadores `@example.com` criados pelo próprio teste). O relógio: o backend injeta `Clock` e os testes usam datas fixas; o E2E fixa o relógio do navegador em 15/06/2026 (`page.clock`).
- [ ] As metas de cobertura estão aprovadas e medidas no CI.
  > ⏳ 2026-10-29 16:20 +00:00 — Metas aprovadas pelo utilizador em 2026-10-29 (backend ≥ 80% de linhas, domínio ≥ 90% de ramificações, frontend utilitários/validadores ≥ 90%) e medidas localmente: backend 97,8% de linhas e domínio 96,4% de ramificações (JaCoCo); frontend 96,7% (validadores/formatação) e 94,9% (estado) de ramificações (v8). O CI publica os relatórios (`backend-test-reports`, `frontend-coverage`), mas não há portão e o CI ainda não rodou no GitHub.
- [x] Todo defeito corrigido tem teste de regressão que falha sem a correção.
  > ✅ 2026-10-29 16:20 +00:00 — Defeitos achados na F12 e o teste que falhava sem a correção: caractere NUL em texto → 500 (`SecurityHardeningIntegrationTest.controlCharactersNeverCauseAServerError`); `<li>` sem lista em Categorias, botões de moeda sem o texto visível no nome e região rolável sem foco por teclado (as varreduras axe de `a11y.spec.ts` falhavam antes e passam depois). Defeitos das fatias anteriores têm teste (DESC-REP-03, saldo negativo, isolamento etc.).
- [x] Evidências distinguem executado, escrito e não executado.
  > ✅ 2026-10-29 16:20 +00:00 — Seguido em todas as specs fechadas (✅ executado, ⏳ parcial com o que falta; o CI no GitHub aparece sempre como "criado, não executado").

### Skills de implementação

- **Skills obrigatórias:** `java-modern-engineering` e `angular-architect` (orientação de teste por camada).
- **Caminhos:** `C:\Users\dowgl\.codex\skills\java-modern-engineering\SKILL.md`; `.claude/skills/angular-architect/SKILL.md`.
- **Responsabilidade:** boas práticas de teste Java e Angular conforme as referências das skills (`code-quality-and-testing`, `architecture-quality`).
- **Momento de utilização:** ao escrever testes em cada módulo.
- **Dependências:** BACKEND-SPEC-09, FRONTEND-SPEC-08.
- **Limitações:** **sem skill dedicada a estratégia/qualidade de software** nos diretórios indicados.

## Tasks

### Descoberta e decisão

- [x] Aprovar metas de cobertura e portões.
  > ✅ 2026-10-29 16:20 +00:00 — Metas aprovadas pelo utilizador em conversa em 2026-10-29 (cobertura e desempenho: p95 ≤ 300/500/800 ms). Portões: o CI roda testes e varreduras; limites de cobertura não bloqueiam (decisão: a cobertura guia, não substitui cenários de risco).

### Validação

- [x] Medir a cobertura de base após o primeiro módulo e registar.
  > ✅ 2026-10-29 16:20 +00:00 — Medida em 2026-10-29 (ver o critério de metas). JaCoCo 0.8.14 no `pom.xml` (relatório na fase `verify`) e `@vitest/coverage-v8`.

## Resultado esperado

- Qualidade mensurável, rastreável e proporcional ao risco financeiro.
- Preservados os valores de referência numéricos da origem como fixtures.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
