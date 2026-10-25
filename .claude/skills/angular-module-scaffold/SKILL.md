---
name: angular-module-scaffold
description: Implementa ou amplia um módulo Angular 21+ a partir de operações OpenAPI já aprovadas, combinando um padrão estrutural obrigatório com decisões locais justificadas e mantendo um doc.md junto ao código. Use para páginas, rotas, integração REST, estado e testes de um módulo frontend; não use para criar contratos, gerar o projeto Angular ou alterar regras sem especificação aprovada.
---

# Angular module scaffold

Implemente comportamento de frontend definido por contrato em uma aplicação Angular 21+ existente. O OpenAPI é a fonte de verdade para operações HTTP e DTOs; a estrutura atual do projeto define como o módulo é organizado.

## Seleção necessária

Se o pedido não identificar o módulo, a funcionalidade, o recurso, o `operationId` ou a combinação método e rota, peça a seleção antes de alterar código. Leia o contrato e as rotas atuais para oferecer opções concretas quando isso ajudar, mas não escolha a primeira operação em nome do utilizador.

Se não houver contrato OpenAPI, peça a sua localização ou a criação prévia do contrato. Não invente endpoints, DTOs, campos, erros, autorização ou regras de negócio. Esta skill implementa clientes do contrato; a autoria ou alteração de contratos pertence ao processo de OpenAPI.

## Fluxo de trabalho

1. Leia as instruções do repositório, a spec aprovada, a memória do produto, o contexto técnico e o `doc.md` do módulo quando ele já existir. Localize a aplicação Angular, os comandos de validação, as convenções, as rotas e a configuração de geração OpenAPI.
2. Leia [implementação orientada a contrato](references/contract-first.md). Resolva o escopo pelos `operationId`s, tags ou método e rota. Inspecione schemas, códigos de resposta, erros, parâmetros, paginação e segurança aplicáveis.
3. Identifique os tipos gerados ou a forma já adotada para consumir o contrato. Reutilize-os; não replique DTOs em interfaces manuais. Modele tipos de apresentação apenas quando houver diferença real entre API e UI.
4. Crie ou estenda somente o módulo selecionado. Use [estrutura de módulo](references/module-structure.md) para decidir a organização. Não gere uma árvore fixa quando a funcionalidade for pequena.
5. Crie o `doc.md` no diretório raiz do módulo usando [o modelo](assets/doc.md), ou atualize-o quando o módulo já existir. O documento descreve propósito, jornadas, contrato consumido, estado, dependências e validação; ele não substitui o OpenAPI nem copia schemas completos.
6. Implemente a rota, página, componentes específicos, cliente de dados e estado necessários. Mantenha regras oficiais no backend e trate carregamento, vazio, erro e sucesso de modo explícito.
7. Escreva testes proporcionais para os comportamentos alterados e execute as verificações configuradas. Leia [validação](references/verification.md). Corrija falhas introduzidas pela mudança e registe o que não pôde ser executado.

## Padrão híbrido

Leia [o exemplo de módulo](references/module-example.md) antes de criar o primeiro módulo de uma funcionalidade.

### Regras determinísticas

Todo módulo de funcionalidade deve:

- ficar sob `features/<modulo>/`, usando nome em kebab-case;
- conter `<modulo>.routes.ts` e `doc.md` no diretório raiz;
- declarar as suas rotas junto da funcionalidade;
- manter páginas em `pages/` e componentes específicos em `ui/` quando existirem;
- manter o acesso ao contrato, mappers e estado da funcionalidade em `data-access/` quando consumir a API;
- documentar no `doc.md` propósito, jornadas, operações OpenAPI consumidas, responsável pelo estado e validação;
- consumir somente operações e tipos do OpenAPI previamente aprovados;
- tratar carregamento, vazio, sucesso e erro de cada jornada que consome dados;
- atualizar o `doc.md` sempre que a rota, operação, estado, dependência ou validação do módulo mudar.

Não crie diretórios vazios. O padrão define locais e responsabilidades, não uma árvore obrigatória sem conteúdo.

### Decisões do agente

O agente escolhe a menor solução coerente e regista a decisão no `doc.md` quando ela tiver impacto relevante. Exemplos:

- criar ou não `model/`, conforme exista uma diferença real entre modelo de API e de apresentação;
- usar estado local, estado da funcionalidade ou estado compartilhado, conforme os consumidores reais;
- usar Signals para estado síncrono e RxJS para fluxos assíncronos, cancelamento ou concorrência;
- criar componentes em `ui/` apenas quando houver responsabilidade de apresentação reutilizável dentro do módulo;
- carregar uma rota sob demanda quando houver uma rota própria e ganho de separação;
- introduzir um mapper, adapter ou Strategy somente quando uma diferença, variação ou complexidade concreta o justificar.

Não criar stores globais, wrappers, interfaces, mappers ou padrões de projeto apenas para preencher uma estrutura.

## Limites de implementação

- Prefira componentes standalone, tipagem estrita e carregamento sob demanda quando a rota justificar.
- Agrupe código por funcionalidade. `data-access`, `pages` e `ui` são opções, não diretórios obrigatórios.
- Componentes apresentam e emitem intenções; clientes HTTP adaptam o contrato; estado tem um único proprietário claro.
- Use Signals para estado síncrono e derivados. Use RxJS para fluxos assíncronos e concorrência. Não mantenha duas fontes mutáveis para o mesmo dado.
- Reactive Forms tipados são o padrão estável no Angular 21+. Não adote APIs experimentais de formulários ou recursos sem decisão explícita.
- Os tipos genéricos do HttpClient não validam JSON em runtime. Valide respostas externas incertas na fronteira quando o risco justificar.
- Não altere o OpenAPI, o backend ou a aplicação Flutter de referência sob esta skill, salvo autorização explícita e escopo separado.
- Não acrescente bibliotecas de estado, UI, geração, monorepo ou mapeamento sem benefício concreto e autorização compatível com a mudança.

## Resultado esperado

Ao concluir, informe o módulo e as operações cobertas, as rotas e jornadas entregues, o `doc.md` criado ou atualizado, as verificações executadas e qualquer decisão que ainda dependa do utilizador.

