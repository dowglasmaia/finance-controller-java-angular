# 22-backend-modulo-preferencias

> **ID:** BACKEND-SPEC-17 · **Categoria:** Backend (módulo `preferences`) · **Estado:** decisão delegada (P-19 → servidor, por utilizador) — não executada.

## Objetivo

Guardar no servidor, por utilizador, a moeda selecionada e o tema, para que o acompanhem entre dispositivos.

## Escopo

- Inclui: `getPreferences`, `savePreferences`, padrões e validação.
- Não inclui: mês de referência e período do relatório (não persistidos na origem — DESC-MES-01, DESC-REP-08).

## Contexto técnico local

### Origem e decisão

Na origem a moeda (`selected_currency`, padrão BRL) e o tema (`theme_mode`, padrão `system`) viviam no navegador (DESC-CUR-03, DESC-PRF-01). Como há login (P-01), a escolha da análise (P-19, "escolha a melhor opção") é guardá-las no servidor por utilizador (ADR-008); antes de entrar, a interface usa o tema escuro (padrão desde 2026-10-27) ou a escolha guardada no navegador.

### Contrato

`GET /preferences` → `{currency: BRL|EUR, theme: LIGHT|DARK|SYSTEM}` (padrões `BRL`, `DARK` se nunca gravado; decisão de 2026-10-27); `PUT /preferences` com os dois campos; valor desconhecido → 400 `VALIDATION_FAILED`. Tabela `preference` com `owner_id` único (BACKEND-SPEC-02).

### Comportamento relacionado

Alternar o tema segue DESC-PRF-01 (dark→light, qualquer outro→dark). A troca de moeda grava a preferência. A mudança de moeda causada por salvar um lançamento noutra moeda (P-15) também grava.

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [FRONTEND-SPEC-04](../27-frontend-gerenciamento-de-estado/spec.md)

## Regras e critérios de aceitação

- [ ] Um utilizador novo recebe `BRL` e `DARK`.
- [ ] Gravar `EUR` e `DARK` e voltar a ler devolve os mesmos valores, também noutro dispositivo.
- [ ] O utilizador A nunca lê nem altera as preferências do B.
- [ ] Moeda ou tema desconhecidos são rejeitados.

### Skills de implementação

- **Skills obrigatórias:** `java-modern-engineering`, `java-spring-create-module`, `openapi-rest-contract`, `java-persistence-performance`.
- **Caminhos:** `C:\Users\dowgl\.codex\skills\java-modern-engineering\SKILL.md`; `C:\Users\dowgl\.codex\skills\java-spring-create-module\SKILL.md`; `C:\Users\dowgl\.codex\skills\openapi-rest-contract\SKILL.md`; `.claude/skills/java-persistence-performance/SKILL.md`.
- **Responsabilidade:** contrato `preferences` e implementação.
- **Momento de utilização:** depois de `identity`.
- **Dependências:** BACKEND-SPEC-05, 02.
- **Limitações:** nenhuma específica.

## Tasks

### Contrato e dados

- [ ] Prévia aprovada e contrato `preferences`; migração `preference`.

### Back-end

- [ ] Implementar domínio, caso de uso, adaptador e controller.

### Validação

- [ ] Testar padrões, persistência e isolamento.

## Resultado esperado

- Preferências persistentes e privadas por utilizador.
- Preservados os padrões e a persistência de moeda e tema da origem.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
