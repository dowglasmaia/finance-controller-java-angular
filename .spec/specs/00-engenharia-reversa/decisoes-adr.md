# Registo de decisões arquiteturais

Estado: `proposta`, `aprovada` (decisão do responsável do produto em 2026-10-24, ver [pendencias.md](pendencias.md)), `delegada` (escolha da análise a pedido, aguarda objeção), `cancelada`. Complementam, sem repetir, o [contexto técnico global](../../memory/contexto-tecnico.md).

| ID | Título | Estado | Origem da decisão | Specs |
|---|---|---|---|---|
| ADR-001 | Módulos do monólito e dependências por portas | proposta | — | 03, 06, 12 |
| ADR-002 | Dinheiro em decimal exato | aprovada | P-07 | 03, 05, 07, 08 |
| ADR-003 | Data financeira `LocalDate`, auditoria `Instant`, mês `YYYY-MM` | proposta | — | 03, 08 |
| ADR-004 | Contratos OpenAPI por módulo | aprovada | P-08 | 05, 06, 09 |
| ADR-005 | Regras e alertas de meta no backend | proposta | P-20 (mês de referência) | 03, 12, 16, 18 |
| ADR-006 | Importação de backup atómica | **cancelada** | P-03, P-16 | 21 |
| ADR-007 | Erros como Problem Details com código estável | proposta | — | 03, 10 |
| ADR-008 | Preferências por utilizador no servidor | delegada | P-19 | 22, 27 |
| ADR-009 | Compatibilidade do backup JSON v2 | **cancelada** | P-03, P-16 | 21 |
| ADR-010 | Frontend por funcionalidade, signals, formulários tipados | proposta | — | 23 |
| ADR-011 | API versionada em `/api/v1` | proposta | — | 05 |
| ADR-012 | Identidade, autenticação e isolamento por proprietário | aprovada (política) / delegada (técnica) | P-01, P-24, P-25 | 08, 11, 12, 29, 48, 49 |
| ADR-013 | Bibliotecas de frontend: ApexCharts e Lucide | aprovada (gráficos) / delegada (ícones) | P-17, P-22 | 23, 25 |
| ADR-014 | Entrega em imagens Docker e CI mínimo por esteira | aprovada | P-02, P-18 | 41, 42 |

## ADR-001 — Módulos do monólito e dependências por portas

- **Decisão:** módulos `identity`, `category`, `transaction`, `budget`, `moneybox`, `report`, `preferences`. Dependências: `transaction→category`; `budget→category,transaction`; `report→category,transaction,moneybox,budget`; todos dependem de `identity` apenas para obter o proprietário do contexto. Portas invertidas: `CategoryUsageGuard` (definida em `category`, implementada em `transaction`) e `BudgetImpactPort` (definida em `transaction`, implementada em `budget`). O módulo `backup` deixa de existir (P-03).
- **Consequências:** `ModularityTests` valida os limites; o alerta é síncrono e transacional.

## ADR-002 — Dinheiro em decimal exato (aprovada)

`BigDecimal` escala 2 no Java, `NUMERIC(19,2)` no banco, string decimal no JSON (detalhe técnico). Limiares comparados sem divisão (`gasto×5 ≥ limite×4`). Arredondamento `HALF_UP` apenas na exibição e onde houver conversão.

## ADR-003 — Datas e meses

Data financeira `LocalDate`; criação/atualização `Instant` UTC; mês `YYYY-MM`. Intervalos de período inclusivos por dia.

## ADR-004 — Contratos por módulo (aprovada)

Um contrato OpenAPI 3.1 por módulo em `src/main/resources/openapi/modules/<modulo>.yaml`, erros comuns em `openapi/common/responses.yaml` (`ProblemDetail`), uma execução do OpenAPI Generator por contrato com pacotes próprios. O contrato único do scaffold (`openapi/openapi.yaml`, módulo Modulith `generated` aberto) é **preservado** até o módulo de exemplo ser removido, conforme a skill `openapi-rest-contract`; nenhuma migração silenciosa.

## ADR-005 — Regras e alertas de meta no servidor

