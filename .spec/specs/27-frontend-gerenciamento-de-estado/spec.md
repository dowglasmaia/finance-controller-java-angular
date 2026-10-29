# 27-frontend-gerenciamento-de-estado

> **ID:** FRONTEND-SPEC-04 · **Categoria:** Frontend · **Estado:** proposta para revisão — não aprovada, não executada.

## Objetivo

Definir quem é dono de cada estado do frontend — contexto global (mês, moeda, tema), estado de cada funcionalidade e invalidação após mutações —, substituindo os providers Riverpod da origem sem duplicar dados.

## Escopo

- Inclui: estado global, estado por funcionalidade, sessão, preferências, invalidação e atualização.
- Não inclui: regras financeiras (backend), apresentação (páginas).

## Contexto técnico local

### Estado global (signals no `core`)

| Estado | Origem | Persistência | Padrão |
|---|---|---|---|
| `referenceMonth` | `referenceMonthProvider` (DESC-MES-01) | nenhuma (por sessão) | 1.º dia do mês atual |
| `selectedCurrency` | `SelectedCurrencyNotifier` (DESC-CUR-03) | servidor, por utilizador (`getPreferences`/`savePreferences` — ADR-008, P-19) | `BRL` |
| `themeMode` | `ThemeModeNotifier` (DESC-PRF-01) | servidor, por utilizador; antes do login, `localStorage` só para o tema | `DARK` (padrão escuro, decisão de 2026-10-27); alternar: dark→light, light→dark; `SYSTEM` segue o sistema |
| `session` (utilizador e *access token*) | novo (P-01) | só memória (nunca `localStorage`) | sem sessão |

**Decisão (aprovada pelo utilizador em 2026-10-27):** até a F10 (preferências no servidor), moeda e tema são preferências do dispositivo em `localStorage` e **não** são limpos ao sair; a F10 passa a sincronizá-los por utilizador. Acesso a `localStorage` sempre dentro de `try/catch`. Após o login, as preferências do servidor prevalecem; falha ao gravar mostra erro mas não bloqueia o uso.

### Estado por funcionalidade

Cada funcionalidade guarda, em signals, o resultado das suas consultas com `status` (`loading`, `success`, `empty`, `error`), os dados e os filtros locais (por exemplo, `typeFilter` e `page` em lançamentos, `period` em relatórios, aba em categorias). Estado de formulário é local ao formulário. **Não** se mantém cópia de dados derivados do servidor (saldos, progressos): sempre lidos da API.

### Consultas dependentes de contexto

Consultas são recalculadas quando `referenceMonth` ou `selectedCurrency` mudam, com cancelamento da anterior (FRONTEND-SPEC-03):

| Funcionalidade | Depende de |
|---|---|
| Dashboard, Lançamentos, Metas | mês + moeda |
| Relatórios | mês (âncora) + moeda + período |
| Cofrinhos, categorias | moeda (cofrinhos); nenhuma (categorias) |

### Invalidação após mutações (substitui `reload()` dos notifiers)

| Mutação | Atualiza |
|---|---|
| criar/editar/excluir lançamento | lista, visão geral, progresso de metas, relatórios |
| salvar categoria/subcategoria | categorias, seletores de categoria, metas (lista de categorias de despesa) |
| salvar meta | metas, progresso |
| movimentação ou cofrinho | cofrinhos, resumo de reservas, visão geral (total de reservas) |
| meta geral de reservas | resumo de reservas |
| entrar / sair | tudo é recarregado ou descartado; sair limpa todo o estado em memória |

### Troca de moeda ao salvar (P-15, decidida)

Cada moeda tem os seus lançamentos, despesas e dashboard. Conforme a lógica existente (DESC-CUR-04), salvar um lançamento noutra moeda faz a interface passar a selecionar essa moeda, atualizando `selectedCurrency` e gravando a preferência. É comportamento de cliente.

### Riscos

Estado duplicado entre páginas se cada uma guardar o mês próprio; manter um único proprietário (`core`).

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [FRONTEND-SPEC-03](../26-frontend-integracao-http/spec.md)

## Regras e critérios de aceitação

- [x] Existe exatamente um proprietário de `referenceMonth`, `selectedCurrency` e `themeMode`.
  > ✅ 2026-10-27 — `ReferenceMonthStore`, `CurrencyStore` e `ThemeStore` em `core/state` são os únicos donos. Verificação: `stores.spec.ts`. Desvio: sincronização por utilizador no servidor é da F10.
