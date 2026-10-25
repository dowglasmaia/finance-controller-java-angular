# Contexto Técnico Global

## Objetivo

Orientar a evolução técnica do Finance Controller com decisões claras, responsabilidades bem definidas e mudanças pequenas. Este documento descreve a arquitetura pretendida; não representa uma aplicação já implementada.

## Stack base

| Área | Tecnologia ou abordagem |
|---|---|
| Frontend | Angular 21+ com TypeScript e tipagem estrita. |
| Backend | Java 25 com Spring Boot. |
| Banco de dados | PostgreSQL relacional. |
| Integração | API REST com mensagens JSON. |
| Contratos da API | OpenAPI como descrição compartilhada entre frontend e backend. |
| Plataforma | Aplicação exclusivamente web responsiva. |
| Organização | Monorepo com frontend e backend separados. |

As versões específicas de Spring Boot, PostgreSQL, Node.js e ferramentas de build serão definidas antes da criação da aplicação, respeitando a compatibilidade com Java 25 e Angular 21+.

## Decisões já tomadas

- Atender computadores, tablets e celulares pelo navegador, sem aplicações nativas.
- Usar Java 25, Angular 21+, PostgreSQL e comunicação REST/JSON.
- Adotar arquitetura limpa, organizada em camadas e por responsabilidades.
- Aplicar SOLID e Clean Code com foco em clareza e manutenção.
- Usar padrões de projeto quando resolverem um problema concreto.
- Evoluir por mudanças pequenas e rastreáveis, com uma especificação objetiva por entrega.
- Especificar e aprovar o comportamento antes de implementar.
- Manter contratos e convenções compartilhados com uma única fonte de verdade.

## Organização do sistema

A diretriz inicial é um **monólito modular no backend** e uma aplicação Angular organizada por funcionalidades. A separação em módulos é lógica; não exige serviços independentes nem múltiplas unidades de implantação.

Os agrupamentos funcionais iniciais são:

- Categorias e subcategorias.
- Lançamentos financeiros.
- Orçamento e metas de gastos.
- Cofrinhos e movimentações de reservas.
- Consultas para dashboard e relatórios.
- Backup e importação de dados.
- Preferências da aplicação.

Esses agrupamentos orientam a organização, sem obrigar a criação antecipada de todos os módulos. Dashboard e relatórios podem consultar dados dos demais módulos sem duplicar as suas regras.

Cada módulo deve expor operações claras e preservar os seus detalhes internos. Evitar dependências circulares e acesso direto aos detalhes de persistência de outro módulo.

## Arquitetura do backend

### Camadas e responsabilidades

| Camada | Responsabilidade |
|---|---|
| Apresentação / API | Receber requisições, validar formato, mapear dados de entrada e saída e devolver respostas HTTP. |
| Aplicação | Coordenar casos de uso, verificar pré-condições, definir a unidade de trabalho e chamar o domínio e as portas necessárias. |
| Domínio | Representar conceitos financeiros, invariantes, cálculos e regras de negócio. |
| Infraestrutura | Implementar persistência, acesso ao PostgreSQL e integrações externas. |
| Configuração | Conectar implementações e configurar a execução da aplicação. |

### Direção das dependências

- A apresentação depende da aplicação.
- A aplicação depende do domínio e de contratos que expressam as necessidades dos casos de uso.
- A infraestrutura implementa esses contratos e pode depender dos tipos internos necessários.
- O domínio permanece independente de HTTP, Spring, banco de dados e bibliotecas de persistência.
- A configuração faz a composição das implementações.

O fluxo de execução pode chegar ao banco através de um repositório, mas o domínio não deve depender da implementação desse repositório.

### Regras de implementação

