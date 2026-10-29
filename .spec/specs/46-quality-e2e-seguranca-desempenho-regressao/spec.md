# 46-quality-e2e-seguranca-desempenho-regressao

> **IDs:** QUALITY-SPEC-04 (ponta a ponta), QUALITY-SPEC-05 (segurança, desempenho e regressão) · **Categoria:** Qualidade · **Estado:** executada na F12 (2026-10-29); execução no GitHub Actions pendente.

## Objetivo

Definir as jornadas de ponta a ponta que provam a paridade funcional com a origem e as verificações de segurança, desempenho e regressão.

## Escopo

- Inclui: jornadas E2E, verificações de segurança, cargas de desempenho, lista de regressões.
- Não inclui: ferramentas definitivas (recomendação: Playwright para E2E; escolha final registada na implementação).

## Contexto técnico local

### Jornadas E2E (QUALITY-SPEC-04, sobre o conjunto no `compose`)

| ID | Jornada | Resultado verificável | Origem |
|---|---|---|---|
| J-00 | Cadastro e entrada | cadastrar com e-mail e senha, sair, entrar de novo; rota de dados sem sessão leva a `/entrar` | P-01 |
| J-00b | Recuperação de senha | pedir o link (com SMTP de captura), abrir o link, definir nova senha, entrar com ela; a senha antiga e o link usado deixam de funcionar; o pedido para e-mail inexistente dá a mesma resposta | P-24 |
| J-01 | Primeiro acesso de um utilizador novo | 11 categorias (7 despesa, 4 receita) com subcategorias e "Outros" | DESC-SEE-01 |
| J-02 | Criar despesa em BRL e entrada em EUR | cada uma só aparece na sua moeda; saldos separados | DESC-CUR-02 |
| J-03 | Navegar meses | dashboard e lançamentos mudam com o mês; lançamento futuro aparece no mês certo | DESC-MES-01/02 |
| J-04 | Meta com alerta | com meta 1000, gastos 700→850 mostram aviso; →1100 mostram estouro; nova despesa não repete | DESC-BUD-06/07 |
| J-05 | Meta por categoria prioriza global | só o alerta da categoria aparece | DESC-BUD-07 |
| J-06 | Categoria com lançamentos | exclusão bloqueada com a mensagem; sem lançamentos, exclui com subcategorias | DESC-CAT-03 |
| J-07 | Cofrinho completo | criar, depositar 1000 e 500, sacar 300 → saldo 1200; sacar 1300 é recusado; com saldo 0 o saque fica indisponível; moeda bloqueada; meta geral independente | DESC-BOX-03/05/09; P-04 |
| J-08 | Relatórios | Semana/Mês/Ano coerentes com os totais; ano mostra resumo mensal; mês de referência ≠ atual coerente | DESC-REP-03/06 |
| J-09 | Isolamento entre utilizadores | o utilizador B não vê categorias, lançamentos, metas, cofrinhos nem totais do A | P-01 |
| J-10 | Metas flexíveis e dashboard | "Sem meta neste mês" desativa só aquele mês; o dashboard mostra o progresso da meta | P-06, P-14 |
| J-11 | Preferências | tema e moeda persistem ao recarregar e noutro dispositivo; mês volta ao atual | DESC-PRF-01, DESC-MES-01; P-19 |
| J-12 | Responsividade e teclado | fluxo J-02 completo em 320 px e só por teclado | FRONTEND-SPEC-07 |

Cada jornada é determinística, com dados sintéticos e relógio fixo. Ferramenta de E2E: P-17.

### Segurança (QUALITY-SPEC-05)

| Verificação | Alvo |
|---|---|
| Dependências | análise de vulnerabilidades em Maven e npm no CI |
| Segredos | varredura de segredos no repositório e nas imagens |
| Entrada | valores extremos, strings longas, JSON malformado, corpo acima do teto, caracteres de controle |
| Cabeçalhos e CORS | cabeçalhos de segurança; origem não autorizada recusada |
| Erros | nenhum 4xx/5xx expõe pilha ou SQL |
| Acesso | rotas de dados exigem sessão; isolamento entre utilizadores; força bruta limitada; *refresh token* rotativo; senha nunca em logs nem respostas |
| Logs | nenhum valor financeiro registado |

### Desempenho

Conjunto sintético de 50 000 lançamentos distribuídos por 5 anos e duas moedas: medir `listTransactions`, `getMonthlyOverview` e `getReport(YEAR)`, com um segundo utilizador de volume semelhante para verificar o isolamento sem perda de desempenho; meta inicial p95 < 500 ms nas três consultas (GLOBAL-SPEC-01), a confirmar. Frontend: lista paginada e gráficos sem bloqueio perceptível.

### Regressão

Lista permanente de testes para defeitos e decisões conhecidas: DESC-REP-03 (barras × totais), DESC-CAT-06, DESC-SUB-02/03, DESC-BOX-06 (saldo nunca negativo)/07, DESC-CUR-04 (troca de moeda ao salvar), DESC-BUD-08 (sem alerta fora do mês de referência), isolamento entre utilizadores.

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [QUALITY-SPEC-00](../44-quality-estrategia/spec.md)
- [INFRA-SPEC-06](../41-infra-build-e-execucao/spec.md)

## Regras e critérios de aceitação

- [x] J-00…J-12 passam em ambiente limpo, com dados sintéticos e relógio fixo.
  > ✅ 2026-10-29 16:20 +00:00 — Playwright 1.64 (`e2e/`): 33 testes (13 jornadas J-00, J-00b e J-01…J-12 mais 18 varreduras de acessibilidade) passam contra o `compose` em um projeto isolado (`-p fc-e2e`: PostgreSQL, API e nginx em imagens, Mailpit), em paralelo e também com `--workers=1`; banco novo, utilizadores sintéticos criados pelo próprio teste e relógio do navegador fixo em 15/06/2026. J-12: fluxo de criar despesa em 320 px só por teclado (Tab/Enter/digitação, foco visível, foco devolvido ao fechar, sem rolagem horizontal).