- [x] Moeda e tema sobrevivem a recarregar a página; o mês volta ao atual.
  > ✅ 2026-10-27 — Moeda e tema persistem em `localStorage` e o mês volta ao atual. Verificação: `stores.spec.ts` (persistência, restauração, valor inválido).
- [ ] Mudar o mês ou a moeda recarrega todas as consultas dependentes e descarta respostas antigas.
  > ⏳ 2026-10-27 — Parcial: ainda não há consultas dependentes. Falta: implementar nas fatias de dados.
- [ ] Cada mutação da tabela atualiza as áreas listadas sem recarregar a página.
  > ⏳ 2026-10-27 — Parcial: ponto de extensão `AppStateReset.register`. Falta: mutações (F4–F9).
- [x] A aplicação funciona (valores padrão) com `localStorage` indisponível.
  > ✅ 2026-10-27 — Verificação: `stores.spec.ts` ("funciona com localStorage indisponível"); acesso sempre em `try/catch` (`safe-storage.ts`).
- [x] Moeda e tema voltam do servidor ao entrar noutro dispositivo.
  > ⏳ 2026-10-27 — Parcial: depende do módulo de preferências. Falta: F10.
  > ✅ 2026-10-29 15:10 +00:00 — `preferences-sync.spec.ts` (depois de entrar o servidor prevalece e não regrava; falha ao ler mantém o do dispositivo) e no navegador contra a API real: preferências EUR/LIGHT gravadas, `localStorage` apagado (outro dispositivo), sessão restaurada → EUR e tema claro aplicados.
- [x] Salvar um lançamento em EUR com BRL selecionado passa a selecionar EUR e mostra os dados de EUR.
  > ⏳ 2026-10-27 — Parcial: depende de lançamentos. Falta: F5.
  > ✅ 2026-10-29 15:10 +00:00 — O diálogo de lançamento já trocava a moeda (F5); desde a F10 essa troca é gravada no servidor pela `PreferencesSync` (`preferences-sync.spec.ts`: trocar a moeda grava `{currency, theme}`).
- [ ] Sair descarta o estado; um segundo utilizador não vê dados do primeiro.
  > ⏳ 2026-10-27 — Parcial: sessão e mês descartados no logout (teste e navegador; rota protegida volta a `/entrar`). Falta: moeda e tema continuam no navegador até a F10 (decisão de preferências de dispositivo, a confirmar); sem dados financeiros para validar a troca de conta.
  > ⏳ 2026-10-29 15:10 +00:00 — `preferences-sync.spec.ts` (`ao sair moeda e tema voltam ao padrão, saem do dispositivo e nada é gravado no servidor`) e no navegador: depois de Sair, `fc.currency` e `fc.theme` somem do `localStorage`, a tela de entrada fica no tema escuro padrão e as preferências do utilizador só voltam no próximo login. Ainda sem teste com dois utilizadores de verdade no mesmo navegador.
- [ ] Nenhum saldo ou progresso é guardado no cliente além do último resultado da API.
  > ⏳ 2026-10-27 — Parcial: sem saldos ainda. Falta: conferir nas fatias de dados.

### Skills de implementação

- **Skills obrigatórias:** `angular-architect`, `angular-module-scaffold`.
- **Caminhos:** `.claude/skills/angular-architect/SKILL.md`; `.claude/skills/angular-module-scaffold/SKILL.md`.
- **Responsabilidade:** signals para estado síncrono, RxJS para fluxos assíncronos, estado com dono claro por funcionalidade.
- **Momento de utilização:** com o shell, antes das páginas.
- **Dependências:** FRONTEND-SPEC-00, 03, 06; BACKEND-SPEC-17.
- **Limitações:** a skill desaconselha bibliotecas de estado sem necessidade concreta.

## Tasks

### Front-end

- [ ] Implementar o estado global em `core` e as consultas dependentes.
  > ⏳ 2026-10-27 — Parcial: estado global (`SessionStore`, mês, moeda, tema) implementado. Falta: consultas dependentes (fatias de dados).
- [ ] Implementar a invalidação por mutação.
  > ⏳ 2026-10-27 — Parcial: `AppStateReset` com callbacks. Falta: invalidação real quando houver mutações.

### Validação

- [ ] Testar persistência, padrões, cancelamento e invalidação.
  > ⏳ 2026-10-27 — Parcial: persistência e padrões testados. Falta: cancelamento e invalidação.

## Resultado esperado

- Estado simples, previsível e sem duplicação, com o mesmo contexto global da origem.
- Preservadas as preferências persistidas e a regra de mês por sessão.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
