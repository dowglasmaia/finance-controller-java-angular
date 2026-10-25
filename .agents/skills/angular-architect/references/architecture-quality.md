# Arquitetura, tipagem e qualidade

Estas são recomendações de engenharia para esta skill, não obrigações universais do framework. Adapte-as ao tamanho da mudança e às convenções aprovadas.

## Organização proporcional

Agrupe por funcionalidades em `features/`. Separe apresentação, acesso a dados e regras apenas quando houver responsabilidades distintas. Uma funcionalidade pequena pode ter poucos ficheiros; uma maior pode justificar `ui/`, `data-access/` e `domain/`.

- Domínio puro: TypeScript independente de Angular/HTTP, quando existirem regras que beneficiem desse isolamento.
- Apresentação: componentes, estado da interação, formulários e modelos de apresentação.
- Acesso a dados: clientes HTTP, adaptação de DTOs e persistência autorizada.
- Composição: rotas, providers e ligação entre dependências.
- Partilha: componentes ou utilitários efetivamente reutilizados; não um diretório para tudo o que ainda não tem dono.

Evite ciclos entre funcionalidades. Não reproduza todas as camadas Java no navegador. DTO e modelo de apresentação podem coincidir se não houver divergência; introduza mapper quando ele proteger uma diferença concreta.

## SOLID aplicado

| Princípio | Aplicação útil | Excesso a evitar |
|---|---|---|
| Responsabilidade única | Separar apresentação de transporte HTTP e regras reutilizáveis. | Uma classe por linha ou por operação trivial. |
| Aberto/fechado | Isolar uma política variável quando existirem variantes reais. | Framework de estratégias para uma única regra estável. |
| Substituição | Adaptadores respeitam o mesmo significado de erros, valores e efeitos. | Herança com operações que passam a lançar “não suportado”. |
| Segregação | Um consumidor depende apenas das operações de que necessita. | Serviço global com todas as operações do sistema. |
| Inversão | Introduzir porta e token DI quando é preciso trocar infraestrutura ou proteger domínio. | Interface para cada serviço sem fronteira ou substituição útil. |

Interfaces TypeScript não existem em runtime: uma interface não serve sozinha como token DI. Use `InjectionToken<Port>` ou uma abstração com representação em runtime quando essa fronteira for necessária. Funções puras não precisam de DI.

## Tipagem forte e legibilidade

Configure `compilerOptions.strict: true` e `angularCompilerOptions.strictTemplates: true`. Considere `noUncheckedIndexedAccess`, `exactOptionalPropertyTypes` e `noImplicitOverride` conforme a compatibilidade e o custo de migração; não force alterações globais não relacionadas.

Use uniões discriminadas para estados mutuamente exclusivos; modele ausência com `null` ou campo opcional de forma consistente. Prefira inferência local e tipos explícitos em contratos públicos. `satisfies` ajuda a conferir configurações sem apagar a inferência.

Receba dados não confiáveis como `unknown` e valide antes de usar. Não substitua `any` por um `as Tipo` sem prova. Reduza condicionais aninhadas com retornos antecipados; nomeie ações pelo que fazem. Comentários explicam motivo e restrições, não traduzem cada linha.

Fonte de semântica: [TypeScript strict](https://www.typescriptlang.org/tsconfig/strict.html).

## Evitar e substituir

| Evitar | Preferir e porquê |
|---|---|
| Componente que faz HTTP, parsing, cálculo e apresentação | Separar as responsabilidades que mudam por motivos diferentes. |
| Estado global por conveniência | Estado local ou provider da funcionalidade; global apenas para partilha real. |
| `subscribe` dentro de `subscribe` | Compor operadores com semântica de concorrência explícita. |
| `effect` para manter totais duplicados | Derivação por `computed`. |
| Mutar arrays/objetos de inputs ou signals | Criar novas referências preservando o proprietário do estado. |
| `catchError(() => of([]))` em qualquer erro | Estado de erro distinguível de resultado vazio válido. |
| Casts para esconder erros do compilador | Corrigir o contrato ou fazer narrowing/validação. |
| `setTimeout` para “consertar” renderização | Identificar notificações, ciclo de vida e condição de corrida. |
| Herança de componentes para partilhar utilitários | Composição, funções e diretivas quando há comportamento reutilizável. |
| NgRx, microfrontends, Nx, SSR ou wrappers genéricos por padrão | Introduzir ferramentas apenas com benefício concreto e custo aceite. |
| Barrel exports que criam ciclos | Imports claros e APIs de funcionalidade deliberadas. |
| Testar nomes de métodos privados | Testar resultado, estado visível e contrato observável. |

## Segurança, experiência e desempenho

Guards controlam navegação e UX; autorização deve ser aplicada no servidor. Não confie em campos ocultos, controlos desabilitados ou bundle frontend para proteger dados. Não guarde segredos no código cliente.

Preserve sanitização; não introduza `bypassSecurityTrust*` para silenciar problemas de HTML externo. Decisões sobre tokens, cookies e CSRF dependem da arquitetura aprovada. Fontes: [segurança Angular](https://angular.dev/best-practices/security), [limites dos route guards](https://angular.dev/guide/routing/route-guards).

Use labels associadas a inputs, botões semânticos, mensagens de erro compreensíveis, foco previsível e navegação por teclado. Cubra carregamento, vazio e falha sem remover desnecessariamente o contexto do utilizador.

Carregue funcionalidades sob demanda quando houver ganho real. Considere `@defer` para UI secundária e bibliotecas pesadas; mantenha estados de espera e acessibilidade. Meça antes de otimizar; OnPush não corrige algoritmos caros. Não aplique SSR a um dashboard pessoal apenas por hábito.

## Aplicação ao Finance Controller

Use specs aprovadas para decidir locale, moedas, precisão, datas, mês de referência e comportamento offline. Dados monetários não devem sofrer arredondamento acidental por conversão de uma string decimal da API para `number`. Inteiros em unidades mínimas exigem escala e limites seguros definidos; bibliotecas decimais exigem necessidade concreta.

Não replique saldos persistidos ou regras Flutter só porque existem no legado. Preserve evidências e decida equivalência funcional antes da implementação. Backend local, servidor remoto e PWA offline têm implicações distintas; service worker sozinho não torna uma API disponível offline.

## Validação proporcional

- Lógica: limites, entradas inválidas e casos relevantes à regra.
- HTTP: parâmetros, corpo, decoding e erros com infraestrutura de teste apropriada.
- Componentes: interação, validação e estados visíveis.
- Navegação: rotas, parâmetros e redirects.
- Fluxos críticos: teste integrado em navegador quando justificado.
- Build Angular: confirmar tipos e templates; lint apenas se configurado.

Informe o que foi executado. Não alegue acessibilidade, performance ou paridade completas a partir de um teste unitário.