- [x] Nenhuma vulnerabilidade crítica aberta; nenhum segredo detetado.
  > ✅ 2026-10-29 16:20 +00:00 — Achados e correções: Trivy nas imagens → 4 CRITICAL do Tomcat 11.0.24 e 5 HIGH do Jackson 3.1.5 (corrigidos fixando `tomcat.version=11.0.25` e `jackson-bom.version=3.1.7` no `pom.xml`), 1 CRITICAL de `brotli-libs` na imagem do nginx (`apk upgrade` no Dockerfile); OSV-Scanner → vitest 4.0.8 (CRITICAL 9.8, só dependência de desenvolvimento), `@vitest/mocker` e `undici` (corrigidos com vitest 4.1.12 e `npm audit fix`). Depois: Trivy 0 CRITICAL e 0 segredos (1 MEDIUM sem relevância), OSV-Scanner `No issues found`, `npm audit` 0, gitleaks 0 (árvore e histórico). Limite: o OSV-Scanner lê só as dependências diretas do `pom.xml`; as transitivas do Maven são cobertas pelo Trivy na imagem.
- [x] As metas de desempenho são medidas e registadas com o volume sintético; resultados fora da meta geram decisão registada.
  > ✅ 2026-10-29 16:20 +00:00 — `PerformanceIntegrationTest` (PostgreSQL real, MockMvc em processo, 100 000 lançamentos = 50 000 por utilizador em 5 anos e 2 moedas, 40 amostras após aquecimento): p95 de `listTransactions` 13 ms (meta 300), `getMonthlyOverview` 23 ms (meta 500), `getReport(YEAR)` 22 ms (meta 800), `getReport(MONTH)` 11 ms (meta 500). O volume do vizinho não entra nos totais (`theTotalsOfTheMeasuredUserNeverIncludeTheNeighboursVolume`). Ressalva: sem rede nem HTTP real; o que importa aqui é o SQL e o isolamento.
- [x] Cada item da lista de regressão tem teste automatizado.
  > ✅ 2026-10-29 16:20 +00:00 — DESC-REP-03 (`monthBarsAddUpToTheTotals…` e J-08); DESC-CAT-06 (`CATEGORY_TYPE_CONFLICT`, fluxos de categoria e lançamento); DESC-SUB-02/03 (`duplicateNamesAreRejectedIgnoringCaseAndSpaces`, `createAlwaysHasExactlyOneProtectedOthers`, `subcategoriesCanBeAddedAndRemovedButOthersIsProtected`); DESC-BOX-06 (saldo nunca negativo, vários testes e J-07) e DESC-BOX-07 (meta ≥ 0: `invalidInputsAre400`, `DomainGuardTests`); DESC-CUR-04 (diálogo de lançamento e J-02); DESC-BUD-08 (`noAlertForIncomeForExpensesOutsideTheReferenceMonth…`); isolamento (um por módulo e J-09).
- [x] Os testes E2E não dependem de dados reais nem da ordem de execução.
  > ✅ 2026-10-29 16:20 +00:00 — Cada jornada cria o seu utilizador (`createUser`) e os seus dados pela API; rodou em paralelo (3 workers) e em série (`--workers=1`) com 33 de 33 verdes.

### Skills de implementação

- **Skills obrigatórias:** nenhuma específica confirmada para E2E, segurança ou desempenho.
- **Caminhos:** —
- **Responsabilidade:** —
- **Momento de utilização:** —
- **Dependências:** INFRA-SPEC-06; módulos concluídos; BACKEND-SPEC-05.
- **Limitações:** **sem skills de E2E, segurança ou desempenho nos diretórios indicados.** Para a UI, a lista de revisão de `angular-tailwind-ui-specialist` apoia a jornada J-12 (`.claude/skills/angular-tailwind-ui-specialist/references/ui-review-checklist.md`).

## Tasks

### Descoberta e decisão

- [x] Registar a escolha final das ferramentas de E2E, segurança e carga.
  > ✅ 2026-10-29 16:20 +00:00 — E2E e acessibilidade: Playwright + `@axe-core/playwright` (aprovados em 2026-10-29). Segurança: gitleaks, OSV-Scanner, `npm audit`, Trivy (imagens Docker; rodam também no CI) e testes `SecurityHardeningIntegrationTest`. Carga: teste de integração com `generate_series` e medição de p95 (sem k6/JMeter: o alvo era o SQL e o isolamento, não a rede). Contrato: sem biblioteca extra + oasdiff no CI.

### Validação

- [x] Implementar e executar J-00…J-12; medir o desempenho e registar a evidência.
  > ✅ 2026-10-29 16:20 +00:00 — `e2e/tests/auth.spec.ts` (J-00, J-00b, J-09, J-11), `money.spec.ts` (J-01…J-06, J-10), `boxes-reports.spec.ts` (J-07, J-08, J-12) e `a11y.spec.ts`; resultados e desempenho acima. Acessibilidade: axe-core com regras WCAG 2.0/2.1 A e AA (inclui contraste) em todas as páginas e diálogos, nos temas escuro e claro, em 320, 768 e 1280 px, sem nenhuma violação depois das correções (ver spec 47).

## Resultado esperado

- Paridade funcional demonstrada de ponta a ponta e risco de segurança e desempenho medido.
- Preservado: isolamento por moeda, importação segura e identidade das regras financeiras.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
