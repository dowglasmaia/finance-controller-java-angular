# Como trabalhamos com IA neste projeto

> Documento de estudo e de explicação a terceiros. Descreve o processo que usamos de facto (não um ideal teórico) e o relaciona com os conceitos de **SDD**, **Harness Engineering**, **Loops de agente** e **Engenharia de Grafos**.
> Escrito durante a fatia F1. Revise quando o processo mudar.

## 1. Resumo em 30 segundos

Não pedimos "faz-me uma app" a um modelo. Trabalhamos assim:

1. **As specs são a fonte da verdade** (o que construir, como validar).
2. **O modelo executa uma fatia pequena de cada vez**, dentro de um ambiente controlado (o *harness*): instruções, memória, ferramentas, permissões e testes.
3. **Cada fatia segue um ciclo curto**: prévia → aprovação humana → implementação → verificação → evidência.
4. **O humano decide e commita**; a IA propõe, implementa e prova.

Em uma frase: **Spec-Driven Development, executado dentro de um harness, em loops curtos com aprovação humana, planeado como um grafo de dependências.**

## 2. As quatro ideias e onde encaixam

| Conceito | O que é | Como aparece aqui |
|---|---|---|
| **SDD** (Spec-Driven Development) | A especificação aprovada vem antes do código e governa-o | `.spec/specs/*`, `.spec/memory/*`, regra "especificar e aprovar antes de implementar" |
| **Harness Engineering** | Engenharia de tudo o que rodeia o modelo para o tornar fiável: contexto, ferramentas, limites, verificação, memória | `CLAUDE.md`, `como-executar.md`, skills, subagentes, testes, memória persistente, permissões |
| **Loop (agente)** | Ciclo repetido: planear → agir → observar → verificar → corrigir | O ciclo por fatia e por tarefa; `mvnw verify` como "observação" |
| **Engenharia de Grafos** | Modelar o trabalho/conhecimento como nós e dependências | Roadmap F0–F12 como grafo de dependências; paralelismo por fan-out/fan-in |

Importante: **harness é o conceito mais abrangente**. SDD é a *camada de especificação* do harness; o loop é o *comportamento em execução*; o grafo é a *forma de planear a ordem e o paralelismo*.

## 3. SDD: a spec manda

**Ideia:** o código é consequência da spec. Se o comportamento muda, muda-se a spec primeiro, com autorização.

Como organizamos (`.spec/`):

- `memory/produto.md` e `memory/contexto-tecnico.md`: o "porquê" e as restrições técnicas globais.
- `specs/NN-nome/spec.md`: uma spec por assunto. Cada uma tem **Objetivo, Escopo, Contexto técnico, Critérios de aceitação, Cenários de teste, Tasks e Encerramento**.
- `specs/47-implementation-plano-e-sequencia`: define as **fatias F0–F12** e a ordem.
- `shared/roadmap.md`: visão derivada da spec 47 (mapa, não fonte).
- `shared/como-executar.md`: o procedimento de execução (o "manual de operação" do agente).
- `CLAUDE.md` (raiz): regras permanentes carregadas em toda sessão.

Regras que tornam o SDD real (não decorativo):

- Tarefa só é marcada `[x]` **com evidência verificada**. Sem prova, fica `⏳` parcial, com nota do que falta. (Exemplo real: spec 11 ficou parcial porque o Docker não respondeu.)
- Alterações de contrato ou comportamento exigem aprovação (ex.: aprovámos `REQUEST_NOT_SUPPORTED` antes de entrar no catálogo da spec 10).
- Trabalhar só dentro do escopo da spec.

## 4. Harness Engineering: o ambiente que torna o modelo fiável

**Ideia:** o desempenho de um agente depende menos do "prompt inteligente" e mais do **sistema à volta**. Um modelo bom num ambiente mal desenhado erra; um ambiente bem desenhado reduz erro, desvio e retrabalho.

Os componentes do nosso harness:

