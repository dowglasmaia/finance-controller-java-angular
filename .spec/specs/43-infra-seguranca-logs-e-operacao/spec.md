# 43-infra-seguranca-logs-e-operacao

> **IDs:** INFRA-SPEC-09 (segredos, permissões e segurança), INFRA-SPEC-10 (logs, métricas e monitoramento), INFRA-SPEC-12 (operação e manutenção) · **Categoria:** Infraestrutura · **Estado:** executada na F12 (2026-10-29); execução dos scanners no CI, ensaio de reversão e escolha de alertas/segredos de produção pendentes (P-18, P-27).

## Objetivo

Definir a gestão de segredos e permissões, a coleta de logs e métricas e a rotina de operação e manutenção do ambiente de produção.

## Escopo

- Inclui: segredos, permissões mínimas, retenção de logs, métricas, alertas básicos e rotinas de manutenção.
- Não inclui: autenticação de utilizadores (BACKEND-SPEC-05), configuração de aplicação (BACKEND-SPEC-08).

## Contexto técnico local

### Origem

Não havia segredos, servidor, logs nem monitoramento (DESC-INF-03). Tudo isto é novo no destino e, portanto, PROPOSTO.

### Segredos e permissões (INFRA-SPEC-09)

- Segredos (senha do banco, `FC_JWT_SECRET`, chaves futuras) em variáveis de ambiente injetadas pelo cofre/segredos da plataforma; nunca em ficheiros versionados, imagens ou logs.
- Rotação documentada da senha do banco; contas separadas para migração e execução.
- Permissões de ficheiros e redes: banco acessível só pelo backend; frontend estático sem acesso ao banco; portas mínimas expostas.
- Análise de segredos acidentais no repositório no CI (QUALITY-SPEC-05).

### Logs, métricas e monitoramento (INFRA-SPEC-10)

| Tema | Proposta |
|---|---|
| Logs | JSON estruturado, correlação por pedido, sem valores financeiros (BACKEND-SPEC-08); retenção a definir |
| Métricas | Actuator: HTTP, JVM, pool do banco; exposição interna |
| Saúde | liveness/readiness; verificação externa simples de `/actuator/health` |
| Alertas básicos | serviço indisponível, taxa de erro 5xx, falha de backup, disco do banco |
| Frontend | registo de erros do cliente sem dados financeiros; decisão de ferramenta pendente |

### Operação e manutenção (INFRA-SPEC-12)

- Rotina mensal: atualizar dependências e imagens base, verificar vulnerabilidades, rever logs de erro, confirmar o último backup restaurável.
- Procedimentos documentados: reiniciar serviços, aplicar migração, reverter versão, rodar a rotação do segredo de assinatura (invalida sessões).
- Registo de mudanças: cada alteração de esquema ou configuração referenciada a uma spec.
- Contas de utilizadores da aplicação: gestão administrativa (bloqueio, remoção) não está no escopo; se necessária, é uma spec futura.
- Envio de e-mail: monitorar a taxa de falha e a entregabilidade (SPF/DKIM do domínio remetente) quando o provedor for escolhido (BACKEND-SPEC-18).

### Riscos

Dados financeiros pessoais em logs ou backups mal protegidos. Todo backup deve ficar em local de acesso restrito (INFRA-SPEC-02).

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [BACKEND-SPEC-08](../13-backend-configuracao-logs-e-observabilidade/spec.md)
- [INFRA-SPEC-02](../40-infra-banco-de-dados/spec.md)

## Regras e critérios de aceitação

- [ ] Nenhum segredo em repositório, imagem ou log; verificação automática no CI.
  > ⏳ 2026-10-29 16:20 +00:00 — Repositório e histórico do Git: gitleaks, `no leaks found` (árvore e histórico; `.gitleaks.toml` dispensa só credenciais sintéticas de teste e os valores fictícios documentados de desenvolvimento). Imagens: Trivy `--scanners vuln,secret`, 0 segredos nas imagens `api` e `web`. Logs: `SecurityHardeningIntegrationTest.logsNeverHoldPasswordsTokensEmailsOrFinancialValues` e `PasswordResetMailFailureIntegrationTest`. A verificação automática está no `security-and-contract.yml` (gitleaks, OSV-Scanner, npm audit, Trivy), criado mas **ainda não executado no GitHub Actions**.
- [x] Utilizador do banco da aplicação sem privilégios de DDL em `prod`.
  > ✅ 2026-10-29 16:20 +00:00 — Ver INFRA-SPEC-02 (spec 40): `ProductionProfileIntegrationTest` e `scripts/db/prod-roles.sql`.
- [x] Logs e métricas acessíveis a quem opera, sem valores financeiros nos logs.
  > ✅ 2026-10-29 16:20 +00:00 — Uma linha JSON (ECS) por pedido com `requestId`/`traceId`, sem corpo, consulta, cabeçalhos nem valores; o teste de higiene de logs cobre senha, tokens, e-mail e valores financeiros. Métricas por `/actuator/metrics` (autenticada) e OpenTelemetry opcional; saúde `liveness`/`readiness` (com banco). Como operar: `.spec/shared/operacao.md`.
- [x] Existe alerta (ou verificação periódica documentada) para indisponibilidade e falha de backup.
  > ✅ 2026-10-29 16:20 +00:00 — Verificação periódica documentada em `.spec/shared/operacao.md` (readiness a cada 5 minutos, 5xx e disco diariamente). Falha de backup não se aplica enquanto o backup estiver fora do escopo (P-27). Ferramenta de alerta de produção depende da hospedagem (P-18).
- [ ] O guia de operação cobre reinício, migração, reversão e rotação do segredo de assinatura, e foi ensaiado.
  > ⏳ 2026-10-29 16:20 +00:00 — Guia escrito em `.spec/shared/operacao.md` (reinício, migração Flyway, publicação e reversão por tag, rotação do segredo JWT e da senha do banco, recuperação de senha, rotina mensal). Ensaiado: subida em `prod` com/sem variáveis, utilizador sem DDL, saúde 503 sem banco, migração do zero, rotação do segredo (`SecurityHardeningIntegrationTest`), imagem `web` e suas respostas. **Não ensaiado:** reversão com restauração do banco (sem backup, P-27), alerta externo e rotação da senha numa instância real.

### Skills de implementação

- **Skills obrigatórias:** nenhuma confirmada.
- **Caminhos:** —
- **Responsabilidade:** —
- **Momento de utilização:** —
- **Dependências:** INFRA-SPEC-02/07; BACKEND-SPEC-05/08; P-18 (hospedagem adiada).
- **Limitações:** **sem skill de segurança, observabilidade ou operação nos diretórios indicados.** Existem skills equivalentes noutros catálogos da ferramenta, mas fora dos caminhos exigidos; não foram avaliadas nem associadas.

## Tasks

### Descoberta e decisão

- [ ] Escolher a forma de gerir segredos e a ferramenta de monitoramento quando a hospedagem for definida (P-18).
  > ⏳ 2026-10-29 16:20 +00:00 — Decisão adiada por P-18 (hospedagem): hoje os segredos são variáveis de ambiente (`.env` fora do Git no desenvolvimento) e o guia diz como entram em produção. Aberto até a hospedagem ser escolhida.

### Validação

- [ ] Ensaiar o guia de operação e registar o resultado.
  > ⏳ 2026-10-29 16:20 +00:00 — Ensaio parcial registrado no próprio guia (tabela "O que foi ensaiado na F12 e o que não foi").

## Resultado esperado

- Produção com segredos protegidos, observável e com rotina de manutenção.
- Preservada a regra de não registar dados financeiros reais.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
