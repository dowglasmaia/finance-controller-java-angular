# 47-implementation-plano-e-sequencia

> **ID:** IMPLEMENTATION-SPEC-00 · **Categoria:** Planejamento · **Estado:** sequência por fatias verticais (F0–F12) aprovada pelo utilizador em 2026-10-25; F0 concluída; F1 e F2 concluídas em 2026-10-27 e F4 e F5 implementadas em 2026-10-29, todas com pendências registadas (ver "Estado das fatias"). Detalhe em [roadmap](../../shared/roadmap.md).

## Objetivo

Ordenar a futura implementação em fatias com dependências, paralelismo possível e critérios de conclusão, sem iniciar nenhuma delas.

## Escopo

- Inclui: sequência recomendada, dependências entre specs, condições de entrada de cada fatia, critérios de conclusão.
- Não inclui: execução de qualquer spec. Aprovar este plano **não** autoriza implementação (Como executar).

## Contexto técnico local

### Ponto de partida

[Spec 01](../01-scaffold-backend-spring-modulith/spec.md) concluída: projeto backend gerado e validado, com a ressalva de Docker não verificado. Nenhum frontend existe. Duas correções locais ao scaffold (controller não `final`; módulo `generated` aberto) devem ser lembradas se o scaffold for regenerado. Todas as decisões do produto (P-01…P-28) estão em [pendencias.md](../00-engenharia-reversa/pendencias.md); nenhuma pendência bloqueia o primeira fatia.

### Specs fora da sequência

- **Canceladas:** 21 (backup e importação) e 38 (página de backup) — P-03, P-16.
- **Adiada:** INFRA-SPEC-08 (deploy) — P-18.
- **Fora do escopo:** INFRA-SPEC-11 (cópia de segurança do banco) — P-27; verificação de e-mail e exclusão de conta — não solicitadas.
- **Novas:** 48 (recuperação de senha, backend) e 49 (recuperação de senha, frontend) — P-24.

### Marcos

Cada fatia entrega o backend de um módulo e, em seguida, a página correspondente, terminando num ponto de verificação testável pelo utilizador. As fatias executam **em série**, em ordem.

| Fatia | Conteúdo | Specs | Condição de entrada |
|---|---|---|---|
| F0 | Revisão e aprovação das specs; primeiro commit/repositório Git (P-23, manual); backend gerado já movido para `api-finance-controller/` na raiz (spec 01, concluído em 2026-10-24 — P-28) | 01, 02–05 | — |
| F1 Backend base + `identity` | Estrutura de módulos, erros, configuração, Flyway, cadastro/entrada/isolamento | 06, 08 (V1), 10, 11, 13, INFRA 39–41 | F0 |
| F2 Frontend base + autenticação | Scaffold Angular `app-finance-controller` na raiz, shell, tokens, estado, HTTP, formulários, guardas de rota | 23, 24, 25, 26, 27, 28, 29, 30 | F1 (contrato `identity`) |
| F3 CI mínimo | Esteiras de backend e frontend separadas | 42 | repositório remoto (P-23); F1 e F2 |
| F4 Categorias | Módulo + página; semeadura no cadastro; remover o módulo `example` | 15, 34 | F2 |
| F5 Lançamentos | Módulo + página; porta de alerta com implementação falsa | 16, 33 | F4 |
| F6 Metas de gastos | Módulo de metas, alerta real, página | 18, 36 | F5 |
| F7 Dashboard | Visão geral com meta, página | 17, 32 | F6 |
| F8 Cofrinhos | Módulo + página (saldo nunca negativo) | 19, 35 | F2 (independe de F4–F7) |
| F9 Relatórios | Módulo + página | 20, 37 | F5, F8 (e F7 se reutilizar consultas) |
| F10 Preferências | Módulo servidor e integração com o estado | 22, 27 | F2 |
| F11 Recuperação de senha | E-mail, token de uso único, revogação de sessões, SMTP de captura no `compose`; páginas `/esqueci-senha` e `/redefinir-senha` | 48, 49 | F2 |
| F12 Endurecimento | Consolidação de testes, segurança, E2E, observabilidade, operação | 14, 31, 40, 43–46 | fatias anteriores |

