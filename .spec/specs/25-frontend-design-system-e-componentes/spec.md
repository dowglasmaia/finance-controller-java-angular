# 25-frontend-design-system-e-componentes

> **ID:** FRONTEND-SPEC-02 · **Categoria:** Frontend · **Estado:** proposta para revisão — não aprovada, não executada.

## Objetivo

Definir os tokens visuais (Grafite Ciano, evolução do Neon Graphite) e os componentes compartilhados reais da origem, para que as páginas sejam compostas de forma consistente em tema claro e escuro.

## Escopo

- Inclui: tokens de cor, tipografia e espaçamento; inventário de componentes compartilhados com comportamento; formatação.
- Não inclui: páginas, regras financeiras, instalação de UI kits.

## Contexto técnico local

### Tokens (DESC-UX-02) — paleta "Grafite Ciano"

**Decisão (escolhida pelo utilizador em 2026-10-27, opção A):** a identidade passa a ser **Grafite Ciano**, evolução do Neon Graphite original: mantém grafite e ciano, com acento mais contido, superfícies em quatro níveis e cores semânticas escurecidas no tema claro. Substitui a tabela original da origem Flutter (`income #34D399`, `expense #FB7185`, `warning #FBBF24`, `accent #22D3EE`, `surface #F1F5F9`/`#0B1120`, cards `#131C30`/`#24304A`) e a decisão anterior de escurecer só quatro cores. **O tema escuro é o padrão** em sessão nova; o claro é uma paleta própria, não uma inversão.

| Token (Tailwind) | Escuro (padrão) | Claro | Uso |
|---|---|---|---|
| `surface` | `#0A0F1A` | `#F4F6FA` | fundo da aplicação |
| `brand` | `#0D1424` | `#0F172A` | menu lateral (`on-brand` `#E6EDF7`/`#F8FAFC`, `on-brand-muted` `#AAB6CB`) |
| `surface-raised` | `#121A2B` | `#FFFFFF` | cartões, painéis e cabeçalho |
| `surface-sunken` | `#0C1322` | `#EEF1F6` | campos de formulário, áreas rebaixadas e *hover* |
| `surface-overlay` | `#172136` | `#FFFFFF` | menus, popovers, modais e notificações |
| `line` | `#1F2A40` | `#D5DBE6` | bordas de cartões e divisórias |
| `line-input` | `#5B6B8A` | `#7B8AA3` | bordas de campos e botões secundários (≥ 3:1) |
| `content` / `content-muted` | `#E6EDF7` / `#9AA8BF` | `#0F172A` / `#4B5A70` | texto primário e secundário |
| `accent` / `on-accent` | `#38BDF8` / `#06121F` | `#0369A1` / `#FFFFFF` | ação principal, foco, seleção |
| `accent-on-brand` | `#38BDF8` | `#38BDF8` | item ativo no menu lateral |
| `income` | `#34D399` | `#047857` | entradas e "dentro da meta" |
| `expense` | `#FB7185` | `#BE123C` | saídas, erro e "estourada" |
| `warning` | `#FBBF24` | `#92400E` | "perto da meta" |

Regras: `income`, `expense` e `warning` só têm significado financeiro (nunca decoração) e vêm sempre com ícone ou texto; fundos suaves de estado usam o modificador de opacidade (`bg-expense/10`), sem tokens extras. Cores literais nunca aparecem em templates. Fonte Inter (com *fallback* do sistema; sem carregar fonte externa). Espaçamento em escala de 4 px.

**Contraste medido (WCAG 2.x, 2026-10-27):** texto/fundo 16,3:1 (escuro) e 16,5:1 (claro); `content-muted` sobre cartão 7,2:1 e 7,0:1; `accent` sobre cartão 8,1:1 e 5,9:1; `on-accent` sobre `accent` 8,8:1 e 5,9:1; receita 9,0:1 e 5,5:1; despesa 6,5:1 e 6,3:1; aviso 10,4:1 e 7,1:1; `line-input` sobre o campo rebaixado 3,1:1 (claro) e 3,5:1 (escuro), acima dos 3:1 exigidos para limites de controles; foco `accent` sobre o fundo 5,5:1 e 8,9:1. Menor valor de texto: 5,5:1 (AA, ≥ 4,5:1).

### Camadas, raios, sombras e movimento

- **Profundidade**: fundo → menu (`brand`) → cartão (`surface-raised`) → campo rebaixado (`surface-sunken`) → sobreposição (`surface-overlay`). No escuro a elevação vem da diferença de superfície e borda (`shadow-card` é nenhuma); no claro, de uma sombra curta. `shadow-overlay` só em menus, gavetas e notificações.
- **Raios**: `rounded-control` (8 px: botões, campos, menus) e `rounded-card` (12 px: cartões e painéis). Sem outros raios.
- **Movimento**: `duration-fast` (150 ms) com `ease-standard`; respeita `prefers-reduced-motion`.
- **Camadas z**: cabeçalho 20, menu fixo 30, gaveta 40, notificações 50, atalho "Ir para o conteúdo" 60.

