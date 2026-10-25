---
name: angular-tailwind-ui-specialist
description: Design and implement polished, responsive, accessible Angular 21+ interfaces using Tailwind CSS. Use for Angular templates, visual hierarchy, CSS tokens, responsive layouts, forms, states, and UI review. Do not use for business rules, API design, or creating a new Angular project.
---

# Angular Tailwind UI specialist

Build clear and durable Angular interfaces with Tailwind CSS. This skill owns the presentation layer: HTML templates, component-scoped styles when necessary, visual tokens, layout, responsive behavior, and accessibility. It does not own domain logic, HTTP operations, application state, or OpenAPI contracts.

## Product direction

Read `.spec/memory/produto.md` and `.spec/memory/contexto-tecnico.md` before changing the interface. For Finance Controller, preserve the Neon Graphite identity, responsive web use, Portuguese (Brazil), clear financial status colors, light/dark theme support, and comprehensible loading, empty, error, and validation states.

Read [Neon Graphite tokens](references/neon-graphite-tokens.md) before introducing a new shared color or visual primitive. Read [UI review checklist](references/ui-review-checklist.md) before declaring a UI task complete.

## Working process

1. Inspect the affected feature, existing global styles, design tokens, Tailwind configuration, and reusable UI components. Reuse an established pattern before adding a new one.
2. Identify the user task, main action, content hierarchy, data states, and smallest useful responsive layout.
3. Implement semantic Angular HTML first: headings in order, native controls, labels, buttons, lists, tables, and landmarks. Add Tailwind utility classes directly in the template for spacing, layout, typography, colors, and breakpoints.
4. Use mobile-first utilities. Start with the narrow layout and add `sm:`, `md:`, `lg:`, or `xl:` only where the content needs more room.
5. Use component SCSS only for selectors, pseudo-elements, animation details, or patterns that utilities cannot express cleanly. Keep it local to the component. Do not recreate a second global design system in component styles.
6. Verify the states and viewport sizes in the checklist. Run the project’s prescribed build or checks when the task requires validation.

## Tailwind and styling rules

- Tailwind CSS is the default styling tool. Use the Angular/Tailwind integration already established by `angular-frontend-scaffold`: `.postcssrc.json` with `@tailwindcss/postcss` and `@use 'tailwindcss';` in `src/styles.scss`.
- Prefer semantic design tokens over literal colors. Use the mapped Tailwind semantic names when present, such as `bg-surface`, `text-content`, `text-income`, `text-expense`, and `text-warning`.
- If the project has no approved token mapping, add the smallest shared semantic mapping required in `src/styles.scss`, based on the Neon Graphite reference. Do not add a component library or a large theme configuration.
- Use the spacing scale, `gap-*`, `p-*`, `space-y-*`, `max-w-*`, grid, and flex utilities consistently. Avoid arbitrary pixel values unless a concrete visual constraint demands one.
- Do not use `@apply` to hide a one-off collection of utilities. Extract a reusable Angular component only after a real second use, or when it encapsulates behavior and accessibility.
- Do not hardcode colors in templates, create unexplained magic z-index values, rely only on color to communicate financial status, or use hover as the only way to reveal an action.
- Respect `prefers-reduced-motion`; animations must be brief, optional, and never the only feedback.

## Responsive layout

- Support a usable layout from 320px through common tablet and desktop widths.
- Keep the primary action reachable without horizontal scrolling. Stack controls and cards on narrow screens; use grids or split layouts only when space supports them.
- Use responsive typography and line lengths that remain readable. Long financial values, dates, labels, and validation messages must wrap or truncate intentionally without hiding essential information.
- Tables must remain usable on small screens: choose a horizontally scrollable region with visible affordance, or transform each row into a labeled card when that is clearer. Do not shrink text until unreadable.
- Preserve touch targets of roughly 44 by 44 CSS pixels for primary interactive controls and leave enough separation to prevent accidental taps.

## Accessibility and interaction

- Use native `button`, `a`, `input`, `select`, `textarea`, `table`, and heading elements before ARIA roles. Add ARIA only when native semantics do not express the interaction.
- Every field has a persistent visible label. Connect validation and helper text with `aria-describedby`; mark invalid fields with `aria-invalid` when applicable.
- Provide a visible keyboard focus style with sufficient contrast. Ensure every interactive control is keyboard reachable and that focus order follows visual order.
- Maintain contrast in both themes. Pair income, expense, and warning color with text or an icon so status does not depend on color alone.
- Ensure loading, success, empty, and error messages are understandable and announced appropriately when their content changes.

## Angular boundaries

- Keep templates declarative. Do not calculate financial values, make HTTP calls, mutate shared state, or implement business rules in HTML or styling code.
- Bind typed view models from the feature component. Use `@if`, `@for`, and `@switch` to represent explicit UI states, with stable `track` expressions for lists.
- Keep presentation components focused on inputs, outputs, and rendering. Reusable controls must expose accessible names and support disabled, invalid, and loading behavior when relevant.
- Follow `angular-module-scaffold` for feature structure and approved API contracts. This skill refines the UI within that structure.
