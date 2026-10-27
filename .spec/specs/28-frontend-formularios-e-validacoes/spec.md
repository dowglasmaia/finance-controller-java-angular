# 28-frontend-formularios-e-validacoes

> **ID:** FRONTEND-SPEC-05 · **Categoria:** Frontend · **Estado:** proposta para revisão — não aprovada, não executada.

## Objetivo

Padronizar formulários tipados, máscaras, validações e mensagens, preservando os textos e as regras de preenchimento da origem e complementando-os com os erros devolvidos pelo servidor.

## Escopo

- Inclui: máscara monetária, validadores, formulários por funcionalidade com campos, mensagens e dependências entre campos.
- Não inclui: regras oficiais (backend), layout das páginas.

## Contexto técnico local

### Máscara e interpretação de valores (DESC-TXN-10)

- `MoneyInput`: aceita apenas dígitos, interpretados como centavos (`1` → `0,01`; `123456` → `1.234,56`), prefixo `R$` ou `€` conforme a moeda do formulário, dica `0,00`. Não admite negativos.
- Conversão para o contrato: texto mascarado → string decimal com ponto (`"1234.56"`), sem passar por `number`.
- Edição: preencher o campo com o valor formatado sem símbolo (`1.234,56`).

### Mensagens de validação (preservadas — `validators.dart`)

| Situação | Mensagem |
|---|---|
| campo de texto vazio | `Campo obrigatório` |
| valor vazio | `Informe um valor` |
| valor não numérico | `Valor inválido` |
| valor ≤ 0 | `O valor deve ser maior que zero` |
| categoria não escolhida | `Selecione uma categoria` |

### Formulários

| Formulário | Campos (obrigatório \*) | Dependências e regras |
|---|---|---|
| Lançamento | tipo (ENTRADA/SAÍDA)\*, valor\*, moeda\*, descrição\*, categoria\*, subcategoria, data\*, observações | Trocar o tipo limpa a categoria e a subcategoria incompatíveis; categoria só lista as que aceitam o tipo; subcategoria só após escolher categoria e é limpa ao trocar a categoria; moeda padrão = moeda global (editável); data inicial: agora se o mês de referência é o atual, senão dia 1 do mês de referência; data entre 2000-01-01 e 2100-12-31 (DESC-TXN-06) |
| Categoria | nome\*, tipo\* (Despesa/Receita/Ambos), ícone, cor, subcategorias | Mudar o tipo com lançamentos incompatíveis mostra `CATEGORY_TYPE_CONFLICT` (P-13). Na criação, "Outros" já vem na lista, com cadeado e sem remover; nova subcategoria vazia é ignorada; na edição, adicionar/remover subcategoria grava na hora (DESC-SUB-04); duplicado → erro `Já existe uma categoria com este nome.` |
| Cofrinho | nome\* (dica "Reserva até dezembro de 2026"), moeda\*, "Onde está guardado", objetivo, ícone, cor | Moeda desabilitada com a dica `A moeda não pode mudar enquanto houver aportes.` quando há movimentações (DESC-BOX-05); objetivo ≥ 0 (corrige DESC-BOX-07; P-21, decidida) |
| Movimentação | tipo (Depósito/Saque)\*, valor\*, data\*, observação | Moeda exibida = a do cofrinho, não editável; botão `Depositar` ou `Sacar` com a cor do tipo; com saldo ≤ 0 o tipo Saque fica desabilitado com a explicação "Saldo insuficiente"; o servidor valida o valor (`INSUFFICIENT_BALANCE`) |
| Entrada | e-mail\*, senha\* | formato de e-mail; mensagem genérica em credencial inválida |
| Cadastro | e-mail\*, senha\* (mín. 8), confirmação da senha\* | confirmação igual à senha; erros do servidor por campo |
| Pedir recuperação | e-mail\* | mensagem de confirmação sempre neutra (FRONTEND-SPEC-16) |
| Redefinir senha | nova senha\* (mín. 8), confirmação\* | confirmação igual; token lido da URL e removido dela; token inválido leva a pedir novo link |
| Meta geral de reservas | meta total | Vazio = sem meta; texto de ajuda da origem |
| Meta de gastos | meta padrão, meta do mês (valor, ou caixa "Sem meta neste mês"), meta por categoria | Todos opcionais; vazio remove; a caixa "Sem meta neste mês" desabilita o campo do mês (P-06); ao mudar mês ou moeda os campos recarregam (DESC-BUD-09) |

