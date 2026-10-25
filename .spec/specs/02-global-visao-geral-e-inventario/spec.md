# 02-global-visao-geral-e-inventario

> **ID:** GLOBAL-SPEC-00 · **Categoria:** Global · **Estado:** proposta para revisão — não aprovada, não executada.

## Objetivo

Fixar a visão geral, o escopo e o inventário funcional da reconstrução do Finance Controller (Flutter → Java/Spring Boot + Angular), para que cada capacidade do produto atual tenha dono, evidência e spec correspondente.

## Escopo

- Inclui:
  - Inventário funcional e técnico das capacidades observadas na origem, com ligação às specs de destino.
  - Fronteira entre o que é preservado, o que é adaptado e o que fica fora.
  - Mapa das specs por categoria e a ordem de leitura.
- Não inclui:
  - Regras detalhadas (ver [GLOBAL-SPEC-02](../04-global-dominio-e-glossario/spec.md) e [BACKEND-SPEC-01](../07-backend-dominio-e-regras-de-negocio/spec.md)).
  - Qualquer implementação. Esta spec não autoriza código nem migração.

## Contexto técnico local

### Origem e destino

| Aspeto | Origem (Flutter) | Destino |
|---|---|---|
| Plataforma | Flutter com mobile, web e PWA (DESC-NAV-01) | Aplicação **somente web responsiva** (memória de produto) |
| Dados | Hive/IndexedDB local por origem, sem servidor (DESC-DAT-01) | PostgreSQL atrás de API REST (contexto técnico global) |
| Estado | Riverpod (`providers.dart`) | Backend como fonte da verdade; Angular com signals |
| Autenticação | Inexistente (DESC-DAT-01) | **E-mail e senha; cada utilizador só vê os seus dados** (P-01, decidida) |
| Offline | Sim, PWA (DESC-INF-01) | **Não** — API web entregue como imagem Docker (P-02, decidida) |
| Dados | locais, por navegador | **Banco novo, sem migração e sem backup de aplicação** (P-03, P-16) |

### Preservado, adaptado, excluído

- **Preservado:** todas as regras de negócio confirmadas (moedas, lançamentos, categorias, metas, cofrinhos, relatórios, backup JSON v2).
- **Adaptado:** cálculos passam para o backend; precisão decimal; datas financeiras; erros padronizados; diálogos de formulário viram páginas/diálogos Angular.
- **Excluído:** layouts e navegação móveis, `app_bottom_nav`, swipe para excluir, build Android/iOS, scripts PowerShell de PWA (DESC-INF-02).
- **Excluído por decisão do produto:** backup (manual e automático), importação, migração de dados da origem, PWA e uso offline (P-02, P-03, P-16).
- **Acrescentado por decisão do produto:** contas por e-mail e senha com isolamento de dados (P-01) e recuperação de senha por e-mail (P-24).
- **Nomes e local:** backend `api-finance-controller` e frontend `app-finance-controller`, na raiz do projeto (P-28).

### Inventário funcional