F8, F10 e F11 podem ser antecipadas após F2; F3 pode esperar até depois de F4 se não houver repositório remoto.

Hospedagem e deploy ficam fora da sequência até P-18 ser decidida.

### Estado das fatias (atualizado em 2026-10-29)

| Fatia | Estado | Evidência e pendências |
|---|---|---|
| F0 | ✅ Concluída | Commits `3135bd8` e `7a03399`; backend e frontend scaffold na raiz. Remoto ainda não verificado (entrada da F3). |
| F1 | ✅ Concluída em 2026-10-27, com pendências registadas | Contrato, migração V1, domínio, casos de uso, persistência, segurança e controller do `identity`. Verificação (2026-10-27): `./mvnw clean verify` com Docker, 159 testes, 0 falhas, incluindo migração, persistência e `IdentityFlowIntegrationTest` (rotação, reuso, 429, isolamento entre utilizadores). Correções feitas ao verificar: `InMemoryExampleRepository` deixou de ser `final`; o teste de fluxo passou a exigir cookie `Secure` por propriedade. Pendências: semeadura das 11 categorias (F4), recuperação de senha (F11), auditoria dos logs, isolamento nos módulos de dados, specs 10, 13 e INFRA 39–41 ainda sem fechamento formal, matriz de rastreabilidade. |
| F2 | ✅ Concluída em 2026-10-27 (escopo reduzido aprovado), com pendências registadas | Frontend base (specs 23, 29 e as partes de 24–28 e 30 usadas pelo shell e pela autenticação): build de produção e 72 testes Vitest verdes; percurso real no navegador contra o backend com PostgreSQL (cadastro, recarga com cookie rotativo, saída, login com `returnUrl`, 409 e 429). Escopo reduzido aprovado em 2026-10-27: `KpiCard`, `MoneyInput`, `ConfirmDialog`, `DataTable`, `ProgressBar`, pickers, gráficos e os formulários de lançamento, categoria, cofrinho, movimentação e metas passam para as fatias que os usam (F4–F9); consultas dependentes de mês/moeda, invalidação por mutação e preferências no servidor (F10) idem. Desvio a aprovar: cores do tema claro escurecidas para contraste. |
| F3 | ⏳ Implementada localmente em 2026-10-29; falta execução no GitHub Actions | Cinco fluxos em `.github/workflows/` (backend-ci, frontend-ci, dois esqueletos de deploy desabilitados). Verificação local: `mvnw clean verify` (159 testes), build e 73 testes do frontend, `docker build` das duas imagens. Pendente: commit/push manual e execução verde nas esteiras (spec 42). |
| F4 | ✅ Implementada em 2026-10-29; critérios que dependem de lançamentos ficam para a F5 | **Backend** (spec 15): contrato `categories` (7 operações, aprovado e gerado), V2 `category`/`subcategory` com unicidade por coluna gerada, módulo `category` (domínio, 7 casos de uso, adaptador JPA com filtro por `owner_id`, controller), SPI `CategoryUsageGuard` com implementação provisória "sem uso", semente das 11 categorias por evento síncrono `identity.UserRegistered` na transação do cadastro, módulo `example` removido. `./mvnw clean verify` com Docker: 181 testes, 0 falhas (inclui isolamento entre utilizadores, corrida concorrente por nome e `ModularityTests`). **Frontend** (spec 34): `/categorias` (abas, cartões, diálogo, subcategorias), `CategoriesStore`, `Modal` e `ConfirmDialog` compartilhados; build verde e 90 testes Vitest; verificado no navegador contra a API real. Postman e READMEs atualizados. Pendências: ~~bloqueios que exigem lançamentos reais (`CATEGORY_IN_USE`, `CATEGORY_TYPE_CONFLICT`, desassociação de subcategoria)~~ (fechados na F5) e o seletor de Metas → F6; utilizadores criados antes da F4 não têm categorias semeadas; teclado completo, 320 px e contraste na página. |
| F5 | ✅ Implementada em 2026-10-29; o alerta real de meta fica para a F6 | **Backend** (spec 16): contrato `transactions` (5 operações, aprovado e gerado), V3 `transaction_entry` (`numeric(12,2)`, CHECKs, `ON DELETE SET NULL` da subcategoria, índice de listagem), módulo `transaction` (domínio, 5 casos de uso, adaptador JPA com filtro por `owner_id` e moeda, ordenação RN-TXN-08, totais do conjunto filtrado, paginação), `ExpenseQuery` pública, `BudgetImpactPort` com implementação provisória, `TransactionCategoryUsageGuard` real (fecha as pendências da F4), `CategoryDirectory` pública em `category`, `FieldValidationException` e `Currency` em `shared`. `./mvnw clean verify` com Docker: 214 testes, 0 falhas. **Frontend** (spec 33): `/lancamentos` (filtro, subtotais, tabela/cartões, paginação, diálogo criar/editar, exclusão com confirmação, toasts de alerta já mapeados), `MoneyInputDirective` com decimal exato; build verde e 127 testes Vitest; verificado no navegador contra a API real. Postman, specs 15/34 e docs atualizados. Pendências: cálculo real do alerta e teste de concorrência no limiar → F6; agregados do dashboard → F7; teclado completo, 320/768 px e contraste → F12. |
| F6–F12 | Não iniciadas | — |

