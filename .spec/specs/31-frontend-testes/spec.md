# 31-frontend-testes

> **ID:** FRONTEND-SPEC-08 · **Categoria:** Frontend · **Estado:** proposta para revisão — não aprovada, não executada.

## Objetivo

Definir os testes do frontend e a decisão de ferramenta, já que o scaffold não cria testes e a origem não tinha testes de interface.

## Escopo

- Inclui: o que testar por camada, cenários mínimos e a decisão do executor.
- Não inclui: testes de backend, E2E completos (QUALITY-SPEC-04).

## Contexto técnico local

### Ponto de partida

- O scaffold Angular **não inclui arquivos nem comando de teste** (skill `angular-frontend-scaffold`); o executor a introduzir é o padrão do Angular 21 no momento (P-17, delegada), configurado depois do scaffold.
- A origem testa apenas lógica pura; nenhum teste de widget (DESC-TST-01).

### O que testar (PROPOSTO)

| Camada | Cenários |
|---|---|
| Utilitários | formatação `Intl` (`R$ 1.234,56`, `€ 1.234,56`, `+R$`/`-R$`); máscara de valor (`123456` → `1.234,56`; negativos impossíveis); conversão para string decimal; formato de datas e mês |
| Validadores | as cinco mensagens da origem; valor `0`, `-1`, vazio, `abc`, `100,50`, `1.234,56` |
| Estado | valores padrão sem `localStorage`; moeda e tema vindos do servidor; troca de moeda ao salvar lançamento noutra moeda; sessão só em memória e limpa ao sair; cancelamento ao trocar mês; invalidação por mutação |
| Autenticação | guard redireciona sem sessão; interceptor renova em 401 e repete uma vez; falha de renovação leva a `/entrar`; token ausente de `localStorage` |
| Dados | mapeamento de `ApiError` por `code`; falha de rede não vira lista vazia |
| Componentes | `KpiCard`, `MoneyInput`, `ConfirmDialog`, `MonthSwitcher` (volta ao atual), `CurrencySelector`, paginação (`1–15 de N`) |
| Páginas | estados carregando/vazio/erro/sucesso de cada página; filtro de tipo; trocar tipo limpa categoria; moeda desabilitada em cofrinho com movimentações; confirmação antes de excluir |
| Acessibilidade | foco em diálogos, `aria-live`, navegação por teclado no menu |

### Decisão

Usar o executor padrão do Angular 21 e, se necessário, uma biblioteca de testes de componente; adicionar apenas o necessário (a skill de arquitetura exige "sem frameworks sem necessidade") e registar a escolha final em ADR.

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [QUALITY-SPEC-00](../44-quality-estrategia/spec.md)
- [FRONTEND-SPEC-00](../23-frontend-arquitetura-angular/spec.md)

## Regras e critérios de aceitação

- [ ] O executor de testes está definido e `npm test` (ou equivalente) roda sem navegador interativo.
- [ ] Todos os cenários da tabela têm teste que passa e é rastreável à regra.
- [ ] Nenhum teste usa dados financeiros reais.
- [ ] A cobertura de utilitários e validadores é 100% das ramificações listadas.
- [ ] O build de produção compila templates em modo estrito (`tsc` isolado não basta).

### Skills de implementação

- **Skills obrigatórias:** `angular-architect`, `angular-module-scaffold`.
- **Caminhos:** `.claude/skills/angular-architect/SKILL.md`; `.claude/skills/angular-module-scaffold/SKILL.md`.
- **Responsabilidade:** testes proporcionais ao comportamento alterado; verificação de templates por build.
- **Momento de utilização:** com cada página/módulo.
- **Dependências:** FRONTEND-SPEC-00.
- **Limitações:** **sem skill dedicada a testes** nos diretórios indicados; `angular-frontend-scaffold` não instala testes.

## Tasks

### Front-end

- [ ] Configurar o executor e escrever os testes por camada.

### Validação

- [ ] Executar a suíte e o build de produção; registar contagens e resultado.

## Resultado esperado

- Suíte frontend que protege formatação, validação, estado e estados de tela.
- Preservado o princípio de testar comportamento, não duplicar a implementação.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
