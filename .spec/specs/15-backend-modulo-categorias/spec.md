# 15-backend-modulo-categorias

> **ID:** BACKEND-SPEC-10 · **Categoria:** Backend (módulo `category`) · **Estado:** proposta para revisão — não aprovada, não executada.

## Objetivo

Implementar o módulo de categorias e subcategorias, incluindo a semente inicial, com as regras de unicidade, proteção de "Outros" e bloqueio de exclusão da origem.

## Escopo

- Inclui: operações `listCategories`, `createCategory`, `getCategory`, `updateCategory`, `deleteCategory`, `addSubcategory`, `deleteSubcategory`; semente de 11 categorias; SPI `CategoryUsageGuard`.
- Não inclui: lançamentos (BACKEND-SPEC-11), metas por categoria (BACKEND-SPEC-13), interface (FRONTEND-SPEC-11).

## Contexto técnico local

### Regras aplicadas (BACKEND-SPEC-01)

RN-CAT-01…11 (decididas: P-11 desassocia lançamentos ao excluir subcategoria; P-12 sem limite de categorias; P-13 bloqueia mudar o tipo com lançamentos incompatíveis) e RN-OWN (cada utilizador tem as suas categorias, com nome único por utilizador). Evidências: DESC-CAT-01…06, DESC-SUB-01…04, DESC-SEE-01.

### Fluxos

- **Criar:** valida nome (obrigatório, único ignorando caixa/espaços), tipo, ícone, cor → cria categoria e "Outros" (+ subcategorias informadas, ignorando "Outros" duplicada) numa transação → 201.
- **Atualizar:** nome/tipo/ícone/cor; unicidade excluindo o próprio id. Alterar o tipo com lançamentos incompatíveis é recusado com `CATEGORY_TYPE_CONFLICT` (a origem permitia — DESC-CAT-06; P-13). Alterar a categoria de um lançamento é livre e os totais, sendo derivados, acompanham.
- **Excluir:** `CategoryUsageGuard.isInUse(id)`; se em uso → `CATEGORY_IN_USE`; senão remove categoria e subcategorias (cascata).
- **Subcategoria:** adicionar valida nome não vazio e unicidade na categoria; excluir recusa "Outros" (`SUBCATEGORY_PROTECTED`); com lançamentos vinculados, desassocia-os (ficam sem subcategoria — P-11). Não há renomear na origem (DESC-SUB-01); manter.
- **Semente:** 11 categorias (despesa: Carro, Alimentação, Moradia, Saúde, Lazer, Educação, Vestuário; receita: Salário, Vale Alimentação, Investimento, Bônus), cada uma com as subcategorias de `seed_service.dart::_data` e "Outros"; ícones e cores da origem; idempotente; executada no **cadastro de cada utilizador** (BACKEND-SPEC-05), na mesma transação.

### Dados de semente (DESC-SEE-01)

| Categoria | Tipo | Ícone | Subcategorias (além de "Outros" em todas) |
|---|---|---|---|
| Carro | EXPENSE | `directions_car` | Combustível, Manutenção, Seguro, Multas |
| Alimentação | EXPENSE | `restaurant` | Supermercado, Restaurante, Delivery, Padaria |
| Moradia | EXPENSE | `home` | Aluguel, Condomínio, Energia, Água, Internet |
| Saúde | EXPENSE | `favorite` | Médico, Farmácia, Plano de Saúde, Academia |
| Lazer | EXPENSE | `sports_esports` | Cinema, Viagem, Streaming, Esporte |
| Educação | EXPENSE | `school` | Curso, Livros, Assinatura |
| Vestuário | EXPENSE | `checkroom` | Roupas, Calçados, Acessórios |
| Salário | INCOME | `work` | CLT, PJ, Freelance |
| Vale Alimentação | INCOME | `lunch_dining` | VA, VR |
| Investimento | INCOME | `trending_up` | Dividendos, Renda Fixa, FIIs |
| Bônus | INCOME | `card_giftcard` | PLR, Comissão |

Cores: Carro laranja, Alimentação verde, Moradia azul, Saúde vermelho, Lazer roxo, Educação índigo, Vestuário marrom, Salário teal, Vale Alimentação verde-claro, Investimento âmbar, Bônus ciano (`Colors.*` do Flutter; conversão para `#RRGGBB` na implementação — GLOBAL-SPEC-02).

### Riscos

Ordenação alfabética sem distinguir caixa usa o `collation` do banco; fixar para evitar diferenças por ambiente.

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [BACKEND-SPEC-01](../07-backend-dominio-e-regras-de-negocio/spec.md)
- [BACKEND-SPEC-03](../09-backend-api-rest-e-openapi/spec.md)

## Regras e critérios de aceitação

- [ ] Criar categoria com nome já existente em outra caixa (`alimentação`) devolve 409 `CATEGORY_NAME_DUPLICATED`; duas criações concorrentes com o mesmo nome resultam em uma criação e um 409.
- [ ] Toda categoria criada tem exatamente uma subcategoria "Outros".
- [ ] `DELETE` de "Outros" devolve 409 `SUBCATEGORY_PROTECTED`.
- [ ] `DELETE` de categoria com lançamentos devolve 409 `CATEGORY_IN_USE` e nada é removido; sem lançamentos remove também as subcategorias.
- [ ] `listCategories?type=EXPENSE` inclui `EXPENSE` e `BOTH`; ordem alfabética sem distinguir caixa.
- [ ] Ao cadastrar um utilizador existem 11 categorias dele (7 despesa, 4 receita) com as subcategorias da tabela; outro utilizador não as vê.
- [ ] Dois utilizadores podem ter categorias com o mesmo nome.
- [ ] Mudar o tipo de uma categoria com lançamentos incompatíveis devolve 409 `CATEGORY_TYPE_CONFLICT`.
- [ ] Criar uma 51.ª categoria é permitido (sem limite).
- [ ] Excluir subcategoria com lançamentos mantém os lançamentos, agora sem subcategoria.

### Cenários de teste

Unicidade (caixa, espaços, concorrência); "Outros" (excluir, duplicar, renomear); exclusão com/sem lançamentos; filtro por tipo; semente idempotente; integração com `ModularityTests` (sem dependência de `transaction`).

### Skills de implementação

- **Skills obrigatórias:** `java-modern-engineering`, `java-spring-create-module`; `openapi-rest-contract` para o contrato do módulo.
- **Caminhos:** `C:\Users\dowgl\.codex\skills\java-modern-engineering\SKILL.md`; `C:\Users\dowgl\.codex\skills\java-spring-create-module\SKILL.md`; `C:\Users\dowgl\.codex\skills\openapi-rest-contract\SKILL.md`.
- **Responsabilidade:** contrato `categories`; implementação das 7 operações; modelagem Java.
- **Momento de utilização:** contrato primeiro, depois implementação, quando o módulo for selecionado.
- **Dependências:** BACKEND-SPEC-00/01/02/03/04/05.
- **Limitações:** nenhuma específica.

## Tasks

### Contrato e dados

- [ ] Aprovar a prévia e gerar o contrato `categories`.
- [ ] Migrações `category` e `subcategory` e semente.

### Back-end

- [ ] Implementar domínio, casos de uso, adaptador e controller das 7 operações e a SPI.

### Validação

- [ ] Executar os testes de unicidade, exclusão e semente com PostgreSQL real.

## Resultado esperado

- Gestão de categorias equivalente à origem, com unicidade garantida no banco.
- Preservada a independência: `category` não depende de `transaction`.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
