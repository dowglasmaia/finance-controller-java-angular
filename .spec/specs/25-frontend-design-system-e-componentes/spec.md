# 25-frontend-design-system-e-componentes

> **ID:** FRONTEND-SPEC-02 · **Categoria:** Frontend · **Estado:** proposta para revisão — não aprovada, não executada.

## Objetivo

Definir os tokens visuais (Neon Graphite) e os componentes compartilhados reais da origem, para que as páginas sejam compostas de forma consistente em tema claro e escuro.

## Escopo

- Inclui: tokens de cor, tipografia e espaçamento; inventário de componentes compartilhados com comportamento; formatação.
- Não inclui: páginas, regras financeiras, instalação de UI kits.

## Contexto técnico local

### Tokens (DESC-UX-02)

| Papel | Valor | Observação |
|---|---|---|
| `income` | `#34D399` | entradas e estado "dentro da meta" |
| `expense` | `#FB7185` | saídas e estado "estourada" |
| `warning` | `#FBBF24` | "perto da meta" |
| `primary` | `#0F172A` | grafite da marca e do menu |
| `accent` | `#22D3EE` | ciano neon: seleção e acento |
| `surface` / `surfaceDark` | `#F1F5F9` / `#0B1120` | fundos |
| `neutral` | `#64748B` | texto secundário |
| `webCardDark`, `webBorderDark` | `#131C30`, `#24304A` | superfícies e bordas no escuro |

**Decisão (aprovada pelo utilizador em 2026-10-27):** no tema claro os papéis `accent`, `income`, `expense` e `warning` usam variantes mais escuras (`#0E7490`, `#047857`, `#BE123C`, `#B45309`) para ganhar contraste sobre fundos claros; no tema escuro valem os valores da tabela (`#22D3EE`, `#34D399`, `#FB7185`, `#FBBF24`). A razão de contraste ainda deve ser medida (task de descoberta abaixo).

Os tokens são mapeados para nomes semânticos do Tailwind (`bg-surface`, `text-income`, `text-expense`, `text-warning`) seguindo `references/neon-graphite-tokens.md` da skill de UI; cores nunca fixas em templates. Fonte Inter. Espaçamento em escala de 4 px.

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

Cada componente de dados tem estados de carregamento (esqueleto ou indicador), vazio (mensagens da origem), erro e sucesso. Tema claro/escuro por classe no `html`, respeitando `prefers-color-scheme` quando `SYSTEM`.

### Riscos

Contraste do ciano sobre fundo claro precisa de verificação (a origem não mediu); a skill exige contraste suficiente nos dois temas.

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [FRONTEND-SPEC-07](../30-frontend-responsividade-e-acessibilidade/spec.md)

## Regras e critérios de aceitação

- [ ] Todos os tokens existem como variáveis e nomes semânticos, com valores equivalentes nos dois temas.
  > ⏳ 2026-10-27 — Parcial: tokens como variáveis CSS e nomes semânticos Tailwind em `styles.scss`, com claro e escuro. Desvio a aprovar: no tema claro `accent`, `income`, `expense` e `warning` foram escurecidos (`#0e7490`, `#047857`, `#be123c`, `#b45309`) em vez dos valores da spec, para contraste; razão de contraste não medida numericamente.
  > ✅ 2026-10-27 — Decisão: o utilizador aprovou as cores escurecidas do tema claro (ver "Decisão" em Tokens). Resta medir o contraste e percorrer a lista de revisão de UI; este critério continua aberto até lá.
- [x] Nenhum template usa cor literal.
  > ✅ 2026-10-27 — Verificação: busca por cores hexadecimais em `.ts` e `.html` de `src/app` sem ocorrências.
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
  > ✅ 2026-10-27 — Decisão registada: cores do tema claro escurecidas, aprovadas pelo utilizador. A task continua aberta: falta medir a razão de contraste dos tokens nos dois temas e testar o ApexCharts (F7).
  > ⏳ 2026-10-27 — Parcial: Lucide verificado em uso; ApexCharts por `peerDependencies`. Falta: validar o contraste dos tokens com medição.

### Front-end

- [ ] Implementar tokens, tema claro/escuro e os componentes da tabela.
  > ⏳ 2026-10-27 — Parcial: tokens, tema (`ThemeStore`), `PageHeader`, `AppPanel`, `EmptyState`, `Toast`, `CurrencySelector`, `MonthSwitcher`, `ThemeToggle`, `FormField`. Falta: `KpiCard`, `MoneyInput`, `ConfirmDialog`, `DataTable`, `ProgressBar`, pickers, `TypeToggle` e gráficos, adiados para as fatias que os usam (escopo F2 aprovado).

### Validação

- [ ] Percorrer a lista de revisão de UI da skill nos dois temas e em 320 px.
  > ⏳ 2026-10-27 — Parcial: 320 px sem rolagem horizontal verificado em `/entrar` e no shell; temas claro e escuro vistos no navegador. Falta: percorrer a lista completa da skill.

## Resultado esperado

- Biblioteca interna de componentes pequena, reutilizável, acessível e fiel à identidade Neon Graphite.
- Preservada a distinção visual de receitas, despesas e avisos.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
