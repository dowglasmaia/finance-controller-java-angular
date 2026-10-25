# Tokens Neon Graphite

Use these as the initial semantic palette for Finance Controller. They reflect the approved product direction and must be centralized in `src/styles.scss` when the frontend is initialized.

```scss
@use 'tailwindcss';

:root {
  --app-surface: #f1f5f9;
  --app-surface-raised: #ffffff;
  --app-content: #0f172a;
  --app-content-muted: #475569;
  --app-brand: #0f172a;
  --app-accent: #22d3ee;
  --app-income: #34d399;
  --app-expense: #fb7185;
  --app-warning: #fbbf24;
}

.dark {
  --app-surface: #0b1120;
  --app-surface-raised: #111827;
  --app-content: #e2e8f0;
  --app-content-muted: #94a3b8;
  --app-brand: #0f172a;
}

@theme inline {
  --color-surface: var(--app-surface);
  --color-surface-raised: var(--app-surface-raised);
  --color-content: var(--app-content);
  --color-content-muted: var(--app-content-muted);
  --color-brand: var(--app-brand);
  --color-accent: var(--app-accent);
  --color-income: var(--app-income);
  --color-expense: var(--app-expense);
  --color-warning: var(--app-warning);
}
```

This mapping enables semantic utilities such as `bg-surface`, `bg-surface-raised`, `text-content`, `text-content-muted`, `text-income`, `text-expense`, and `text-warning`. Extend it only when a repeated visual role has a clear name and use across more than one feature.

Use `brand` and `accent` for hierarchy and interactive emphasis. Reserve `income`, `expense`, and `warning` for financial meaning; they must not become generic decorative colors.