| Componente do harness | Nosso equivalente | Para que serve |
|---|---|---|
| **Instruções permanentes** | `CLAUDE.md` | Regras que nunca podem ser esquecidas (owner_id, BigDecimal, 1 use case por operação, sem H2) |
| **Contexto sob demanda** | `.spec/memory/*`, spec da fatia | O modelo lê só o que a fatia precisa |
| **Procedimento** | `como-executar.md` + ordem interna da fatia | Evita pular etapas |
| **Skills** | `openapi-rest-contract`, `java-spring-create-module`, `java-modern-engineering` | Procedimentos especializados e repetíveis (ex.: gerar contrato com validação) |
| **Subagentes** | Agentes em paralelo (use cases, persistência, segurança, controller) | Dividir trabalho independente sem poluir o contexto principal |
| **Ferramentas e permissões** | Leitura/escrita de ficheiros, Maven, modo de permissões | O que o agente pode fazer, com aprovação onde importa |
| **Verificação automática** | `mvnw verify`, testes de domínio/casos de uso, `ModularityTests` (Spring Modulith), Redocly lint, Testcontainers | O modelo não "acha que está certo": o sistema prova |
| **Memória persistente** | Pasta `memory/` (ex.: "commits sempre manuais", "atualizar Postman a cada fatia") | Preferências do humano sobrevivem entre sessões |
| **Portões humanos** | Prévia de contrato, aprovação de decisões, commits | Decisões irreversíveis ou de produto ficam com a pessoa |
| **Artefactos de rastreio** | Evidências nas tasks, matriz de rastreabilidade, ADRs | Auditoria: o que foi feito, como foi provado |

Tradução prática: **quando algo corre mal, a pergunta certa não é "o modelo errou?", é "que parte do harness permitiu o erro?"**. Exemplos reais:

- O teste de vazamento acusou "constraint" falsamente → corrigimos o teste (verificação), não culpámos o modelo.
- O Docker não funcionou → o harness impôs **não declarar sucesso**; ficou registado como não verificado.
- Agentes não podem rodar Maven em simultâneo (partilham `target/`) → regra de coordenação; quem integra roda o build.

## 5. Loop: o ciclo de trabalho

Há dois loops, em escalas diferentes.

**Loop macro (por fatia):**

```
Spec da fatia ──► Prévia/proposta ──► Aprovação humana ──► Implementação
      ▲                                                        │
      │                                                        ▼
 Encerramento ◄── Evidência nas specs ◄── Verificação (mvnw verify, testes)
```

**Loop micro (dentro da implementação):** escrever → compilar/testar → ler o erro → corrigir → repetir até verde. É o loop clássico de agente (agir → observar → ajustar). O humano **não** está dentro deste loop; está nas fronteiras (aprovar antes, validar depois).

Ordem interna fixa da fatia (cada passo desbloqueia o seguinte):

`contrato → migração → domínio → casos de uso → controller → testes de isolamento`

Diferença para "loops autónomos longos" (agente rodando horas sozinho): **o nosso loop é curto e com portões humanos**. Escolha deliberada, porque erro de contrato ou de segurança é caro e o domínio (dinheiro, dados de utilizadores) pede controlo.

## 6. Engenharia de Grafos: a ordem como grafo de dependências

**Ideia:** o trabalho é um grafo orientado acíclico (DAG): nós = unidades de trabalho, arestas = "precisa de". Isso responde a duas perguntas: *qual a ordem válida?* e *o que pode correr em paralelo?*

Onde aplicamos:

- **Roadmap (mermaid):** F0…F12 com arestas de dependência (ex.: `F2 --> F8`, `F5 --> F9`, `F7 -.-> F9` pontilhada = dependência condicional). Corrigimos 4 inconsistências entre roadmap e spec 47: o grafo precisa de estar certo para a ordem estar certa.
- **Paralelismo (fan-out / fan-in):** dentro da F1, após contrato e migração, as partes independentes (use cases, persistência, segurança, controller) foram para **agentes em paralelo (fan-out)** e a integração e o build foram feitos por mim (**fan-in**). Só se paraleliza o que **não toca os mesmos ficheiros** nem depende do resultado do outro.
- **Grafo de módulos no código:** Spring Modulith + `ModularityTests` verificam que as dependências entre módulos respeitam o desenho (ex.: outros módulos só falam com `identity` via `CurrentOwner`).
- **Rastreabilidade:** spec → tasks → código → teste → evidência é uma cadeia navegável (um grafo de proveniência).

O que **não** estamos a fazer (para ser honesto ao explicar): não usamos um framework de grafos de agentes (tipo LangGraph) nem grafo de conhecimento/GraphRAG. Aplicamos o **pensamento em grafo** ao planeamento e à orquestração, de forma manual e documentada.

## 7. Onde estamos mais alinhados

| Abordagem | Alinhamento | Comentário |
|---|---|---|
| SDD | **Muito alto** | É a espinha dorsal |
| Harness Engineering | **Muito alto** | É o nome certo para o "como" completo; CLAUDE.md, skills, verificação, memória, portões |
| Loop de agente | **Médio-alto** | Loops curtos com humano nas fronteiras, não autónomos longos |
| Engenharia de Grafos | **Médio** | Usada no planeamento e paralelismo; sem tooling de grafos |