### Erros do servidor

`fieldErrors` do `ApiError` aparecem junto do campo; `code` sem campo aparece como toast. O formulário permanece aberto e os dados digitados são preservados.

### Acessibilidade

Rótulo visível em todo campo; `aria-describedby` para ajuda e erro; `aria-invalid`; foco no primeiro campo inválido ao enviar; botões desabilitados durante o envio; valor inicial `autofocus` no campo de valor em formulários rápidos (origem usa `autofocus` ao criar).

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [FRONTEND-SPEC-02](../25-frontend-design-system-e-componentes/spec.md)
- [BACKEND-SPEC-04](../10-backend-validacoes-erros-e-excecoes/spec.md)

## Regras e critérios de aceitação

- [ ] Digitar `123456` no campo de valor mostra `1.234,56` e envia `"1234.56"`.
  > ⏳ 2026-10-27 — Parcial: `MoneyInput` não implementado (F5, primeiro formulário com valor).
- [ ] Enviar valor vazio, `0,00` ou texto inválido mostra a mensagem correspondente da tabela e não chama a API.
  > ⏳ 2026-10-27 — Parcial: validadores `requiredValue`, `decimalValue`, `positiveAmount` com as mensagens exatas testados (`validators.spec.ts`). Falta: ligação ao `MoneyInput` e ao formulário de lançamento (F5).
- [ ] Trocar ENTRADA por SAÍDA limpa a categoria se ela não aceita o novo tipo.
  > ⏳ 2026-10-27 — Parcial: formulário de lançamento é da F5.
- [ ] Ao editar um cofrinho com movimentações, o campo de moeda está desabilitado.
  > ⏳ 2026-10-27 — Parcial: formulário de cofrinho é da F8.
- [ ] Um erro `CATEGORY_NAME_DUPLICATED` aparece no campo Nome e mantém o diálogo aberto.
  > ⏳ 2026-10-27 — Parcial: `applyServerErrors` marca erros do servidor por campo (testado). Falta: formulário de categoria (F4).
- [ ] Todo campo tem rótulo acessível e o primeiro inválido recebe o foco.
  > ⏳ 2026-10-27 — Parcial: formulários de entrada e cadastro com rótulo visível, `aria-describedby`/`aria-invalid` e foco no primeiro campo inválido (teste e navegador). Falta: demais formulários.

### Skills de implementação

- **Skills obrigatórias:** `angular-architect`, `angular-module-scaffold`, `angular-tailwind-ui-specialist`.
- **Caminhos:** `.claude/skills/angular-architect/SKILL.md`; `.claude/skills/angular-module-scaffold/SKILL.md`; `.claude/skills/angular-tailwind-ui-specialist/SKILL.md`.
- **Responsabilidade:** Reactive Forms tipados (arquitetura/módulo); aparência, rótulos e acessibilidade dos campos (UI).
- **Momento de utilização:** em cada formulário.
- **Dependências:** FRONTEND-SPEC-02, 03.
- **Limitações:** Signal Forms não adotados (experimentais no 21).

## Tasks

### Front-end

- [ ] Implementar `MoneyInput` e os validadores compartilhados.
  > ⏳ 2026-10-27 — Parcial: validadores compartilhados em `shared/forms/validators.ts`. Falta: `MoneyInput` (F5).
- [ ] Implementar os formulários da tabela e o mapeamento de erros do servidor.
  > ⏳ 2026-10-27 — Parcial: formulários de Entrada e Cadastro e `applyServerErrors` entregues. Falta: Lançamento, Categoria, Cofrinho, Movimentação, metas (F4–F9) e recuperação de senha (F11).

### Validação

- [ ] Testar a máscara, cada mensagem e as dependências entre campos.
  > ⏳ 2026-10-27 — Parcial: cada mensagem dos validadores testada. Falta: máscara e dependências entre campos.

## Resultado esperado

- Formulários consistentes, acessíveis e com as mesmas mensagens da origem.
- Preservada a regra: validar no cliente para feedback e no servidor para proteção.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
