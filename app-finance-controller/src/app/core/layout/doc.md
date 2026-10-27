# core/layout

Shell da aplicação autenticada (FRONTEND-SPEC-01/07, fatia F2).

## Componentes

- `ShellComponent` (`shell.ts`/`shell.html`): menu lateral fixo em `lg+`; abaixo de `lg`, gaveta aberta pelo botão de menu. Cabeçalho com `MonthSwitcher` (só quando `route.data.showMonth === true`), `CurrencySelector`, `ThemeToggle` e menu do utilizador (e-mail + `Sair`). `<main id="conteudo" tabindex="-1">` com `<router-outlet />`.
- `AppTitleStrategy`: título do documento `"<route.title> · Finance Controller"`; registrada em `app.config.ts` (`TitleStrategy`).
- `NAV_ITEMS`: Dashboard `/` (exato), Lançamentos `/lancamentos`, Relatórios `/relatorios`, Categorias `/categorias`, Cofrinhos `/cofrinhos`, Meta `/metas` (prefixo).

O contêiner de toasts **não** fica aqui: está em `app.html`, para aparecer também em `/entrar` e `/cadastrar`.

## Dependências

`SessionStore` (e-mail), `ReferenceMonthStore`, `CurrencyStore`/`ThemeStore` (via componentes de `shared/ui`), `AuthService.logout()` (limpa a sessão e navega para `/entrar`; falha → toast de erro).

## Comportamento e decisões

- Um único `ng-template` de navegação renderizado no menu fixo e na gaveta (sem duplicar markup). O menu fixo é `hidden lg:flex` (fora da ordem de tabulação abaixo de `lg`); a gaveta só existe no DOM quando aberta (`@if`).
- Rota ativa: `routerLinkActive` + `ariaCurrentWhenActive="page"`; o destaque visual vem do atributo (`aria-[current=page]:bg-accent`), então o estilo e o anúncio têm a mesma fonte. Dashboard usa correspondência exata; as demais, por prefixo.
- Após cada `NavigationEnd`: fecha gaveta/menu do utilizador, atualiza `showMonth` a partir dos `data` da rota folha e move o foco para o `h1` da página (ou o `main`) via `afterNextRender`. As páginas devem usar `app-page-header` (ou um único `h1`).
- Skip-link "Ir para o conteúdo" intercepta o clique (a tag `<base href="/">` faria `#conteudo` navegar) e foca o `main`.
- Rota `**` fica dentro do shell (404 com menu para quem está autenticado; sem sessão o `authGuard` leva a `/entrar`).
- Rótulo de versão: constante `APP_VERSION` (`v0.1.0`); atualizar quando houver política de versionamento.

## Acessibilidade

Gaveta com `role="dialog"` `aria-modal`, foco inicial no botão Fechar, foco preso (Tab/Shift+Tab circulam), Esc fecha e devolve o foco ao botão de menu, clique no fundo fecha. Botão de menu com `aria-expanded`/`aria-controls`. Menu do utilizador é uma divulgação (`aria-expanded`), fecha com Esc (foco volta ao botão), clique fora ou saída do foco. Alvos de 44 px; animação da gaveta de 150 ms (reduzida globalmente por `prefers-reduced-motion`).

## Validação

`shell.spec.ts`: seis entradas na ordem, `aria-current` na rota ativa, seletor de mês condicionado a `showMonth`, foco no `h1`, gaveta com Esc, menu do utilizador. Pendente de verificação manual: 320 px sem rolagem horizontal, ordem de foco real e contraste nos dois temas.