### Primitivas de interface

- `appButton` (`ButtonDirective`, `<button>` ou `<a>`): `primary` (uma ação principal por contexto), `secondary` e `ghost`; altura mínima 44 px, estados *hover*, *focus-visible* e *disabled*.
- `appFieldControl` (dentro de `app-form-field`): estilo único de `input`/`select`/`textarea` (campo rebaixado, borda `line-input`, `aria-invalid` com borda e anel `expense`, placeholder em `content-muted`, desabilitado). As páginas não repetem classes de campo.
- Mensagem de erro de formulário: caixa com borda e fundo suave `expense`, sempre com texto.

### Diretrizes do Dashboard (F7)

O Dashboard só mostra dados que existem na spec do produto (saldo do mês, entradas, saídas, metas, cofrinhos, categorias, lançamentos recentes). Hierarquia: o **saldo do mês** é o único destaque grande; entradas e saídas ficam um nível abaixo; metas e cofrinhos são secundários; gráficos só onde ajudam a comparar (barras entrada/saída, pizza por categoria) e usam as cores dos tokens. Valores monetários com `tabular-nums`, alinhados à direita nas listas e com sinal explícito. Sem gradientes, brilhos ou animações decorativas; no máximo um cartão destacado por tela.

Os tokens são mapeados para nomes semânticos do Tailwind (`bg-surface`, `bg-surface-raised`, `border-line`, `text-income`...) em `src/styles.scss`, seguindo `references/neon-graphite-tokens.md` da skill de UI (atualizado com esta paleta).

### Componentes compartilhados (origem → destino)

| Componente | Origem | Comportamento essencial |
|---|---|---|
| `KpiCard` | `kpi_card.dart` | rótulo, valor monetário ou texto, ícone, cor, variação opcional (▲/▼ com texto, não só cor) |
| `AppPanel` | `widgets/web/app_panel.dart` | cartão com título, ação à direita e conteúdo |
| `PageHeader` | `web_page_header.dart` | título, subtítulo, ações |
| `MonthSwitcher` | `month_switcher.dart` | anterior/próximo, rótulo `Junho 2026`, volta ao atual |
| `CurrencySelector` | `currency_selector.dart` | BRL \| EUR, versão compacta |
| `EmptyState` | `empty_state.dart` | ícone + mensagem |
| `ConfirmDialog` | `confirm_delete_dialog.dart` | confirmação de exclusão com o nome do item |
| `Toast` | `feedback.dart` | sucesso, erro, aviso (ícone + cor), anunciado por leitores de tela |
| `MoneyInput` | `amount_input_field.dart` | máscara por dígitos (centavos), prefixo da moeda, grande destaque |
| `DataTable` + paginação | `DataTable` das telas | cabeçalho em maiúsculas, linhas alternadas, rodapé `1–15 de N` |
| `ProgressBar` | `LinearProgressIndicator` | valor 0..1, cor por estado |
| `IconPicker`, `ColorPicker` | `category_form_dialog.dart` | grade de 24 ícones / 12 cores (cofrinho: 10 ícones); ícones da biblioteca **Lucide** com tabela de correspondência dos nomes Material da origem (ADR-013); cores gravadas como `#RRGGBB` |
| `TypeToggle` | `_TypeBtn` | ENTRADA / SAÍDA |
| Gráficos | `income_expense_bar_chart`, `category_pie_chart` | barras duplas entrada/saída com dica de valor; pizza com percentuais e legenda; **ApexCharts** via `ng-apexcharts` (ADR-013), SVG, tema claro/escuro e cores dos tokens, com alternativa textual |

### Formatação (DESC-CUR-07, DESC-PRF-02)

`Intl` com `pt-BR`: `R$ 1.234,56` / `€ 1.234,56`; sinal explícito `+`/`-` em listas; datas `dd/MM/yyyy`; mês `Junho 2026`. O valor vem da API como string decimal e **não** é convertido para `number` para cálculos.

### Estados

Cada componente de dados tem estados de carregamento (esqueleto ou indicador), vazio (mensagens da origem), erro e sucesso. Tema claro/escuro por classe `dark` no `html`. **Padrão: escuro** (decisão de 2026-10-27), aplicado já no `index.html` para não piscar o tema claro; só a escolha explícita `LIGHT` guardada o remove. `SYSTEM` continua disponível como modo (segue `prefers-color-scheme`), mas não é mais o valor inicial.

### Riscos

Contraste medido (ver Tokens). Falta medir o contraste dos gráficos ApexCharts nos dois temas (F7).

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [FRONTEND-SPEC-07](../30-frontend-responsividade-e-acessibilidade/spec.md)

## Regras e critérios de aceitação

