# angular-tailwind-ui-specialist — como usar com uma IA

Skill de **interface**: templates Angular com Tailwind CSS, hierarquia visual, tokens, layout responsivo, formulários, estados e acessibilidade. Cuida só da camada de apresentação. As instruções que a IA segue estão em [SKILL.md](SKILL.md), com [tokens Neon Graphite](references/neon-graphite-tokens.md) e [checklist de revisão de UI](references/ui-review-checklist.md).

## 1. Para que serve

| Faz | Não faz |
|---|---|
| Constrói/ajusta templates com Tailwind, mobile-first (320px até desktop) | Regras de negócio, cálculos financeiros ou chamadas HTTP |
| Aplica a identidade **Neon Graphite**, tema claro/escuro e tokens semânticos (`bg-surface`, `text-income`, `text-expense`) | Desenhar a API ou o contrato |
| Garante acessibilidade: rótulos, foco visível, contraste, `aria-*`, alvos de ~44px | Criar um projeto Angular (use `angular-frontend-scaffold`) |
| Trata estados de carregamento, vazio, erro, sucesso e validação | Instalar UI kit ou biblioteca de componentes |
| Revisa UI existente com o checklist | Definir estado da aplicação (use `angular-module-scaffold`) |

## 2. Pré-requisitos

- Projeto Angular com Tailwind configurado (`.postcssrc.json` e `@use 'tailwindcss';` em `src/styles.scss`), como entrega o `angular-frontend-scaffold`.
- Leitura de `.spec/memory/produto.md` e `.spec/memory/contexto-tecnico.md` (a IA faz isso).

## 3. Dados a informar

| Informação | Obrigatória | Observação |
|---|---|---|
| Tela ou componente alvo | **Sim** | Caminho ou nome da página |
| Tarefa do usuário e ação principal | Recomendado | Ex.: "registrar um lançamento em poucos toques" |
| Estados a cobrir | Opcional | Carregamento, vazio, erro, validação |
| Restrições | Opcional | Ex.: "só mobile", "sem novas cores" |

## 4. Como acionar

### Exemplos de pedido

Criar:

```text
Use a skill angular-tailwind-ui-specialist para montar o template da página de categorias
(app-finance-controller/src/app/features/categorias/pages). Lista com ação "Nova categoria",
estados de carregamento, vazio e erro, tema claro e escuro. Não altere a lógica.
```

Revisar:

```text
Use a skill angular-tailwind-ui-specialist para revisar a página de lançamentos com o
checklist de UI: responsividade em 320px, foco, contraste e estados. Liste os problemas.
```

### Claude Code / Codex / outras

Claude Code: `/angular-tailwind-ui-specialist` + pedido. Codex: `$angular-tailwind-ui-specialist`. Sem suporte a skills: "Leia `.agents/skills/angular-tailwind-ui-specialist/SKILL.md` e siga as instruções para: ...".

## 5. Como a IA trabalha

1. Inspeciona a feature, estilos globais, tokens e componentes reutilizáveis; reutiliza padrões existentes.
2. Define a tarefa do usuário, a ação principal e a hierarquia do conteúdo.
3. Escreve HTML semântico primeiro (títulos, labels, botões nativos, tabelas, landmarks), depois classes Tailwind mobile-first.
4. Usa SCSS de componente só para o que utilitários não resolvem.
5. Verifica estados e larguras do checklist e roda o build/check quando pedido.

## 6. Regras principais

- Tokens semânticos em vez de cores literais; nada de `@apply` para esconder utilitários avulsos.
- Receita/despesa/alerta sempre com texto ou ícone, nunca só cor.
- Tabelas utilizáveis no celular: rolagem horizontal visível ou linhas como cartões.
- Respeitar `prefers-reduced-motion`; hover nunca é o único jeito de revelar ação.
- Templates declarativos: `@if`, `@for` (com `track`) e `@switch` para estados.

## 7. Erros comuns

| Sintoma | Causa e solução |
|---|---|
| A IA recusa mexer em regra/cálculo | Esperado: fora do escopo; use `angular-module-scaffold`/`angular-architect` |
| Cores fora do padrão | Peça para usar os tokens de [neon-graphite-tokens.md](references/neon-graphite-tokens.md) |
| Classes Tailwind sem efeito | Confirme `@use 'tailwindcss';` em `styles.scss` e `.postcssrc.json` |
