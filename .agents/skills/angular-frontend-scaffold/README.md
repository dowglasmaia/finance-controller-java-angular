# angular-frontend-scaffold — como usar com uma IA

Skill que **cria o projeto Angular 21 do zero** (shell mínimo, funcional e validado por build). Use **uma vez**, no início do frontend. Você não roda os scripts à mão: pede a uma ferramenta de IA (Claude Code, Codex, Copilot etc.) e informa o nome do projeto. As instruções que a IA segue estão em [SKILL.md](SKILL.md) e [references/scaffold-contract.md](references/scaffold-contract.md).

## 1. Para que serve

| Faz | Não faz |
|---|---|
| Cria o projeto Angular 21 com TypeScript estrito, SCSS, standalone, zoneless e sem SSR | Adicionar uma funcionalidade a um projeto Angular existente (use `angular-module-scaffold`) |
| Configura Tailwind CSS via PostCSS (`.postcssrc.json`) | Criar contrato OpenAPI ou chamar a API |
| Cria o shell: rota lazy `/`, página inicial, `HttpClient` e interceptor de erro tipado | Instalar biblioteca de estado, UI kit, autenticação, proxy ou testes |
| Fixa `npm start` na porta `4200` | Subir servidor, rodar `ng serve`, `npm test` ou requisições HTTP |
| Valida com `npm run build` de produção | Sobrescrever pasta existente (nunca usa `--force`) |

## 2. Pré-requisitos

- Node.js 20.19+ ou 22.12+ e npm.
- Internet (o gerador baixa `@angular/cli@21` e instala dependências).
- Uma **pasta pai que já exista**. O projeto é criado em `<pasta-pai>/<nome-do-projeto>` e essa pasta final **não pode existir**.

## 3. Dados a informar

| Informação | Obrigatória | Observação |
|---|---|---|
| Nome do projeto | **Sim** | Se faltar, a IA pergunta: "Qual é o nome do projeto Angular que devo criar?" e não cria nada antes. Neste monorepo: `app-finance-controller` |
| Pasta pai | **Sim** | Onde o projeto será criado. Neste monorepo, a raiz do repositório |
| Versão do Angular CLI | Não | Padrão `@angular/cli@21`. Só informe outra versão se quiser uma major posterior suportada |

## 4. Como acionar

### Modelo de pedido (serve para qualquer ferramenta)

```text
Use a skill angular-frontend-scaffold para criar o frontend Angular.

- Nome do projeto: app-finance-controller
- Pasta pai: C:\Users\dowgl\OneDrive\Documents\projetos\finance_controller_java_angula

Valide com o build ao final e me mostre o resultado.
```

### Claude Code

A skill fica em `.claude/skills/angular-frontend-scaffold` (este repositório). Abra o Claude Code na raiz e peça:

```text
/angular-frontend-scaffold
Nome: app-finance-controller
```

### Codex e outras ferramentas

Cite a skill pelo nome (`$angular-frontend-scaffold`) ou, se a ferramenta não carregar skills, dê o caminho:

```text
Leia .agents/skills/angular-frontend-scaffold/SKILL.md e siga as instruções
para criar o projeto "app-finance-controller" na raiz do repositório.
```

A ferramenta precisa executar comandos no terminal (`node`, `npm`).

## 5. O que a IA faz, em ordem

1. Confirma o nome e que a pasta de destino não existe.
2. Roda o gerador: `node <skill>/scripts/scaffold.mjs --output <pasta-pai> --project-name <nome>`.
3. Confere o resultado contra o contrato do scaffold.
4. Valida: `node <skill>/scripts/validate.mjs <projeto-gerado>` (instalação, configuração Tailwind e `npm run build`).
5. Informa o local gerado e o resultado do build.

## 6. Estrutura gerada

```text
app-finance-controller/
  .postcssrc.json
  package.json                  scripts: start (porta 4200) e build
  src/styles.scss               começa com @use 'tailwindcss';
  src/app/
    app.ts / app.html / app.scss / app.config.ts / app.routes.ts
    core/http/                  api-base-url.token.ts, api-error.ts, api-error.interceptor.ts
    features/home/              home.routes.ts, doc.md, pages/home-page.*
```

A base da API é `/api` por padrão e só será ajustada quando o contrato e o ambiente forem definidos.

## 7. Depois do scaffold

Para rodar: `cd app-finance-controller`, `npm start` e abra `http://localhost:4200`. Próximos passos: aprovar o contrato com `openapi-rest-contract` e implementar cada módulo com `angular-module-scaffold`.

## 8. Erros comuns

| Sintoma | Causa e solução |
|---|---|
| A IA pergunta o nome | Esperado: informe o nome do projeto |
| "Diretório já existe" | A skill nunca sobrescreve. Escolha outro nome ou remova a pasta por conta própria |
| Falha ao baixar o Angular CLI | Verifique internet, proxy e versão do Node |
| Build falha na validação | A IA deve informar o comando e o erro; não declara sucesso |