| ID | Funcionalidade | Descobertas | Specs de backend | Specs de frontend |
|---|---|---|---|---|
| F-01 | Lançamentos (criar, listar, editar, excluir, filtrar) | DESC-TXN-* | BACKEND-SPEC-11 | FRONTEND-SPEC-10 |
| F-02 | Categorias e subcategorias, incluindo seed inicial | DESC-CAT-*, DESC-SUB-*, DESC-SEE-01 | BACKEND-SPEC-10 | FRONTEND-SPEC-11 |
| F-03 | Mês de referência | DESC-MES-01/02 | BACKEND-SPEC-12 | FRONTEND-SPEC-04, 09, 10 |
| F-04 | Saldo mensal e dashboard | DESC-MES-03, DESC-DSH-* | BACKEND-SPEC-12 | FRONTEND-SPEC-09 |
| F-05 | Multimoeda BRL/EUR e seletor global | DESC-CUR-* | todos os módulos | FRONTEND-SPEC-04, 25 |
| F-06 | Metas de gastos (global, mensal, por categoria) | DESC-BUD-01…05, 09…11 | BACKEND-SPEC-13 | FRONTEND-SPEC-13 |
| F-07 | Alerta ao cruzar 80%/100% | DESC-BUD-06…08 | BACKEND-SPEC-13, 11 | FRONTEND-SPEC-10 |
| F-08 | Cofrinhos | DESC-BOX-01, 05, 07, 08, 10, 12 | BACKEND-SPEC-14 | FRONTEND-SPEC-12 |
| F-09 | Movimentações de reserva | DESC-BOX-02…04, 06, 11 | BACKEND-SPEC-14 | FRONTEND-SPEC-12 |
| F-10 | Meta geral de reservas | DESC-BOX-09 | BACKEND-SPEC-14 | FRONTEND-SPEC-12 |
| F-11 | Relatórios (semana/mês/ano) | DESC-REP-* | BACKEND-SPEC-15 | FRONTEND-SPEC-14 |
| F-12 | Backup manual: exportar e importar JSON | DESC-BKP-01…05 | **cancelada** (BACKEND-SPEC-16) | **cancelada** (FRONTEND-SPEC-15) |
| F-13 | Backup automático em pasta | DESC-BKP-06/07 | **excluída** (P-03) | — |
| F-14 | Preferências: tema e moeda | DESC-PRF-01, DESC-CUR-03 | BACKEND-SPEC-17 (servidor, por utilizador) | FRONTEND-SPEC-02, 04 |
| F-15 | Migração de saldo legado de cofrinhos | DESC-BOX-13 | **excluída** (P-16) | — |
| F-16 | PWA e uso offline | DESC-INF-01 | **excluída** (P-02) | — |
| F-17 | Navegação e layout | DESC-NAV-02, DESC-UX-* | — | FRONTEND-SPEC-01, 02, 07 |
| F-18 | Contas: cadastro, entrada, saída; isolamento de dados (novo) | P-01 | BACKEND-SPEC-05 | FRONTEND-SPEC-06 |
| F-19 | Recuperação de senha por e-mail (novo) | P-24 | BACKEND-SPEC-18 (cobre também BACKEND-SPEC-07) | FRONTEND-SPEC-16 |

### Mapa de specs

Ver o [índice geral](../00-engenharia-reversa/README.md): lista completa, ordem de leitura e itens consolidados ou não aplicáveis.

### Riscos

- Divergências entre documentação e código (DESC-CUR-08, DESC-BUD-11, DESC-REP-07) podem levar a requisitos errados se a documentação for tomada como fonte.
- A origem não tem testes de fluxo; comportamentos de interface são **verificados no código, não executados**.

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [Catálogo de descobertas](../00-engenharia-reversa/descobertas.md)
- [Matriz de rastreabilidade](../00-engenharia-reversa/matriz-rastreabilidade.md)
- [Pendências](../00-engenharia-reversa/pendencias.md)

## Regras e critérios de aceitação

- [ ] Cada funcionalidade F-01…F-19 aponta para pelo menos uma spec, ou para o registo da decisão que a exclui.
- [ ] Nenhuma funcionalidade móvel exclusiva consta como requisito.
- [ ] Toda divergência documental listada tem uma regra vigente indicada (código) e uma pendência quando a decisão é humana.
- [ ] O inventário é revisto sempre que uma spec funcional for criada, dividida ou cancelada.

### Skills de implementação

- **Skills obrigatórias:** nenhuma para esta spec (documental).
- **Caminhos:** —
- **Responsabilidade:** —
- **Momento de utilização:** —
- **Dependências:** servir de entrada às specs de módulo.
- **Limitações:** nenhuma.

## Tasks

### Descoberta e decisão

- [ ] Rever o inventário com o responsável pelo produto, já com as decisões de [pendencias.md](../00-engenharia-reversa/pendencias.md) aplicadas.

### Validação

- [ ] Conferir que todo F-xx aparece na [matriz de rastreabilidade](../00-engenharia-reversa/matriz-rastreabilidade.md).

## Resultado esperado

- Inventário único e aprovado, que serve de contrato de escopo para as demais specs.
- Preservado: a memória de produto e o contexto técnico global como referências superiores.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
