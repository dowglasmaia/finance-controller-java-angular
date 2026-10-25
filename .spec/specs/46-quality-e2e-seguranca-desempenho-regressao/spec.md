# 46-quality-e2e-seguranca-desempenho-regressao

> **IDs:** QUALITY-SPEC-04 (ponta a ponta), QUALITY-SPEC-05 (segurança, desempenho e regressão) · **Categoria:** Qualidade · **Estado:** proposta para revisão — não aprovada, não executada.

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

- [ ] J-00…J-12 passam em ambiente limpo, com dados sintéticos e relógio fixo.
- [ ] Nenhuma vulnerabilidade crítica aberta; nenhum segredo detetado.
- [ ] As metas de desempenho são medidas e registadas com o volume sintético; resultados fora da meta geram decisão registada.
- [ ] Cada item da lista de regressão tem teste automatizado.
- [ ] Os testes E2E não dependem de dados reais nem da ordem de execução.

### Skills de implementação

- **Skills obrigatórias:** nenhuma específica confirmada para E2E, segurança ou desempenho.
- **Caminhos:** —
- **Responsabilidade:** —
- **Momento de utilização:** —
- **Dependências:** INFRA-SPEC-06; módulos concluídos; BACKEND-SPEC-05.
- **Limitações:** **sem skills de E2E, segurança ou desempenho nos diretórios indicados.** Para a UI, a lista de revisão de `angular-tailwind-ui-specialist` apoia a jornada J-12 (`.claude/skills/angular-tailwind-ui-specialist/references/ui-review-checklist.md`).

## Tasks

### Descoberta e decisão

- [ ] Registar a escolha final das ferramentas de E2E, segurança e carga.

### Validação

- [ ] Implementar e executar J-00…J-12; medir o desempenho e registar a evidência.

## Resultado esperado

- Paridade funcional demonstrada de ponta a ponta e risco de segurança e desempenho medido.
- Preservado: isolamento por moeda, importação segura e identidade das regras financeiras.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
