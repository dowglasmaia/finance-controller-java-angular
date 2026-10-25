# 06-backend-arquitetura-java-spring

> **ID:** BACKEND-SPEC-00 · **Categoria:** Backend · **Estado:** proposta para revisão — não aprovada, não executada.

## Objetivo

Definir a arquitetura e os padrões do backend Java/Spring Boot do Finance Controller sobre o scaffold já criado, para que todos os módulos de funcionalidade sigam a mesma estrutura de camadas, fronteiras e transações.

## Escopo

- Inclui: estrutura de pacotes por módulo, camadas, dependências entre módulos, transações, mapeamento, padrões de projeto e convenções.
- Não inclui: regras de negócio (BACKEND-SPEC-01), tabelas (BACKEND-SPEC-02), endpoints (BACKEND-SPEC-03). Não recria o projeto: o scaffold é a [spec 01](../01-scaffold-backend-spring-modulith/spec.md), concluída.

## Contexto técnico local

### Ponto de partida (CONFIRMADO na spec 01)

Projeto `api-finance-controller` (na raiz do repositório, movido de `backend/finance-controller` em 2026-10-24 — P-28): Java 25, Spring Boot 4.1.1, Spring Modulith 2.1.1, Maven Wrapper, módulo `example` (`api`, `application/{usecase,service,port/out}` — o `service` será removido ao apagar o módulo `example` na F4, adotando a convenção abaixo, `domain/model`, `infrastructure/persistence`), contrato em `src/main/resources/openapi/openapi.yaml`, pacote `generated` como módulo Modulith aberto, Actuator (`health`, `info`, `metrics`), sem banco.

### Estrutura de cada módulo (PROPOSTO)

```
com.dowglasmaia.financecontroller.<modulo>/
  api/              controllers que implementam as interfaces geradas + mappers DTO↔domínio
  application/
    usecase/        uma classe concreta por caso de uso (`<Verbo><Nome>UseCase`, `@Service`, não `final`, método `execute`); limite transacional (`@Transactional`)
    port/out/       interfaces de saída (repositórios, SPIs)
  domain/           records, enums, invariantes, cálculos — sem Spring, HTTP, JPA ou DTOs gerados
  infrastructure/   adaptadores de persistência e outras integrações
  package-info.java @ApplicationModule + @NamedInterface para o que é público
```

- Interfaces só existem em fronteiras reais: portas de saída (`port/out`) e a API pública que o módulo expõe a outros módulos via `@NamedInterface` (decisão de 2026-10-25: sem interface por caso de uso dentro do módulo, sem sufixo `Service`). O resto é interno.
- Domínio imutável sempre que possível (records); entidades JPA ficam em `infrastructure` e **não** são o domínio (contexto técnico global).
- Persistência: JPA/Hibernate nos adaptadores, com consultas otimizadas por caso de uso, e Flyway para migrações (P-17, decidido). Todo repositório filtra por `owner_id` (P-01) por meio de um ponto único, para que nenhuma consulta o esqueça.
- Módulo `identity` fornece o proprietário autenticado aos demais; o módulo `backup` não existe (P-03).
- Mapeadores manuais, sem biblioteca (poucos campos por tipo).

### Dependências entre módulos

Conforme [GLOBAL-SPEC-01](../03-global-arquitetura-e-requisitos-transversais/spec.md): `transaction→category`, `budget→{category,transaction}`, `report→{category,transaction,moneybox,budget}`. SPIs invertidas: `CategoryUsageGuard` e `BudgetImpactPort`. `ModularityTests` deve permanecer verde.

### Transações e consistência

- `@Transactional` nos serviços de aplicação, nunca em controllers nem no domínio.
- Atómicas: excluir cofrinho (movimentações + cofrinho), registar saque com verificação de saldo, salvar meta (global + categorias + ajuste), criar categoria com subcategorias, cadastrar utilizador com as 11 categorias padrão.
- Restrições de unicidade e chaves estrangeiras também no banco (RT-07).

### Padrões de projeto (somente com problema concreto)

