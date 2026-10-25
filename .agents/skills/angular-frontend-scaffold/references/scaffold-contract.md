# Contrato do scaffold Angular

O gerador cria `output/project-name` por meio do Angular CLI e interrompe se esse diretório já existir. O comando base é equivalente a:

```text
npx --yes @angular/cli@21 new <project-name> --directory <output/project-name> --routing --style=scss --standalone --strict --zoneless --ssr=false --skip-tests --skip-git --package-manager=npm --defaults --interactive=false --ai-config=none
```

Após a geração do Angular, o gerador instala as dependências de Tailwind CSS:

```text
npm install tailwindcss @tailwindcss/postcss postcss
```

Ele também cria `.postcssrc.json` na raiz:

```json
{
  "plugins": {
    "@tailwindcss/postcss": {}
  }
}
```

Como o projeto usa SCSS, `src/styles.scss` inicia com `@use 'tailwindcss';`. O shell inicial contém um pequeno uso de classes utilitárias para confirmar que Tailwind participa do build. Não é criado `tailwind.config.*`: a integração atual do Tailwind com Angular e PostCSS não exige esse arquivo para o caso inicial.

Depois da geração, ele substitui somente o shell inicial por uma estrutura mínima e funcional:

```text
src/app/
  app.ts
  app.html
  app.scss
  app.config.ts
  app.routes.ts
  core/http/
    api-base-url.token.ts
    api-error.ts
    api-error.interceptor.ts
  features/home/
    doc.md
    home.routes.ts
    pages/home-page.ts
    pages/home-page.html
    pages/home-page.scss
```

O projeto começa com uma única rota lazy (`/`) e uma página estática de boas-vindas. Ela prova a navegação e o build sem inventar funcionalidades de negócio.

`HttpClient` e um interceptor de erro são registrados para estabelecer a fronteira HTTP. Não há serviço que dispare requisições, proxy, URL de servidor ou inicialização de API. A base relativa padrão é `/api` e pode ser trocada quando o contrato e a estratégia de ambiente forem definidos.

O `package.json` termina com apenas estes comandos de execução do projeto:

```json
{
  "scripts": {
    "start": "ng serve --port 4200",
    "build": "ng build"
  }
}
```

O validador confirma a presença das dependências e da configuração de Tailwind e, em seguida, executa apenas `npm run build`. Não existem arquivos de teste gerados e nenhum teste é executado.
