# 24-frontend-rotas-e-navegacao

> **ID:** FRONTEND-SPEC-01 · **Categoria:** Frontend · **Estado:** proposta para revisão — não aprovada, não executada.

## Objetivo

Definir as rotas, o shell de navegação e os comportamentos de navegação do frontend, preservando as áreas funcionais da origem e adaptando-as a uma aplicação web responsiva.

## Escopo

- Inclui: tabela de rotas, shell com menu, seletor de moeda, mês e tema no shell, carregamento sob demanda, rota não encontrada.
- Não inclui: conteúdo das páginas, o guard e as páginas de entrada/cadastro (FRONTEND-SPEC-06).

## Contexto técnico local

### Rotas (origem → destino)

| Rota de origem (DESC-NAV-01) | Destino proposto | Página |
|---|---|---|
| `/` | `/` | Dashboard |
| `/transactions` | `/lancamentos` | Lançamentos |
| `/transactions/new`, `/transactions/:id/edit` | diálogo sobre `/lancamentos` (sem rota própria) | Formulário de lançamento |
| `/categories` | `/categorias` | Categorias |
| `/categories/new`, `/categories/:id/edit` | diálogo sobre `/categorias` | Formulário de categoria |
| `/reports` | `/relatorios` | Relatórios |
| `/budget` | `/metas` | Meta de gastos |
| `/backup-data` | — (removida, P-03) | — |
| — | `/entrar`, `/cadastrar` | Entrada e cadastro (públicas; FRONTEND-SPEC-06) |
| — | `/esqueci-senha`, `/redefinir-senha` | Recuperação de senha (públicas; FRONTEND-SPEC-16) |
| `/cofrinhos` | `/cofrinhos` | Cofrinhos |
| — | `**` | Página não encontrada, com link para o Dashboard |

Em web, a origem já abre criação/edição em diálogos (DESC-NAV-01); as rotas `new/edit` eram do mobile e **não** são criadas. A escolha dos caminhos em português (PROPOSTO) segue a interface; pode manter os caminhos em inglês da origem se preferido (decisão de marca de URL, sem impacto funcional).

### Shell (DESC-NAV-02)

- Menu lateral com **seis** entradas na ordem: Dashboard, Lançamentos, Relatórios, Categorias, Cofrinhos, Meta (a entrada Backup da origem foi removida — P-03); item ativo destacado, correspondência por prefixo (`/lancamentos/...`).
- Marca "Finance Controller" e rótulo de versão.
- Seletor de moeda BRL | EUR, alternância de tema e menu do utilizador com o e-mail e a ação `Sair` no shell.
- Conteúdo com largura máxima e rolagem própria.
- Em telas estreitas o menu vira gaveta (FRONTEND-SPEC-07); a navegação continua acessível por teclado.

### Contexto compartilhado entre páginas (DESC-MES-01)

O seletor de mês (‹ Junho 2026 ›, clique no rótulo volta ao atual) aparece nas páginas Dashboard, Lançamentos e Relatórios; a página Metas usa o mesmo mês. O mês de referência é estado global do cliente (FRONTEND-SPEC-04) e não vai na URL; alternativa de `?mes=AAAA-MM` fica como melhoria opcional (links partilháveis), não como requisito.

### Carregamento

Cada funcionalidade é uma rota **lazy** (`loadChildren`) com o seu `<modulo>.routes.ts`; título da página por rota; foco movido para o título da página após a navegação (acessibilidade).

### Pendências

A origem não tinha rota de erro nem de login; a autenticação (P-01) acrescenta `/entrar` e `/cadastrar`, e o guard de FRONTEND-SPEC-06 protege todas as demais rotas.

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [FRONTEND-SPEC-00](../23-frontend-arquitetura-angular/spec.md)
- [FRONTEND-SPEC-07](../30-frontend-responsividade-e-acessibilidade/spec.md)

## Regras e critérios de aceitação

- [ ] Cada rota da tabela abre a página correspondente e é carregada sob demanda; sem sessão, as rotas de dados levam a `/entrar`.
  > ⏳ 2026-10-27 — Parcial: rotas lazy criadas (`/`, `/lancamentos`, `/relatorios`, `/categorias`, `/cofrinhos`, `/metas`, `/entrar`, `/cadastrar`, `**`); sem sessão, `/lancamentos` levou a `/entrar?returnUrl=%2Flancamentos` (navegador, API simulada). Falta: as páginas reais das rotas de dados (hoje página provisória "Em breve"; F4–F9) e `/esqueci-senha`, `/redefinir-senha` (F11). Próxima ação: cada fatia substitui a sua rota.
- [ ] O item de menu da página atual fica ativo, inclusive em subrotas.
  > ⏳ 2026-10-27 — Parcial: `aria-current="page"` e destaque verificados em teste (`shell.spec.ts`) e no navegador. Falta: conferir subrotas (ainda não existem).
- [ ] Rota desconhecida mostra a página não encontrada, sem erro de consola.
  > ⏳ 2026-10-27 — Parcial: `NotFoundPage` criada como `**` dentro do shell. Falta: verificar a ausência de erro de consola.
- [ ] O mês e a moeda escolhidos persistem ao navegar entre páginas (mês só durante a sessão; moeda entre sessões — DESC-MES-01, DESC-CUR-03).
  > ⏳ 2026-10-27 — Parcial: estado global em `ReferenceMonthStore` (sessão) e `CurrencyStore` (`localStorage`), testados em `stores.spec.ts`. Falta: verificar a persistência ao navegar entre páginas com dados (F5 em diante).
- [ ] Todos os itens de menu são alcançáveis por teclado e anunciam o estado "página atual".
  > ⏳ 2026-10-27 — Parcial: menu com `a`/`button` nativos, `aria-current` e gaveta com Esc e foco devolvido (teste e navegador). Falta: percurso completo por Tab/Enter.

### Skills de implementação

- **Skills obrigatórias:** `angular-architect`, `angular-module-scaffold`, `angular-tailwind-ui-specialist` (menu e shell).
- **Caminhos:** `.claude/skills/angular-architect/SKILL.md`; `.claude/skills/angular-module-scaffold/SKILL.md`; `.claude/skills/angular-tailwind-ui-specialist/SKILL.md`.
- **Responsabilidade:** estrutura de rotas por funcionalidade; apresentação do shell.
- **Momento de utilização:** depois do scaffold e antes das páginas.
- **Dependências:** FRONTEND-SPEC-00, 02, 04.
- **Limitações:** `angular-module-scaffold` exige contrato OpenAPI aprovado para módulos que consomem API.

## Tasks

### Front-end

- [x] Implementar o shell, as rotas lazy e a página não encontrada.
  > ✅ 2026-10-27 — Shell (menu lateral, gaveta, seletores de moeda/mês/tema, menu do utilizador com `Sair`), rotas lazy e `NotFoundPage` entregues; `AppTitleStrategy` define o título. Verificação: build e `shell.spec.ts`; navegador. Desvio: as rotas de dados apontam para `ComingSoonPage` até as fatias respectivas.

### Validação

- [ ] Testar a navegação por teclado e a rota ativa; build de produção.
  > ⏳ 2026-10-27 — Parcial: rota ativa e build verificados. Falta: navegação por teclado completa.

## Resultado esperado

- Navegação equivalente à da origem em web, com URLs claras e carregamento sob demanda.
- Preservadas as sete áreas funcionais e os seletores globais de moeda, mês e tema.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
