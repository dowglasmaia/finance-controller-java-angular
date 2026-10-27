# 13-backend-configuracao-logs-e-observabilidade

> **ID:** BACKEND-SPEC-08 · **Categoria:** Backend · **Estado:** proposta para revisão — não aprovada, não executada.

> **BACKEND-SPEC-07 (integrações e processamento assíncrono): sem requisito vindo da origem, mas aplicável ao destino por um motivo.** A origem não faz chamadas de rede nem tem tarefas agendadas (DESC-INF-03); o backup automático em pasta foi removido (P-03). A única integração externa do destino é o **envio de e-mail** da recuperação de senha (P-24), com tarefa de limpeza de tokens expirados — especificada em [BACKEND-SPEC-18](../48-backend-recuperacao-de-senha/spec.md), que cobre este ID. Não há outras integrações nem processamento assíncrono a especificar.

## Objetivo

Definir configuração por perfil, logs e observabilidade básica do backend, estendendo o Actuator já presente no scaffold, sem acrescentar infraestrutura que a origem não exigia.

## Escopo

- Inclui: perfis, propriedades externalizadas, formato de logs, health checks, métricas básicas e metas de desempenho do backend.
- Não inclui: coleta centralizada, tracing distribuído, alertas de operação (INFRA-SPEC-10).

## Contexto técnico local

### Ponto de partida (CONFIRMADO na spec 01)

`application.yml` com perfil padrão `local` e exposição `health,info,metrics` em `/actuator`; porta por `PORT`. A origem não tinha logs, métricas nem variáveis de ambiente (DESC-INF-03).

### Configuração (PROPOSTO)

| Tema | Regra |
|---|---|
| Perfis | `local`, `test`, `prod`; propriedades sensíveis só por variável de ambiente. |
| Banco | `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`; pool com limites explícitos. |
| CORS | Lista de origens permitidas por variável. |
| Segurança | `FC_JWT_SECRET` (assinatura dos tokens), tempos de vida dos tokens, limites de tentativas de entrada; segredo só por variável. |
| Fuso | Servidor em UTC; datas financeiras não dependem de fuso (ADR-003). |
| Idioma | `pt_BR` para mensagens e formatação. |

### Logs

- Logs estruturados (JSON em `prod`), nível `INFO` padrão; identificador de correlação por requisição.
- Nunca registar valores financeiros, descrições de lançamentos, e-mails, senhas ou tokens.
- Erros inesperados com pilha apenas no log, nunca na resposta (BACKEND-SPEC-04).

### Observabilidade

- `GET /actuator/health` com sondas de liveness e readiness; readiness inclui o banco.
- Métricas de HTTP e do pool do banco via Actuator; exposição restrita em `prod` (apenas `health` público; `metrics` interno).
- Medição de duração de `getReport`, `getMonthlyOverview` e `listTransactions` (candidatos a gargalo).

### Desempenho (PROPOSTO, a aprovar)

Metas de GLOBAL-SPEC-01 (p95 < 500 ms para 50 000 lançamentos) com índices de BACKEND-SPEC-02; sem N+1 nas listagens.

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [Scaffold](../01-scaffold-backend-spring-modulith/spec.md)
- [INFRA-SPEC-10](../43-infra-seguranca-logs-e-operacao/spec.md)

## Regras e critérios de aceitação

- [ ] A aplicação arranca em `local`, `test` e `prod` com a configuração correspondente e falha de forma clara se faltar variável obrigatória.
  > ⏳ 2026-10-27 — Parcial: arranque em `local` (API real) e `test` (suíte) verificado; segredo JWT ausente ou curto é recusado na validação das propriedades (`IdentityPropertiesTest`). Falta: arranque no perfil `prod`.
- [ ] `/actuator/health` indica `UP` com banco disponível e `DOWN` sem ele.
  > ⏳ 2026-10-27 — Parcial: `UP` verificado na API real com PostgreSQL. Falta: `DOWN` sem banco.
- [ ] Nenhum log contém valores financeiros ou segredos (verificação por teste de log).
  > ⏳ 2026-10-27 — Parcial: auditoria manual dos logs sem segredos (ver spec 11). Falta: teste automatizado de logs; valores financeiros só existem a partir da F5.
- [ ] Cada resposta traz identificador de correlação presente nos logs do pedido.
  > ⏳ 2026-10-27 — Parcial: `TracingCorrelationTests` verde (`traceId` no MDC) e `ProblemDetail` com `traceId` e `correlationId`. Falta: conferir o identificador em respostas de sucesso e nos logs do pedido.
- [ ] Medições de desempenho dos três pontos candidatos são registadas.
- [x] Logs não contêm e-mails, senhas, tokens nem valores financeiros.
  > ✅ 2026-10-27 — Verificação: busca por e-mail de teste, senhas, JWT, cookie de renovação e segredo nos logs de uma execução real (cadastro, login, saída, 409, 429) e do `verify`: 0 ocorrências. Limitação: auditoria manual; ainda não há valores financeiros. Os logs do `verify` incluem a mensagem de uma exceção provocada de propósito pelo teste de vazamento.

### Skills de implementação

- **Skills obrigatórias:** `java-modern-engineering`.
- **Caminhos:** `C:\Users\dowgl\.codex\skills\java-modern-engineering\SKILL.md`.
- **Responsabilidade:** código de configuração, filtros de correlação e medições.
- **Momento de utilização:** junto do primeiro módulo funcional.
- **Dependências:** BACKEND-SPEC-02; INFRA-SPEC-03.
- **Limitações:** **sem skill de observabilidade/operação nos diretórios indicados.**

## Tasks

### Back-end

- [ ] Criar os perfis, as propriedades externalizadas e o filtro de correlação.
- [ ] Configurar logs estruturados e as sondas de saúde com banco.

### Validação

- [ ] Executar testes de arranque por perfil e de saúde; registar tempos medidos.

## Resultado esperado

- Backend configurável por ambiente, observável de forma básica e sem vazamento de dados.
- Preservado o Actuator do scaffold.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