- Controllers não concentram regras financeiras nem consultas diretas ao banco.
- Casos de uso coordenam operações; regras reutilizáveis pertencem ao domínio.
- Interfaces são usadas nas fronteiras em que protegem uma dependência ou permitem substituição útil.
- Não criar uma interface para cada classe nem uma camada que apenas repassa chamadas sem propósito.
- DTOs representam o contrato da API; entidades de persistência não são expostas diretamente.
- Separar modelos quando as responsabilidades e os ciclos de mudança justificarem essa separação.
- Operações que precisam ser atómicas devem ter limites de transação explícitos nos casos de uso, executados pela integração com a infraestrutura.
- Integridade referencial e unicidade também devem ser protegidas no banco quando aplicável, incluindo condições de concorrência.

## Arquitetura do frontend

- Organizar por funcionalidades, mantendo os seus componentes, estado e acesso a dados próximos.
- Componentes cuidam de apresentação e interação.
- Serviços de acesso a dados encapsulam a comunicação HTTP.
- Estado compartilhado tem um proprietário claro; estado de formulário ou de uma tela permanece local quando possível.
- Regras financeiras oficiais são garantidas pelo backend. O frontend pode validar entradas e apresentar prévias sem substituir essa garantia.
- Usar componentes standalone e tipagem estrita em TypeScript e templates.
- Usar signals para estado síncrono e valores derivados; RxJS para fluxos assíncronos e concorrência quando necessário.
- Evitar estado duplicado, subscrições aninhadas e componentes que concentram HTTP, validação, cálculo e apresentação.
- Tratar carregamento, resultado vazio, sucesso e erro de forma explícita.
- Usar componentes e estilos compartilhados apenas quando houver reutilização real.
- Manter interface responsiva, navegação por teclado, rótulos de campos e feedback compreensível.

## SOLID e Clean Code

| Princípio | Aplicação prática |
|---|---|
| Responsabilidade única | Cada componente, serviço ou classe tem um motivo claro para mudar. |
| Aberto/fechado | Isolar comportamentos variáveis quando houver necessidade real de extensão. |
| Substituição | Implementações de um contrato preservam o significado das operações e dos resultados. |
| Segregação de interfaces | Cada consumidor depende apenas das operações de que precisa. |
| Inversão de dependências | Regras centrais dependem de contratos internos, sem conhecer detalhes de infraestrutura. |

Usar nomes ligados ao domínio, funções coesas, composição e imutabilidade quando apropriado. Evitar complexidade escondida, parâmetros booleanos ambíguos, duplicação de regras e comentários que apenas repetem o código.

Clareza tem prioridade sobre código excessivamente compacto. Não impor quantidade arbitrária de classes, métodos ou linhas como medida de qualidade.

## Padrões de projeto

Padrões são ferramentas para resolver problemas identificados, não uma lista obrigatória para cada funcionalidade.

- **Strategy:** considerar quando houver algoritmos ou políticas intercambiáveis com o mesmo contrato e evolução independente. Por exemplo, diferentes formas de agrupamento de relatórios podem justificar estratégias se a complexidade crescer. Um condicional simples continua adequado quando for mais claro.
- **Repository:** abstrair operações de persistência necessárias ao domínio ou aos casos de uso, sem expor detalhes do banco.
- **Adapter:** traduzir contratos e formatos externos para conceitos internos, incluindo integrações e importação.
- **Factory:** considerar quando a criação de um objeto envolver regras ou combinações que não cabem claramente num construtor ou método de criação.
- **Mapper:** separar conversões relevantes entre API, domínio e persistência, sem introduzir uma biblioteca apenas para copiar poucos campos.

Não introduzir frameworks de estratégias, hierarquias de herança ou repositórios genéricos antes de existir benefício demonstrável.

## Padrões de integração

- O frontend consome a API REST do backend.
- Requisições e respostas usam JSON e contratos explícitos.
- O contrato OpenAPI deve refletir operações, campos, validações, erros e exemplos aprovados.
- O mesmo contrato orienta frontend e backend; não manter cópias editadas manualmente em paralelo.
- Validações simples, como campos obrigatórios e formatos, acontecem no cliente para feedback e no servidor para proteção.
- Regras de negócio, integridade e autorização são garantidas no backend.
- Erros de validação, conflitos de negócio e falhas inesperadas têm tratamento padronizado.
- As respostas de erro devem permitir identificar a categoria do problema, um código estável, uma mensagem compreensível e campos afetados quando aplicável.
- O frontend apresenta os erros em linguagem adequada ao utilizador, sem exibir detalhes internos.
- Códigos HTTP, paginação, filtros, ordenação e representação de valores são definidos nos contratos conforme a necessidade de cada operação.
- Alterações de contrato devem considerar o impacto nos dois lados da integração.