O servidor calcula e devolve `budgetAlert` na resposta de criar/atualizar despesa. Por decisão P-20, o mês avaliado é o **mês de referência enviado pelo cliente**: se a data da despesa não pertence a esse mês, o progresso do mês de referência não muda e não há alerta (comportamento da origem).

## ADR-006 e ADR-009 — Canceladas

Dependiam de importação e exportação de backup, removidas por P-03 e P-16. Ficam registadas para preservar o histórico.

## ADR-007 — Problem Details

Erros `application/problem+json` (RFC 9457) com `code` estável e `errors[{field,message}]`; esquema comum no contrato; mensagens da origem preservadas.

## ADR-008 — Preferências por utilizador (delegada)

Com login (P-01), moeda e tema passam a ser guardados no servidor por utilizador (tabela `preference`), seguindo-o entre dispositivos. Antes do login a interface usa `localStorage` apenas para o tema e o padrão `BRL`. Alternativa descartada: `localStorage` apenas, por perder a preferência ao trocar de dispositivo.

## ADR-010 — Frontend por funcionalidade

`features/<modulo>` com rotas, `doc.md`, `pages/`, `ui/`, `data-access/`; signals, RxJS para fluxos assíncronos, Reactive Forms tipados, `OnPush`, zoneless (padrão do scaffold), Tailwind com tokens semânticos.

## ADR-011 — Versionamento

Prefixo `/api/v1`; alterações incompatíveis criam nova versão.

## ADR-012 — Identidade, autenticação e isolamento

- **Aprovado (P-01):** autenticação por e-mail e senha; cada utilizador vê e altera apenas os seus dados; sem partilha.
- **Complementos decididos:** sem passo extra de confirmação no login (P-25); recuperação de senha por e-mail com link de uso único (P-24, [spec 48](../48-backend-recuperacao-de-senha/spec.md)); verificação de e-mail e exclusão de conta não solicitadas.
- **Proposto (detalhe técnico, sem objeção do responsável):**
  - módulo `identity` com cadastro, entrada, renovação, saída e consulta do utilizador atual;
  - senha com *hash* adaptativo (algoritmo padrão do Spring Security ou Argon2id), nunca registada nem devolvida; e-mail normalizado em minúsculas e único;
  - *access token* JWT de curta duração (~15 min) + *refresh token* aleatório rotativo, guardado com *hash* no banco e enviado em cookie `HttpOnly`/`Secure`/`SameSite`; o *access token* fica só em memória no frontend;
  - limitação de tentativas de entrada e mensagens de erro genéricas;
  - o proprietário vem sempre do token, **nunca** de parâmetro do cliente; todos os repositórios filtram por `owner_id`; recurso de outro utilizador responde 404;
  - ao cadastrar, o servidor cria as 11 categorias padrão do novo utilizador (substitui a flag `seeded` do cliente).
- **Consequências:** `owner_id` em todas as tabelas de dados desde a primeira migração; testes de isolamento obrigatórios por recurso; o módulo `identity` passa a ter uma integração de e-mail (porta `MailSender`).

## ADR-013 — Bibliotecas de frontend (delegada)

- **Gráficos (escolha do responsável, 2026-10-24):** **ApexCharts** via `ng-apexcharts`, pelo visual moderno. A análise tinha recomendado Chart.js (canvas, mais leve), mas a preferência do produto prevalece; os dois são adequados a barras duplas e pizza. Pontos de atenção: bundle maior; renderização em SVG, que facilita acessibilidade e temas; cores vindas dos tokens Neon Graphite; alternativa textual (tabela) para cada gráfico; carregamento sob demanda na rota de dashboard e relatórios para não pesar o início.
- **Ícones:** Lucide (`lucide-angular`), tree-shakable, com tabela de correspondência dos 24 ícones de categoria e 10 de cofrinho da origem (nomes Material).
- **Condição:** confirmar compatibilidade das duas com Angular 21 antes de instalar; a skill proíbe adicionar dependências sem necessidade.

## ADR-014 — Entrega em Docker e CI mínimo

Backend e frontend são entregues como imagens Docker independentes. CI no GitHub Actions com duas esteiras separadas (backend e frontend), cada uma com *build* e testes; o deploy fica preparado, sem destino, até a hospedagem ser escolhida (P-18). O repositório e a raiz dos fluxos são criados pelo responsável (P-23).