Fechamento de F1 e F2 (2026-10-27): critérios de conclusão verificados no que cabe a cada fatia (`mvnw clean verify` e build do frontend verdes, contratos validados, isolamento do `identity` testado, matriz de rastreabilidade atualizada, sem bloqueios abertos). Os critérios das specs que dependem de módulos ou páginas de fatias posteriores permanecem com ⏳ nas respectivas specs e **não** bloqueiam F3 em diante; ficam atribuídos assim:

| Pendência | Fatia que fecha |
|---|---|
| ~~Semeadura das 11 categorias; remoção do módulo `example`; V2 e correlatos~~ | feito na F4 (2026-10-29) |
| ~~Categorias: `CategoryUsageGuard` real (`CATEGORY_IN_USE`, `CATEGORY_TYPE_CONFLICT`), desassociação de subcategoria excluída, seletor de Lançamentos lendo `CategoriesStore`~~ | feito na F5 (2026-10-29) |
| Seletor de Metas lendo `CategoriesStore`; `BudgetImpactPort` real (limiares 80%/100%), alertas e teste de concorrência no limiar | F6 |
| Teste de isolamento entre utilizadores em cada módulo de dados (categorias: feito na F4); códigos de erro dos módulos; rollback das operações atómicas | F5–F10 (cada módulo na sua fatia) |
| `KpiCard`, `ProgressBar`, pickers dedicados, gráficos; formulários de cofrinho, movimentação e metas; invalidação de consultas entre páginas (`ConfirmDialog`, `Modal`, o formulário de categoria, `MoneyInput` e o formulário de lançamento entregues na F4/F5; consultas por mês/moeda entregues na lista de lançamentos) | F6–F9, na fatia que os usa |
| Preferências (moeda, tema) no servidor; limpeza de preferências de dispositivo no logout | F10 |
| Recuperação de senha (`password_reset_token`, e-mail, páginas) | F11 |
| CI, repositório remoto verificado; versionar as imagens Docker por tag (hoje `latest`) | F3 |
| Inspeção das imagens por segredos; Mailpit no `compose` | F12 e F11 |
| Teste automatizado de logs; `DOWN` do health sem banco; perfil `prod`; medições de desempenho; validação de ProblemDetail contra o OpenAPI; verificador de acessibilidade; percurso completo por teclado; 768 e 1280 px; imagens Docker e fallback SPA; máquina limpa; verificação de dependências (QUALITY-SPEC-05) | F12 |
| ~~Aprovação das cores escurecidas do tema claro (spec 25) e da moeda/tema como preferência de dispositivo até a F10~~ | aprovado pelo utilizador em 2026-10-27 (registado nas specs 25 e 27); resta medir o contraste |