## Dados financeiros e persistência

- PostgreSQL é a base relacional do novo produto; o esquema evolui por migrações versionadas.
- Usar representação decimal apropriada para dinheiro, com BigDecimal no Java e tipos decimais no banco.
- Não usar ponto flutuante binário como base de cálculos monetários que exigem precisão decimal.
- Definir escala, arredondamento e representação no JSON antes de implementar cálculos e contratos financeiros.
- Preservar a precisão ao consumir valores no TypeScript; a representação escolhida na API deve considerar essa fronteira.
- Tratar moeda como parte explícita do valor e respeitar a separação entre BRL e EUR.
- Distinguir data financeira de instante de criação ou atualização; locale de apresentação não define o fuso de armazenamento.
- Calcular saldos e totais a partir dos registos de origem, conforme as regras do produto.
- Usar transações nas alterações que precisam ocorrer por completo ou não ocorrer.
- Importações devem validar o conjunto recebido e prever recuperação antes de substituir dados.
- Índices e otimizações devem corresponder às consultas e restrições reais, sem otimização antecipada.

## Contratos e utilitários compartilhados

Centralizar convenções realmente transversais, como erros da API, representação monetária e tratamento de datas.

Manter regras específicas junto da funcionalidade responsável. Não transformar a área compartilhada num depósito de serviços sem dono.

Compartilhar contratos entre Java e TypeScript não significa compartilhar classes de domínio nem reproduzir as mesmas camadas nas duas linguagens.

## Processo de evolução e qualidade

- Cada entrega deve ter uma especificação curta, com objetivo, escopo, regras, critérios de aceitação e limites claros.
- Dividir mudanças grandes em entregas coerentes; não reduzir artificialmente a documentação para encaixar uma alteração demasiado ampla.
- Relacionar implementação e validação aos critérios aprovados.
- Testar regras de domínio, integração com persistência/API e fluxos relevantes da interface.
- Usar testes proporcionais ao risco, incluindo limites monetários, datas e falhas quando afetados.
- Verificar compilação, análise estática e testes pertinentes antes de concluir uma entrega.
- Registar decisões duradouras e manter documentação alinhada ao comportamento entregue.
- Uma aprovação documental não autoriza automaticamente migração ou implementação.

## Restrições

- Manter a solução simples, legível e incremental.
- Evitar abstrações antecipadas, dependências desnecessárias e geração excessiva de código ou documentos.
- Não adotar microserviços, mensageria, CQRS, event sourcing ou microfrontends sem necessidade específica e decisão explícita.
- Não enfraquecer a tipagem para esconder problemas de modelagem ou integração.
- Não colocar regras de negócio em controllers ou templates.
- Não incluir credenciais, segredos ou dados financeiros reais no código e nos testes.
- Não alterar a aplicação de referência como efeito colateral da reconstrução.

## Detalhes ainda a definir

- Versões compatíveis das ferramentas e bibliotecas complementares.
- Ferramentas de build, gestão de dependências, persistência e migração de banco.
- Organização concreta de pacotes e convenções de nomenclatura.
- Autenticação, autorização e eventual isolamento entre utilizadores.
- Execução local ou hospedada, operação sem internet e acesso ao backend.
- Precisão monetária, arredondamento, formato dos valores e semântica de datas na API.
- Política de backup, compatibilidade com dados existentes e recuperação.
- Ambiente de implantação, observabilidade e automação de qualidade.

Essas escolhas devem ser resolvidas quando forem necessárias para uma entrega, sem antecipar toda a infraestrutura.
