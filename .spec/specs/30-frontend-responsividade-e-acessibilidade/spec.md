# 30-frontend-responsividade-e-acessibilidade

> **ID:** FRONTEND-SPEC-07 · **Categoria:** Frontend · **Estado:** proposta para revisão — não aprovada, não executada.

## Objetivo

Garantir que a aplicação seja usável em computador, tablet e celular pelo navegador e acessível por teclado e tecnologias assistivas, indo além da origem, cujo layout web era orientado a desktop.

## Escopo

- Inclui: pontos de quebra, comportamento do menu, tabelas, formulários e diálogos em telas estreitas, critérios de acessibilidade.
- Não inclui: aplicativos nativos, layouts móveis da origem.

## Contexto técnico local

### Evidência e lacuna

A origem alterna entre layouts móvel e web por plataforma (`Responsive.of`, `AdaptiveLayout`) e a web usa menu lateral fixo e grades (DESC-NAV-02). Não foi encontrada evidência de adaptação para tablet ou janela estreita no layout web (INFERIDO: depende da largura máxima do conteúdo e de `ResponsiveRow`, não executado). O produto exige uso responsivo no navegador. A responsividade abaixo é PROPOSTO.

### Regras (PROPOSTO, alinhadas à skill de UI)

| Tema | Regra |
|---|---|
| Larguras | usável de 320 px até desktop; mobile-first com `sm`, `md`, `lg`, `xl` apenas quando o conteúdo precisa |
| Menu | lateral fixa em `lg+`; gaveta com botão de menu abaixo disso |
| KPIs | 1 coluna em estreito; 2 em `sm`; 4 em `lg` |
| Tabelas | rolagem horizontal visível **ou** linhas viram cartões rotulados; nunca texto minúsculo |
| Gráficos | largura fluida com altura mínima; legenda abaixo em estreito |
| Diálogos | tela cheia em telas estreitas; rolagem interna; foco preso e devolvido ao fechar |
| Alvos de toque | ~44×44 px nos controles principais |
| Valores longos | quebram ou truncam com intenção, sem esconder informação essencial |

### Acessibilidade

- Semântica nativa (`button`, `a`, `table`, títulos em ordem); ARIA só quando necessário.
- Foco visível e ordem lógica; tudo alcançável por teclado, incluindo menu, seletor de mês, tabelas paginadas e diálogos (Esc fecha).
- Contraste suficiente nos dois temas; estados financeiros com ícone ou texto além da cor (DESC-UX-01/02).
- Mudanças de estado (carregando, erro, sucesso, alerta de meta) anunciadas por região `aria-live`.
- Respeito a `prefers-reduced-motion`: as animações de entrada do dashboard da origem (`flutter_animate`) viram transições curtas e opcionais.
- Idioma `pt-BR` no documento.

### Riscos

Tabelas largas de lançamentos (6 colunas) são o ponto mais frágil em celular; decidir cartões vs rolagem na página de lançamentos (FRONTEND-SPEC-10).

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [FRONTEND-SPEC-02](../25-frontend-design-system-e-componentes/spec.md)

## Regras e critérios de aceitação

- [ ] Em 320, 768 e 1280 px não há rolagem horizontal da página e a ação principal de cada página está visível.
  > ⏳ 2026-10-27 — Parcial: 320 px sem rolagem horizontal verificado em `/entrar` e no shell. Falta: 768 e 1280 px e as páginas de dados.
- [ ] O menu é totalmente operável por teclado e por toque em todas as larguras.
  > ⏳ 2026-10-27 — Parcial: gaveta abre, prende o foco, fecha com Esc e devolve o foco (teste e navegador); alvos de 44 px. Falta: percurso completo por teclado e por toque.
- [ ] Diálogos prendem o foco, fecham com Esc e devolvem o foco ao elemento de origem.
  > ⏳ 2026-10-27 — Parcial: a gaveta cumpre o requisito; os diálogos de formulário e de confirmação vêm nas fatias de dados.
- [ ] Os estados de carregamento, erro, sucesso e alerta são anunciados por leitores de tela.
  > ⏳ 2026-10-27 — Parcial: `Toast` com região `aria-live` e erros de formulário com `role="alert"`. Falta: estados das páginas de dados.
- [ ] Um verificador automático de acessibilidade não reporta violações críticas nas páginas principais.
  > ⏳ 2026-10-27 — Parcial: não executado. Falta: rodar nas páginas principais (F12 ou por fatia).

### Skills de implementação

- **Skills obrigatórias:** `angular-tailwind-ui-specialist`.
- **Caminhos:** `.claude/skills/angular-tailwind-ui-specialist/SKILL.md` (referências `ui-review-checklist.md` e `neon-graphite-tokens.md`).
- **Responsabilidade:** layout responsivo, acessibilidade e revisão de UI.
- **Momento de utilização:** em cada página e no shell; revisão final por checklist.
- **Dependências:** FRONTEND-SPEC-01, 02.
- **Limitações:** a skill não define regras de negócio.

## Tasks

### Front-end

- [ ] Implementar gaveta de menu, grades responsivas e variantes de tabela.
  > ⏳ 2026-10-27 — Parcial: gaveta de menu entregue. Falta: grades de KPIs e variantes de tabela (fatias de dados).
- [x] Implementar foco, `aria-live` e redução de movimento.
  > ✅ 2026-10-27 — Foco no `h1` após navegar (teste e navegador), foco da gaveta, região `aria-live` no `Toast`, regra CSS `prefers-reduced-motion` e `lang="pt-BR"`. Desvio: a redução de movimento é regra CSS, sem teste automatizado.

### Validação

- [ ] Executar a lista de revisão de UI e a verificação de acessibilidade nas páginas principais.
  > ⏳ 2026-10-27 — Parcial: verificação visual em 320 px e nos dois temas. Falta: lista completa e verificador de acessibilidade.

## Resultado esperado

- Uso confortável de 320 px a desktop, com navegação completa por teclado.
- Preservada a identidade visual e a distinção de estados sem depender só de cor.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
