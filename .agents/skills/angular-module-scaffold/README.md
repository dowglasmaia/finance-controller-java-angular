# angular-module-scaffold — como usar com uma IA

Skill que **implementa (ou amplia) um módulo de funcionalidade Angular** em um projeto que já existe, a partir de operações OpenAPI **já aprovadas**: rotas, páginas, acesso aos dados, estado, testes e um `doc.md` junto ao código. As instruções que a IA segue estão em [SKILL.md](SKILL.md) e [references/](references/).

## 1. Para que serve

| Faz | Não faz |
|---|---|
| Cria `features/<modulo>/` com rotas, páginas, componentes específicos e acesso à API | Criar o projeto Angular (use `angular-frontend-scaffold`) |
| Consome somente operações e tipos do contrato OpenAPI | Escrever ou alterar o contrato (use `openapi-rest-contract`) |
| Trata carregamento, vazio, sucesso e erro de cada jornada | Inventar endpoints, campos ou regras de negócio |
| Cria/atualiza o `doc.md` do módulo (modelo em [assets/doc.md](assets/doc.md)) | Alterar backend ou o app Flutter de referência |
| Escreve testes proporcionais e roda as verificações do projeto | Criar store global, wrappers ou padrões só para preencher estrutura |

O visual (Tailwind, acessibilidade, responsividade) é refinado pela skill `angular-tailwind-ui-specialist`.

## 2. Pré-requisitos

- Projeto Angular 21+ já criado (ex.: `app-finance-controller`).
- Contrato OpenAPI **aprovado** com as operações do módulo (normalmente em `api-finance-controller/src/main/resources/openapi/modules/<modulo>.yaml`).
- Spec do módulo aprovada em `.spec/` quando houver regra de negócio.
- Node.js e dependências instaladas (`npm install`).

## 3. Dados a informar

| Informação | Obrigatória | Observação |
|---|---|---|
| Módulo/recurso a implementar | **Sim** | Módulo, funcionalidade, tag, `operationId` ou método + rota. Se faltar, a IA **pergunta**; ela nunca escolhe por você |
| Caminho do contrato OpenAPI | Se a IA não achar | Sem contrato, a IA pede a localização e não inventa |
| Projeto Angular | Se a IA não estiver aberta nele | Pasta com `package.json` e `angular.json` |
| Jornadas e telas esperadas | Recomendado | Ex.: listar, criar, editar, excluir; filtros; mensagens |
| Regras de interface | Opcional | Ex.: confirmar antes de excluir |

## 4. Como acionar

### Modelo de pedido

```text
Use a skill angular-module-scaffold para implementar um módulo.

- Projeto Angular: app-finance-controller
- Contrato: api-finance-controller/src/main/resources/openapi/modules/categorias.yaml
- Operações: tag "categorias" (listar, criar, editar, excluir)
- Spec: .spec/specs/34-... (página de categorias)

Rode lint, testes e build ao final e me mostre o resultado.
```

### Exemplo curto (Claude Code)

```text
/angular-module-scaffold
Módulo: categorias. Operações: listCategories e createCategory.
```

### Codex e outras ferramentas

Cite `$angular-module-scaffold` ou peça: "Leia `.agents/skills/angular-module-scaffold/SKILL.md` e implemente o módulo `categorias` seguindo o contrato".

## 5. O que a IA faz, em ordem

1. Lê instruções do repositório, spec, memória do produto, contexto técnico e o `doc.md` do módulo (se existir).
2. Resolve o escopo no contrato (`operationId`, tag ou método + rota) e lê schemas, erros, parâmetros e paginação.
3. Reutiliza os tipos gerados ou a forma já adotada para consumir o contrato.
4. Cria ou estende **somente** o módulo selecionado.
5. Cria/atualiza o `doc.md`.
6. Implementa rota, página, componentes, cliente de dados e estado.
7. Escreve testes e roda as verificações configuradas (ver [references/verification.md](references/verification.md)).
8. Reporta módulo, operações cobertas, rotas, verificações e decisões pendentes.

## 6. Estrutura típica

```text
src/app/features/categorias/
  categorias.routes.ts
  doc.md
  pages/            páginas (lista, formulário)
  ui/               componentes específicos, só se houver reuso real
  data-access/      cliente do contrato, mappers e estado
```

Diretórios sem conteúdo não são criados. Regras: componentes standalone, tipagem estrita, Signals para estado síncrono, RxJS para fluxos assíncronos, Reactive Forms tipados.

## 7. Resultado esperado

Módulo funcional e testado, `doc.md` atualizado, relatório de verificação e pendências. Se algum comando falhar, a IA informa o comando e o erro em vez de declarar sucesso.

## 8. Erros comuns

| Sintoma | Causa e solução |
|---|---|
| A IA pergunta qual módulo | Esperado: informe módulo/tag ou `operationId` |
| Pede o contrato | Aprove antes o contrato com `openapi-rest-contract` |
| Quer criar DTOs manuais | Não deve: use os tipos do contrato; peça a correção |
| Build falha em template | Rode o build Angular; `tsc` sozinho não valida templates |