Se tiver de resumir o processo num rótulo: **"SDD sobre um harness, com loops curtos supervisionados"**.

## 8. Exemplo guiado: a fatia F1 (identity)

1. **Spec 47** define F1 e a ordem. **Spec 11** define o comportamento de auth.
2. **Prévia do contrato** (skill `openapi-rest-contract`) → humano aprova → contrato gerado e validado (Redocly + geração Maven).
3. **Migração V1** + esqueleto + tratador de erros + compose, em **paralelo** (agentes), tarefas independentes.
4. **Domínio, casos de uso, persistência, segurança, controller** em **paralelo** (fan-out), cada agente com brief de escopo fechado.
5. **Integração** (fan-in): compilar, rodar 135 testes sem Docker, corrigir.
6. **Evidência honesta:** testes com PostgreSQL escritos mas **não executados** → tasks ficam `⏳`, não `[x]`.
7. **Humano** decide o próximo passo e faz os commits.

## 9. Princípios para guardar

1. **Spec antes de código;** mudança de comportamento exige autorização.
2. **Fatias pequenas e verificáveis;** uma de cada vez, em série; paralelismo só dentro da fatia e só onde é independente.
3. **Prova, não opinião:** "feito" = evidência (build, teste, lint). Sem prova → parcial.
4. **O humano guarda as decisões irreversíveis:** aprovações de contrato/decisões e todos os commits.
5. **Corrigir o sistema, não só o resultado:** se um erro se repete, melhora-se a regra, o teste ou a memória.
6. **Segurança e isolamento por desenho:** `owner_id` em tudo, teste de isolamento por módulo.
7. **Registar o porquê** (ADR, notas de desvio) para a próxima pessoa (ou sessão) não redescobrir.

## 10. Limites e riscos (para discutir com franqueza)

- **Dependência do ambiente de verificação:** sem Docker, parte das provas não existe. O harness não deve "fingir".
- **Paralelismo tem custo de integração:** agentes podem divergir em convenções; o fan-in precisa de revisão e build.
- **Documentação pode desviar do código:** por isso as tasks só fecham com evidência e o roadmap é derivado da spec 47.
- **Contexto do modelo é finito:** daí specs curtas por fatia, memória persistente e resumos entre sessões.
- **Aprovação humana é gargalo deliberado:** troca velocidade por controlo; ajustar conforme a maturidade.

## 11. Como explicar a outra pessoa (roteiro de 2 minutos)

> "Cada funcionalidade tem uma especificação aprovada antes de se escrever código. O assistente de IA não trabalha livre: trabalha dentro de um ambiente com regras permanentes, procedimentos, testes automáticos e memória, que é o que chamamos de *harness*. Dividimos o projeto em fatias ordenadas por dependências, como um grafo, e executamos uma de cada vez. Em cada fatia, a IA propõe, eu aprovo, ela implementa, vários agentes podem trabalhar em paralelo no que é independente, e só damos algo por feito quando há prova: build e testes verdes. Os commits e as decisões de produto ficam comigo."

## 12. Glossário

- **SDD:** desenvolvimento orientado por especificação.
- **Harness:** o sistema à volta do modelo (instruções, ferramentas, memória, verificação, limites).
- **Loop de agente:** ciclo planear → agir → observar → corrigir.
- **Fatia (slice):** unidade entregável vertical (F0–F12), completa e verificável.
- **Fan-out / fan-in:** distribuir trabalho em paralelo / reunir e integrar os resultados.
- **DAG:** grafo orientado sem ciclos; representa dependências.
- **Portão humano (gate):** ponto em que a execução para até haver decisão da pessoa.
- **Evidência:** prova verificável de que uma tarefa está cumprida.
- **ADR:** registo de decisão de arquitetura.
- **Skill:** procedimento especializado reutilizável que o agente carrega quando precisa.
- **Subagente:** agente auxiliar com contexto próprio para uma subtarefa.

## 13. Para estudar a seguir

- Ler, por esta ordem: `CLAUDE.md` → `shared/como-executar.md` → spec 47 → `shared/roadmap.md` → uma spec de módulo (ex.: 11) → o código correspondente.
- Exercício: pegue numa task `⏳` e descreva (a) que prova falta, (b) de que parte do harness depende.
- Pesquisar os termos: *spec-driven development*, *harness engineering*, *agent loop*, *orchestrator-workers*, *DAG task planning*.
