# Finance Controller

Aplicação web responsiva de controle financeiro pessoal (BRL e EUR), reconstruída por especificações (SDD). Monorepo:

- `api-finance-controller/`: backend Java 25, Spring Boot 4.1, Spring Modulith, Maven Wrapper, Clean Architecture. PostgreSQL + Flyway + JPA (ainda por entrar).
- `app-finance-controller/`: frontend Angular 21+ (ainda não criado).
- `.spec/`: specs, decisões e roadmap.

## Leia antes de implementar

- `.spec/memory/produto.md` e `.spec/memory/contexto-tecnico.md`

## Regras

- Especificar e aprovar antes de implementar. Alteração de comportamento, contrato ou plano exige autorização.
- Trabalhar só no escopo da spec; marcar tarefa concluída apenas com evidência verificada.
- Todo repositório filtra por `owner_id` (P-01); cada módulo tem teste de isolamento entre utilizadores.
- Dinheiro em `BigDecimal`/decimal, nunca ponto flutuante.
- Um caso de uso por operação (`<Verbo><Substantivo>UseCase`, `@Service`, método `execute`); interfaces só em portas de saída e fronteiras entre módulos.
- Logs estruturados sem dados sensíveis; sem H2, testes de integração com PostgreSQL (Testcontainers).

## Skill de persistência Java

Para tarefas de JPA/Hibernate, SQL, desempenho de consultas, transações, concorrência, PostgreSQL ou Flyway, leia e siga `.agents/skills/java-persistence-performance/SKILL.md` e apenas as referências pertinentes.

