# shared

Design system mínimo e utilitários reutilizáveis (FRONTEND-SPEC-02/05, fatia F2). Sem regra de negócio, sem HTTP.
Só entra aqui o que a F2 usa; `DataTable`, `ProgressBar`, pickers e `TypeToggle` chegam nas fatias F10 em diante (`KpiCard` entrou na F7 e é reutilizado nos cofrinhos da F8; os gráficos do dashboard são SVG próprio em `features/dashboard`) (`Modal` e `ConfirmDialog` entraram na F4).

## Componentes (`ui/`)

Standalone, `OnPush`, `input()`/`output()`, seletor `app-*`, cores só por tokens semânticos (`bg-surface`, `text-income` azul, `text-expense` vermelho, `text-reserve` violeta, `text-success` verde…), ícones Lucide (`[img]`, registrados por import nomeado, sem registro global).

| Componente | Seletor | API |
|---|---|---|
| `PageHeader` | `app-page-header` | `title` (obrigatório, vira o `h1`), `subtitle`; slot `[actions]` |
| `AppPanel` | `app-panel` | `title` (h2, opcional); slot `[panelAction]`; conteúdo padrão |
| `EmptyState` | `app-empty-state` | `message` (obrigatório), `title`, `icon` (`LucideIconData`, padrão `Inbox`); conteúdo padrão extra |
| `ToastService` | — | `success/error/warning/info(message)`, `show(kind, message, ms?)`, `dismiss(id)`, `toasts` (signal). Auto-dispensa em 5 s |
| `ToastContainerComponent` | `app-toast-container` | Montado **uma vez** em `app.html` (cobre páginas públicas). Região `aria-live="polite"`; erro usa `role="alert"` |
| `CurrencySelector` | `app-currency-selector` | Sem inputs; usa `CurrencyStore` (`select`). Botões com `aria-pressed` |
| `MonthSwitcher` | `app-month-switcher` | `label` (obrigatório); outputs `previous`, `next`, `reset`. O shell liga ao `ReferenceMonthStore` |
| `ThemeToggleComponent` | `app-theme-toggle` | Sem inputs; usa `ThemeStore`. `aria-pressed` = tema escuro ativo |
| `FormFieldComponent` + `FieldControlDirective` | `app-form-field` + `[appFieldControl]` | ver abaixo |
| `Modal` | `app-modal` | `title` (obrigatório, `h2` do diálogo); output `closed` (Esc ou clique no fundo). `role="dialog"` `aria-modal`, foco inicial no primeiro controle (ou `[data-autofocus]`), foco preso e devolvido a quem abriu. Quem usa o destrói com `@if` |
| `ConfirmDialog` | `app-confirm-dialog` | `title`, `message` (obrigatórios), `confirmLabel`, `busy`; outputs `confirmed`, `cancelled`. Foco inicial em Cancelar |
| `KpiCard` | `app-kpi-card` | `label`, `value` (já formatado), `tone` (`positive`/`negative` = bom/ruim em verde/vermelho; `neutral`; `income` azul, `expense` vermelho e `reserve` violeta = natureza do valor), `delta` (`KpiDelta`: texto, tom, ícone e frase por extenso). A variação repete na forma (sinal e ícone) o que a cor diz |
| `MoneyInputDirective` | `input[appMoneyInput]` (em `shared/forms`) | Campo de valor: guarda o decimal da API como texto (`"1234.56"`), mostra com vírgula, aceita só dígitos e um separador, no máximo 2 casas; nunca usa `number`. Combine com `requiredValue`, `decimalValue`, `positiveAmount` |

### FormField

```html
<app-form-field label="E-mail" help="Usado para entrar." [required]="true" [control]="form.controls.email">
  <input appFieldControl type="email" formControlName="email" autocomplete="email" />
</app-form-field>
```

- Importe `FORM_FIELD_IMPORTS` (`FormFieldComponent` + `FieldControlDirective`) no componente.
- Inputs: `label` (obrigatório), `help`, `required` (apenas o `*` visual; o atributo `required` é do consumidor), `control` (AbstractControl) e `error` (texto explícito, tem prioridade).
- Com `control`, a mensagem vem de `visibleErrorMessage` (só depois de tocado/alterado) e reage a eventos do controle (`control.events`), então funciona em `OnPush`/zoneless.
- `appFieldControl` define `id` (casa com `for` do rótulo — **não** defina `id` no controle), `aria-invalid="true"` quando há erro e `aria-describedby` com os ids estáveis da ajuda e/ou do erro. O erro é `<p role="alert">`.
- Ids: `app-field-N` (contador por instância), estáveis durante a vida do componente.

## Formulários (`forms/validators.ts`)

Validadores `ValidatorFn` tipados; cada erro é `{ <chave>: { message } }` com a mensagem pronta (spec 28):

`requiredText` (Campo obrigatório) · `requiredValue` (Informe um valor) · `decimalValue` (Valor inválido) · `positiveAmount` (O valor deve ser maior que zero) · `requiredCategory` (Selecione uma categoria) · `emailFormat` (Informe um e-mail válido) · `minLengthPassword(8)` (A senha deve ter pelo menos 8 caracteres) · `maxLength(n)` · `matchesField('senha')` (As senhas não coincidem).

Os validadores de formato/tamanho aceitam vazio (combine com `requiredText`/`requiredValue`). Decimais são strings (`"1234.56"`) e nunca passam por `number`.

Helpers: `revalidateOnChange(origem, destino, destroyRef)` (reavalia a confirmação quando a senha muda) · `firstErrorMessage` / `visibleErrorMessage` · `focusFirstInvalid(formEl)` (foca o primeiro `input/select/textarea` com `ng-invalid` ou `aria-invalid`; chame após `markAllAsTouched()` no submit) · `applyServerErrors(form, apiError.fieldErrors)` (marca o erro `server` nos controles homônimos e **devolve** os `FieldError` sem campo, para `ToastService.error`). O erro `server` some quando o valor muda.

## Dinheiro e datas (`money/format-money.ts`)

- `formatMoney('1234.56', 'BRL')` → `R$ 1.234,56`; `'EUR'` → `€ 1.234,56`; `'+100.00'` → `+R$ 100,00`; `'-100.00'` → `-R$ 100,00`.
- Usa `Intl.NumberFormat('pt-BR', { style: 'currency' })` com a **string** decimal (NumberFormat v3, precisão exata). Se o runtime não preservar precisão (probe na primeira chamada), cai em `formatCurrencyManual` (BigInt, sem `number`).
- O `Intl` usa NBSP (U+00A0) entre símbolo e valor — intencional (evita quebra de linha). Testes normalizam para espaço.
- `formatDate('2026-06-15')` → `15/06/2026`, por texto, sem `Date` nem fuso. Entrada inválida volta como veio.

## Acessibilidade

Alvos ≥ 44 px (`h-11`/`min-h-11`), foco visível global (`:focus-visible` em `styles.scss`), rótulos persistentes, estado por `aria-pressed`/`aria-invalid`/`aria-describedby`, toasts anunciados por região `aria-live`, animações curtas (reduzidas por `prefers-reduced-motion` em `styles.scss`).

## Decisões

- `MonthSwitcher` é apresentação pura (inputs/outputs): reaproveitável e testável sem store.
- Ícones por `[img]` com import nomeado do `lucide-angular` (tree-shakable) em vez de `LucideAngularModule.pick`.
- Validadores devolvem a mensagem dentro do erro (uma única fonte de texto, spec 28) em vez de um mapa chave→mensagem no template.