- **Repository** (portas de saída) em todos os módulos.
- **Adapter** para mapear entidades JPA ↔ domínio e DTOs gerados ↔ domínio.
- **Strategy** apenas se os agrupamentos de relatório ganharem variações além de semana/mês/ano (DESC-REP-04); hoje um `switch` sobre `ReportPeriod` basta.
- **Factory** para `MoneyBox`/`Category` apenas se a criação (ex.: "Outros" automática) deixar de caber num método estático.

### Convenções Java 25

Records para valores e DTOs de domínio, `sealed`/pattern matching apenas se esclarecerem resultados. Sem recursos Preview. Construtores com validação de invariantes. Nomes de domínio em inglês; mensagens de erro em português do Brasil.

### Substituição do módulo de exemplo

O módulo `example` e o endpoint `GET /api/v1/examples/{id}` existem só como verificação do scaffold. Devem ser removidos quando o primeiro módulo real tiver substituído a sua função de verificação, sem deixar contrato órfão.

### Decisões e riscos

ADR-001, ADR-004, ADR-005. Risco: o `ModularityTests` aceita o módulo `generated` aberto; com contratos por módulo (ADR-004) cada módulo ganha pacotes gerados próprios e o módulo aberto deixa de ser necessário.

## Referências

- [Produto](../../memory/produto.md)
- [Contexto técnico global](../../memory/contexto-tecnico.md)
- [Como executar](../../shared/como-executar.md)
- [Scaffold](../01-scaffold-backend-spring-modulith/spec.md)
- [GLOBAL-SPEC-01](../03-global-arquitetura-e-requisitos-transversais/spec.md)

## Regras e critérios de aceitação

- [ ] Todo módulo segue a estrutura acima e declara `@ApplicationModule` com interface nomeada.
- [ ] O domínio de cada módulo compila sem dependência de Spring, JPA ou tipos de `generated`.
- [ ] `ModularityTests` passa e não há dependência cíclica.
- [ ] Controllers só mapeiam e delegam; nenhuma regra financeira neles.
- [ ] Operações atómicas listadas têm teste de falha parcial que comprova o rollback.
- [ ] O módulo `example` foi removido com o seu contrato e testes.

### Skills de implementação

- **Skills obrigatórias:** `java-modern-engineering`, `java-spring-create-module`.
- **Caminhos:** `C:\Users\dowgl\.codex\skills\java-modern-engineering\SKILL.md`; `C:\Users\dowgl\.codex\skills\java-spring-create-module\SKILL.md`.
- **Responsabilidade:** `java-modern-engineering` define a modelagem e o estilo Java 25 respeitando a arquitetura existente (Modo A); `java-spring-create-module` implementa cada módulo a partir de operações OpenAPI **selecionadas explicitamente**.
- **Momento de utilização:** em cada spec de módulo backend (10–22), depois do contrato aprovado.
- **Dependências:** spec 01 concluída; contratos de GLOBAL-SPEC-03; ADR-004 aprovada.
- **Limitações:** a skill de módulo pede a seleção de módulo/operação e não cria contratos; `java-spring-scaffold` não é reutilizada aqui (só gera projeto novo).

## Tasks

### Descoberta e decisão

- [ ] Confirmar ADR-001 (dependências entre módulos); ADR-004 (contratos por módulo) e P-17 (JPA, Flyway) já estão decididas.

### Back-end

- [ ] Criar o esqueleto de pacotes e `package-info` do primeiro módulo real seguindo a estrutura.
- [ ] Remover o módulo `example` e o seu contrato quando substituído.

### Validação

- [ ] Executar `mvnw clean verify` incluindo `ModularityTests` e registar o resultado.

## Resultado esperado

- Estrutura uniforme e verificável por teste para todos os módulos.
- Preservados o scaffold e as decisões do contexto técnico global.

## Encerramento

Esta mudança termina quando os critérios de aceitação forem atendidos, as tarefas ativas estiverem marcadas com evidência e as verificações pertinentes forem concluídas, conforme [Como executar](../../shared/como-executar.md).