### Alterações em relação à proposta anterior (aprovadas em 2026-10-25)

- Marcos M0–M10 substituídos por fatias F0–F12; correspondência anterior → atual: M1→F1, M2→F2, M3→F3, M4→F4, M5→F5, M6→F6+F7, M7→F8, M8→F9, M9→F10, M1b+M2b→F11, M10→F12.
- Backend e frontend base deixam de correr em paralelo (F1 antes de F2) para permitir progresso verificável passo a passo.
- Recuperação de senha (antes M1b/M2b, logo após M1) passa a F11, por não bloquear módulos financeiros.
- Metas e dashboard (antes M6) separados em F6 e F7.
- Testes unitários, de integração e de isolamento passam a ser escritos em cada fatia; F12 apenas consolida.

### Dependências entre specs

```mermaid
flowchart LR
    S01[01 scaffold ✔] --> S06[06 arquitetura]
    S06 --> S08[08 esquema] --> S11[11 identity] --> S15[15 categorias] --> S16[16 lançamentos] --> S18[18 metas] --> S17[17 dashboard]
    S11 --> S19[19 cofrinhos]
    S11 --> S22[22 preferências]
    S11 --> S48[48 recuperação de senha] --> S49[49 recuperação UI]
    S16 --> S20[20 relatórios]
    S19 --> S20
    S23[23 frontend base] --> S24[24 rotas] --> S29[29 auth UI] --> S34[34 categorias UI]
    S25[25 design system] --> S34
    S34 --> S33[33 lançamentos UI] --> S36[36 metas UI] --> S32[32 dashboard UI]
    S25 --> S35[35 cofrinhos UI]
    S33 --> S37[37 relatórios UI]
    S35 --> S37
    S21[21 backup ✖ cancelada]:::off
    S38[38 backup UI ✖ cancelada]:::off
    classDef off stroke-dasharray: 4 4,opacity:0.5
```

### Paralelismo seguro

- A execução padrão é em série (uma fatia de cada vez). O ponto de acoplamento entre backend e frontend é o contrato `identity` (F1 → F2).
- F8 (cofrinhos), F10 e F11 não dependem de F4–F7 e podem ser antecipadas ou, se o utilizador pedir, executadas em paralelo, por tocarem módulos distintos.
- Cada módulo é uma entrega curta e independente (contrato → backend → frontend), com spec e evidência próprias. Dois módulos nunca editam os mesmos ficheiros.

### Ordem dentro de cada módulo

1. Contrato (prévia aprovada pelo utilizador). 2. Migração. 3. Domínio e testes de domínio. 4. Casos de uso, adaptadores JPA com consultas otimizadas. 5. Controller. 6. Testes de isolamento entre utilizadores. 7. Frontend da página. 8. Validação de contrato e E2E parcial.

### Critérios de conclusão por fatia

Todos os critérios de aceitação das specs da fatia marcados com evidência; `mvnw clean verify` e build do frontend verdes; contratos validados; testes de isolamento passando; matriz de rastreabilidade atualizada; sem bloqueios abertos.

### Aderência ao fluxo SDD

O template exige uma spec curta por entrega: as specs desta análise são o **conjunto de entregas candidatas**, não uma única mudança. Qualquer uma pode ser dividida (por exemplo, `listTransactions` e `createTransaction`) mantendo a rastreabilidade (regra 9 de "Como executar").

### Riscos

