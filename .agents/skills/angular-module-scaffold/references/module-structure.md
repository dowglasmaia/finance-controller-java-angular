# Estrutura de módulo Angular

Um módulo frontend representa uma funcionalidade ou jornada do produto. Ele não precisa espelhar todas as camadas nem todos os pacotes do backend. O padrão mínimo é obrigatório; os diretórios internos existem somente quando tiverem conteúdo e responsabilidade clara.

## Estrutura possível

```text
features/<modulo>/
├── <modulo>.routes.ts
├── doc.md
├── pages/              # componentes de rota
├── ui/                 # componentes exclusivos da funcionalidade
├── data-access/        # cliente REST, mappers e estado da funcionalidade
└── model/              # tipos de apresentação quando necessários
```

Todo módulo tem `<modulo>.routes.ts` e `doc.md`. Comece pelo menor conjunto necessário. Uma página simples pode ter rota, página, cliente e `doc.md`, sem todos os diretórios acima.

## Limites

- `core/` contém configuração transversal, tratamento HTTP e serviços de infraestrutura comuns.
- `shared/` recebe somente código com reutilização comprovada entre funcionalidades.
- `features/` concentra jornadas, componentes e estado de cada módulo.
- O módulo não importa detalhes internos de outro módulo. Quando há partilha real, exponha uma API pequena ou mova a abstração para um local apropriado.
- Não use um estado global como atalho para uma página isolada.

## Rotas e componentes

Defina rotas junto à funcionalidade e carregue-as sob demanda quando houver rota própria e ganho de separação. Páginas compõem a jornada; componentes em `ui/` recebem dados tipados e emitem intenções. Não permita que componentes de apresentação façam chamadas HTTP por conveniência.

