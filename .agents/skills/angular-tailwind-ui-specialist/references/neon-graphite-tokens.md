# Tokens Grafite Ciano (evolução do Neon Graphite)

Paleta aprovada em 2026-10-27 (spec 25). Fonte única: `src/styles.scss`. O tema **escuro é o padrão**; o claro é uma paleta própria. Use só os nomes semânticos; nunca cores literais nos templates.

| Token Tailwind | Escuro | Claro | Uso |
|---|---|---|---|
| `bg-surface` | `#0A0F1A` | `#F4F6FA` | fundo da aplicação |
| `bg-brand` (+ `text-on-brand`, `text-on-brand-muted`, `text-accent-on-brand`) | `#0D1424` | `#0F172A` | menu lateral |
| `bg-surface-raised` | `#121A2B` | `#FFFFFF` | cartões, painéis, cabeçalho |
| `bg-surface-sunken` | `#0C1322` | `#EEF1F6` | campos, áreas rebaixadas, *hover* |
| `bg-surface-overlay` | `#172136` | `#FFFFFF` | menus, popovers, modais, notificações |
| `border-line` | `#1F2A40` | `#D5DBE6` | bordas de cartões e divisórias |
| `border-line-input` | `#5B6B8A` | `#7B8AA3` | bordas de campos e botões secundários |
| `text-content` / `text-content-muted` | `#E6EDF7` / `#9AA8BF` | `#0F172A` / `#4B5A70` | texto |
| `bg-accent` / `text-on-accent` | `#38BDF8` / `#06121F` | `#0369A1` / `#FFFFFF` | ação principal, foco, seleção |
| `text-income` | `#34D399` | `#047857` | entradas |
| `text-expense` | `#FB7185` | `#BE123C` | saídas e erro |
| `text-warning` | `#FBBF24` | `#92400E` | alerta de meta |

Também por token: `rounded-control` (8 px), `rounded-card` (12 px), `shadow-card`, `shadow-overlay`, `duration-fast`, `ease-standard`.

## Regras

- Profundidade: fundo → menu → cartão → campo rebaixado → sobreposição. No escuro a elevação vem de superfície e borda, não de sombra.
- `accent` é para ação, foco e seleção; `income`, `expense` e `warning` só têm significado financeiro e vêm sempre com ícone ou texto.
- Fundos suaves de estado usam o modificador de opacidade (`bg-expense/10`), sem novos tokens.
- Botões: diretiva `appButton` (`primary`, `secondary`, `ghost`). Campos: `appFieldControl` dentro de `app-form-field`. Não repita classes de botão ou campo nas páginas.
- Antes de criar um novo token, confirme que o papel se repete em mais de uma funcionalidade; ao mudar um valor, remeça o contraste (texto ≥ 4,5:1; bordas de controles e foco ≥ 3:1) nos dois temas e atualize a spec 25.