- Esquecer o filtro de proprietário numa consulta (mitigado por filtro central e testes de isolamento).
- `openapi-rest-contract` exige aprovação interativa da prévia, o que impede execução totalmente automática.
- Entregabilidade do e-mail de recuperação depende do provedor e do domínio remetente, só definidos com a hospedagem (P-18).
- Sem cópia de segurança do banco por decisão do produto (P-27).

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [Índice geral](../00-engenharia-reversa/README.md)
- [Pendências](../00-engenharia-reversa/pendencias.md)

## Regras e critérios de aceitação

- [x] Nenhuma fatia começa sem as condições de entrada da tabela.
  > ✅ 2026-10-27 — F1 começou após a F0 (commits `3135bd8` e `7a03399`); F2 começou com o contrato `identity` da F1 aprovado e gerado. Verificação: histórico desta execução e `git log`.
- [x] A ordem respeita as dependências do diagrama.
  > ✅ 2026-10-27 — Executadas F0 → F1 → F2 em série, sem fatia fora de ordem. Verificação: comparação com o diagrama e a tabela de condições de entrada.
- [ ] Cada fatia termina com evidência registada conforme "Como executar".
  > ⏳ 2026-10-27 — Parcial: F1 e F2 com evidência nas specs 06, 08, 10, 11, 13, 23–30, 39–41 e na tabela "Estado das fatias". Falta: F3–F12.
- [ ] Alterações no plano (nova fatia, divisão de spec) mantêm a rastreabilidade na matriz.
  > ⏳ 2026-10-27 — Parcial: o escopo reduzido da F2 (componentes e formulários para F4–F10) está registado nesta spec e na matriz, secção "Estado da implementação por funcionalidade". Falta: manter nas próximas alterações.

### Skills de implementação

- **Skills obrigatórias:** as listadas em cada spec; esta spec não executa nenhuma.
- **Caminhos:** ver cada spec. Resumo: `java-modern-engineering`, `java-spring-create-module`, `openapi-rest-contract` em `C:\Users\dowgl\.codex\skills\`; `java-persistence-performance` em `.claude/skills/` (módulos com persistência e migrações Flyway); `angular-architect`, `angular-module-scaffold`, `angular-frontend-scaffold`, `angular-tailwind-ui-specialist` em `.claude/skills/`.
- **Responsabilidade:** ver catálogo no [índice](../00-engenharia-reversa/README.md#skills).
- **Momento de utilização:** conforme a fatia.
- **Dependências:** `openapi-rest-contract` antes de `java-spring-create-module` e `angular-module-scaffold` em cada módulo.
- **Limitações:** sem skills para banco, segurança, CI/CD, observabilidade, E2E e infraestrutura.

## Tasks

### Descoberta e decisão

- [x] Aprovar o plano.
  > ✅ 2026-10-25 — Sequência por fatias F0–F12 aprovada pelo utilizador em conversa. Verificação: decisão registada nesta spec e em [roadmap](../../shared/roadmap.md).
- [x] Criar o repositório do projeto e o primeiro commit (manual — P-23). A renomeação do backend (spec 01) já foi feita.
  > ✅ 2026-10-27 — Repositório Git com commits `3135bd8` (first commit) e `7a03399` (README) na `main`. Verificação: `git log`. Repositório remoto ainda não verificado (condição de entrada da F3).

### Validação

- [ ] Rever o plano ao fim de cada fatia e registar ajustes.
  > ⏳ 2026-10-27 — Parcial: plano revisto após a F2 (tabela "Estado das fatias"; ajuste: escopo reduzido da F2, itens transferidos para F4–F10). Falta: rever ao fim das próximas fatias. Atualização no mesmo dia: F1 e F2 verificadas com Docker e API real. Próxima ação: fechar formalmente F1 e F2 (matriz de rastreabilidade e pendências listadas) e iniciar a F3 ou F4.

## Resultado esperado

- Sequência de entrega clara, com dependências visíveis antes de qualquer implementação.
- Preservado o princípio de especificar e aprovar antes de implementar.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