- [x] Todos os tokens existem como variáveis e nomes semânticos, com valores equivalentes nos dois temas.
  > ✅ 2026-10-27 — Paleta Grafite Ciano (escolhida pelo utilizador) em `src/styles.scss`: variáveis `--app-*` nos temas claro e escuro (padrão) mapeadas para `@theme inline`, mais raios, sombras e movimento. Contraste medido por script (WCAG): texto ≥ 5,5:1 nos dois temas, `line-input` ≥ 3:1 (valores em Tokens).
- [x] Nenhum template usa cor literal.
  > ✅ 2026-10-27 — Verificação após a refatoração: busca por cores hexadecimais, `bg-white/black/slate…` e valores arbitrários (`[..px]`, `z-[..]`) em `.ts` e `.html` de `src/app` sem ocorrências.
- [x] O tema escuro é o padrão numa sessão nova e o claro é uma paleta própria completa.
  > ✅ 2026-10-27 — `ThemeStore` inicia em `DARK` e `index.html` já traz a classe `dark`; teste "padrão é o tema escuro" e "respeita a escolha guardada (claro)" em `stores.spec.ts` (73 testes verdes). `/entrar` conferido nos dois temas no navegador.
- [ ] `KpiCard`, `MoneyInput`, `ConfirmDialog` e `Toast` têm nome acessível, estados desabilitado/inválido e funcionam por teclado.
  > ⏳ 2026-10-27 — Parcial: `Toast` (com `aria-live`) implementado. Falta: `KpiCard`, `MoneyInput` e `ConfirmDialog` (F5/F7 e fatias que os usam; escopo da F2 aprovado pelo utilizador).
- [ ] Estados financeiros (entrada, saída, aviso, estourado) usam ícone ou texto além da cor.
  > ⏳ 2026-10-27 — Parcial: o `Toast` usa ícone e cor. Falta: demais estados nas fatias com dados.
- [x] A formatação reproduz exemplos da origem: `R$ 1.234,56`, `€ 1.234,56`, `+R$ 100,00`, `-R$ 100,00`.
  > ✅ 2026-10-27 — `formatMoney`/`formatDate` testados com `R$ 1.234,56`, `€ 1.234,56`, `+R$ 100,00`, `-R$ 100,00` (`format-money.spec.ts`, 7 testes), sem converter para `number`. Desvio: o `Intl` usa espaço sem quebra entre o símbolo e o valor.

### Skills de implementação

- **Skills obrigatórias:** `angular-tailwind-ui-specialist`, `angular-architect`.
- **Caminhos:** `.claude/skills/angular-tailwind-ui-specialist/SKILL.md`; `.claude/skills/angular-architect/SKILL.md`.
- **Responsabilidade:** apresentação, tokens e acessibilidade (primeira); fronteiras e tipagem dos componentes (segunda).
- **Momento de utilização:** depois do shell e antes das páginas.
- **Dependências:** FRONTEND-SPEC-00; ADR-013 (ApexCharts, Lucide).
- **Limitações:** a skill de UI não define regras de negócio nem cria projeto.

## Tasks

### Descoberta e decisão

- [ ] Confirmar a compatibilidade de ApexCharts/`ng-apexcharts` e Lucide com Angular 21 e validar o contraste dos tokens.
  > ✅ 2026-10-27 — Contraste dos tokens medido nos dois temas (Grafite Ciano; ver Tokens).
  > ⏳ 2026-10-27 — Parcial: Lucide verificado em uso; ApexCharts por `peerDependencies`. Falta: testar o ApexCharts e o contraste dos gráficos (F7).

### Front-end

- [ ] Implementar tokens, tema claro/escuro e os componentes da tabela.
  > ⏳ 2026-10-27 — Parcial: tokens, tema (`ThemeStore`), `PageHeader`, `AppPanel`, `EmptyState`, `Toast`, `CurrencySelector`, `MonthSwitcher`, `ThemeToggle`, `FormField`; padronização visual de 2026-10-27: `appButton` e estilo único de campo (antes repetidos em 8 lugares), superfícies em quatro níveis, `rounded-control`/`rounded-card`, sombras e movimento por token, item de menu ativo discreto. Falta: `KpiCard`, `MoneyInput`, `ConfirmDialog`, `DataTable`, `ProgressBar`, pickers, `TypeToggle` e gráficos, adiados para as fatias que os usam (escopo F2 aprovado).

### Validação

- [ ] Percorrer a lista de revisão de UI da skill nos dois temas e em 320 px.
  > ⏳ 2026-10-27 — Parcial: após a paleta Grafite Ciano, `/entrar` e o shell (menu, cabeçalho, painel "Em breve") vistos nos dois temas no navegador, e o shell a 375 px sem rolagem horizontal (`scrollWidth` = `clientWidth`). Falta: percorrer a lista completa da skill (320, 768 e 1440 px, teclado ponta a ponta) e as páginas das próximas fatias.

## Resultado esperado

- Biblioteca interna de componentes pequena, reutilizável, acessível e fiel à identidade Grafite Ciano.
- Preservada a distinção visual de receitas, despesas e avisos.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
