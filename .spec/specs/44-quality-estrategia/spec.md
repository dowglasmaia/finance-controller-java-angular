# 44-quality-estrategia

> **ID:** QUALITY-SPEC-00 · **Categoria:** Qualidade · **Estado:** proposta para revisão — não aprovada, não executada.

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
- [ ] Nenhum teste usa dados reais; datas dependem de relógio injetado.
- [ ] As metas de cobertura estão aprovadas e medidas no CI.
- [ ] Todo defeito corrigido tem teste de regressão que falha sem a correção.
- [ ] Evidências distinguem executado, escrito e não executado.

### Skills de implementação

- **Skills obrigatórias:** `java-modern-engineering` e `angular-architect` (orientação de teste por camada).
- **Caminhos:** `C:\Users\dowgl\.codex\skills\java-modern-engineering\SKILL.md`; `.claude/skills/angular-architect/SKILL.md`.
- **Responsabilidade:** boas práticas de teste Java e Angular conforme as referências das skills (`code-quality-and-testing`, `architecture-quality`).
- **Momento de utilização:** ao escrever testes em cada módulo.
- **Dependências:** BACKEND-SPEC-09, FRONTEND-SPEC-08.
- **Limitações:** **sem skill dedicada a estratégia/qualidade de software** nos diretórios indicados.

## Tasks

### Descoberta e decisão

- [ ] Aprovar metas de cobertura e portões.

### Validação

- [ ] Medir a cobertura de base após o primeiro módulo e registar.

## Resultado esperado

- Qualidade mensurável, rastreável e proporcional ao risco financeiro.
- Preservados os valores de referência numéricos da origem como fixtures.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
