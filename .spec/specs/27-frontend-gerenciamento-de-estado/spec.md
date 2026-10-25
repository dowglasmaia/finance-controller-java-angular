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
| `themeMode` | `ThemeModeNotifier` (DESC-PRF-01) | servidor, por utilizador; antes do login, `localStorage` só para o tema | `SYSTEM`; alternar: dark→light, outros→dark |
| `session` (utilizador e *access token*) | novo (P-01) | só memória (nunca `localStorage`) | sem sessão |

Acesso a `localStorage` sempre dentro de `try/catch`. Após o login, as preferências do servidor prevalecem; falha ao gravar mostra erro mas não bloqueia o uso.

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

- [ ] Existe exatamente um proprietário de `referenceMonth`, `selectedCurrency` e `themeMode`.
- [ ] Moeda e tema sobrevivem a recarregar a página; o mês volta ao atual.
- [ ] Mudar o mês ou a moeda recarrega todas as consultas dependentes e descarta respostas antigas.
- [ ] Cada mutação da tabela atualiza as áreas listadas sem recarregar a página.
- [ ] A aplicação funciona (valores padrão) com `localStorage` indisponível.
- [ ] Moeda e tema voltam do servidor ao entrar noutro dispositivo.
- [ ] Salvar um lançamento em EUR com BRL selecionado passa a selecionar EUR e mostra os dados de EUR.
- [ ] Sair descarta o estado; um segundo utilizador não vê dados do primeiro.
- [ ] Nenhum saldo ou progresso é guardado no cliente além do último resultado da API.

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
- [ ] Implementar a invalidação por mutação.

### Validação

- [ ] Testar persistência, padrões, cancelamento e invalidação.

## Resultado esperado

- Estado simples, previsível e sem duplicação, com o mesmo contexto global da origem.
- Preservadas as preferências persistidas e a regra de mês por sessão.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
