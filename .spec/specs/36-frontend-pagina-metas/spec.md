# 36-frontend-pagina-metas

> **ID:** FRONTEND-SPEC-13 · **Categoria:** Frontend (`features/budget`) · **Estado:** proposta para revisão — não aprovada, não executada.

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

- [ ] Com meta 1000: gasto 700 mostra `Dentro da meta`; 800 e 950 `Perto da meta`; 1000 ou mais `Meta estourada`; sem meta `Sem meta definida`.
- [ ] Salvar com campos vazios remove os limites correspondentes.
- [ ] Marcar `Sem meta neste mês` em julho com meta padrão 1000 mostra `Sem meta definida` em julho e continua `1000` em agosto.
- [ ] Trocar o mês mostra o ajuste daquele mês; trocar a moeda mostra a configuração da outra moeda.
- [ ] Os campos não aceitam valores negativos.
- [ ] A gravação é única; em falha nenhum campo é parcialmente salvo e os valores digitados permanecem.
- [ ] Cada estado usa ícone ou texto além da cor.

### Skills de implementação

- **Skills obrigatórias:** `angular-module-scaffold`, `angular-tailwind-ui-specialist`, `angular-architect`.
- **Caminhos:** `.claude/skills/angular-module-scaffold/SKILL.md`; `.claude/skills/angular-tailwind-ui-specialist/SKILL.md`; `.claude/skills/angular-architect/SKILL.md`.
- **Responsabilidade:** módulo e `doc.md`; formulário em lista e barras; estado dependente de mês/moeda.
- **Momento de utilização:** depois do contrato `budgets` e de `categories`.
- **Dependências:** FRONTEND-SPEC-00…05; BACKEND-SPEC-13.
- **Limitações:** nenhuma específica.

## Tasks

### Front-end

- [ ] Criar `features/budget` com página, linha de categoria, serviço de dados e `doc.md`.

### Validação

- [ ] Testar os estados, o recarregamento por mês/moeda e a gravação única.

## Resultado esperado

- Definição de metas equivalente à origem, com progresso preciso vindo do servidor.
- Preservadas as regras de recorrência, ajuste mensal e vazio-remove.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
