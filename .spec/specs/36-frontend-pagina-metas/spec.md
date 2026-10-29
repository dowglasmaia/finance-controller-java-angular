# 36-frontend-pagina-metas

> **ID:** FRONTEND-SPEC-13 · **Categoria:** Frontend (`features/budget`) · **Estado:** executada na F6 (2026-10-29); a verificação manual completa fica pendente — ver evidências.

## Objetivo

Construir a página de meta de gastos: progresso do mês, meta padrão recorrente, ajuste do mês e metas por categoria, com gravação conjunta.

## Escopo

- Inclui: página `/metas`, progresso global, campos de meta, linhas por categoria, gravação.
- Não inclui: cálculo de estado e alerta (backend); meta geral de reservas (FRONTEND-SPEC-12).

## Contexto técnico local

### Página (CONFIRMADO — DESC-BUD-09/10)

- Título `Meta de Gastos`; `MonthSwitcher`; seletor de moeda compacto.
- Cartão de progresso (se há limite global): ícone, `Meta de {Mês Ano}`, `N%`, barra colorida (limitada a 100% na barra) e texto `{gasto} de {limite}`; rótulos `Dentro da meta`, `Perto da meta`, `Meta estourada`, `Sem meta definida` com ícone e cor.
- `Meta mensal padrão` — ajuda: "Teto recorrente aplicado a todo mês. Deixe vazio para não usar meta global." Campo `Meta padrão`.
- `Ajuste para {Mês Ano}` — ajuda: "Opcional: um teto específico para este mês (sobrepõe a meta padrão)." Campo `Meta deste mês`.
- `Metas por categoria` — ajuda: "Opcional: teto recorrente por categoria. Gasto exibido é o do mês selecionado." Uma linha por categoria que aceita despesa: ícone, nome, `{gasto} de {limite}` colorido pelo estado (ou `Gasto: {gasto}`), campo de valor.
- Botão `Salvar meta` grava tudo de uma vez; toast `Meta salva.`; vazio nos campos = remover.
- Mudar o mês recarrega o ajuste; mudar a moeda recarrega todos os campos (DESC-BUD-09).

### Operações consumidas

`getBudget`, `saveBudget`, `getBudgetProgress`, `listCategories` (tipo despesa).

### Pendências

P-06 (decidida, flexível): o campo "Meta deste mês" aceita um valor, e uma caixa `Sem meta neste mês` desativa a meta só naquele mês sem mexer na padrão; vazio e sem a caixa remove o ajuste. P-14 (decidida): o progresso também aparece no dashboard (FRONTEND-SPEC-09). A documentação da origem diz "mês atual", mas o código usa o mês de referência (DESC-BUD-11): vale o mês de referência.

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [BACKEND-SPEC-13](../18-backend-modulo-metas-de-gastos/spec.md)
- [FRONTEND-SPEC-05](../28-frontend-formularios-e-validacoes/spec.md)

## Regras e critérios de aceitação

- [x] Com meta 1000: gasto 700 mostra `Dentro da meta`; 800 e 950 `Perto da meta`; 1000 ou mais `Meta estourada`; sem meta `Sem meta definida`.
  > ✅ 2026-10-29 11:55 +00:00 — `budget-page.spec.ts` (estados vindos do servidor: `OK` 70% `Dentro da meta`; `WARNING` 80% e 95% `Perto da meta`; `EXCEEDED` 100% e 120% `Meta estourada`; sem meta global não há cartão e a categoria mostra `Gasto: {valor}` / `Sem meta definida`). A decisão do estado é do servidor (testada no backend).
- [x] Salvar com campos vazios remove os limites correspondentes.
  > ✅ 2026-10-29 11:55 +00:00 — `budget-page.spec.ts` (campos vazios não vão no pedido: sem `monthlyLimit`, sem `monthOverride`, `categoryLimits` vazio) e backend `savingWithoutOverrideRemovesTheMonthAdjustmentAndZeroRemovesLimits`.
- [x] Marcar `Sem meta neste mês` em julho com meta padrão 1000 mostra `Sem meta definida` em julho e continua `1000` em agosto.
  > ✅ 2026-10-29 11:55 +00:00 — `budget-page.spec.ts` (a caixa envia `monthOverride: {disabled: true}`, desativa o campo e, vindo do servidor, aparece marcada) e backend `disabledMonthHasNoGoal…` (julho `NONE`, agosto continua 1000,00).
- [x] Trocar o mês mostra o ajuste daquele mês; trocar a moeda mostra a configuração da outra moeda.
  > ✅ 2026-10-29 11:55 +00:00 — `budget-page.spec.ts` (mês seguinte consulta o novo `month` e preenche o ajuste daquele mês; moeda EUR consulta `/budgets/EUR` e mostra a configuração dela, descartando o que estava digitado).
- [x] Os campos não aceitam valores negativos.
  > ✅ 2026-10-29 11:55 +00:00 — `MoneyInputDirective` descarta qualquer caractere que não seja dígito ou separador (`money-input.spec.ts`: `-5` → `5`); o servidor também recusa negativos (400).
- [x] A gravação é única; em falha nenhum campo é parcialmente salvo e os valores digitados permanecem.
  > ✅ 2026-10-29 11:55 +00:00 — `budget-page.spec.ts` (um só `PUT`; em 409 os valores digitados permanecem e nenhuma consulta de recarga é feita) e backend `invalidSaveFailsCompletely…` (atomicidade).
- [x] Cada estado usa ícone ou texto além da cor.
  > ✅ 2026-10-29 11:55 +00:00 — `budget-page.spec.ts` (cada estado do cartão traz rótulo, ícone SVG e `role="progressbar"` com `aria-valuenow`; a linha da categoria traz o rótulo para leitor de tela).

### Skills de implementação

- **Skills obrigatórias:** `angular-module-scaffold`, `angular-tailwind-ui-specialist`, `angular-architect`.
- **Caminhos:** `.claude/skills/angular-module-scaffold/SKILL.md`; `.claude/skills/angular-tailwind-ui-specialist/SKILL.md`; `.claude/skills/angular-architect/SKILL.md`.
- **Responsabilidade:** módulo e `doc.md`; formulário em lista e barras; estado dependente de mês/moeda.
- **Momento de utilização:** depois do contrato `budgets` e de `categories`.
- **Dependências:** FRONTEND-SPEC-00…05; BACKEND-SPEC-13.
- **Limitações:** nenhuma específica.

## Tasks

### Front-end

- [x] Criar `features/budget` com página, linha de categoria, serviço de dados e `doc.md`.
  > ✅ 2026-10-29 11:55 +00:00 — `features/budget` (`budget.routes.ts`, `pages/`, `ui/`, `data-access/`, `budget-status.ts`, `doc.md`); tipos gerados em `core/api/generated/budgets.ts`. Build de produção verde; rota `/metas` sob demanda no lugar do placeholder.

### Validação

- [x] Testar os estados, o recarregamento por mês/moeda e a gravação única.
  > ✅ 2026-10-29 11:55 +00:00 — `npm test`: 142 testes Vitest verdes (15 novos em `budget-page.spec.ts`). Navegador contra API real (desktop): meta padrão 1000 e Lazer 200 salvas (`Meta salva.`), cartão `0%` / `Dentro da meta`, despesa de 850 com o toast `Você já usou 80% da sua meta mensal.` e progresso `85%` / `Perto da meta`. Falta: teclado completo, 320/768 px e contraste nos dois temas.

## Resultado esperado

- Definição de metas equivalente à origem, com progresso preciso vindo do servidor.
- Preservadas as regras de recorrência, ajuste mensal e vazio-remove.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
