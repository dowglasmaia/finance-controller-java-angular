# Checklist de revisão de interface

## Estrutura e conteúdo

- [ ] O título da página deixa claro o contexto e há uma única ação principal quando aplicável.
- [ ] O template usa elementos HTML semânticos e hierarquia correta de títulos.
- [ ] Os estados de carregamento, vazio, erro, sucesso e desativado foram considerados quando aplicáveis.
- [ ] Textos estão em português do Brasil e valores financeiros e datas usam a formatação recebida pela camada de apresentação.

## Layout e responsividade

- [ ] A interface funciona em 320px, 768px, 1024px e 1440px sem scroll horizontal involuntário.
- [ ] Ação principal, filtros e formulários permanecem legíveis e acionáveis em telas estreitas.
- [ ] Cards, tabelas e listas preservam rótulos, valores e ações essenciais em todos os tamanhos.
- [ ] Espaçamento, alinhamento, tipografia e contraste seguem os tokens semânticos existentes.

## Acessibilidade

- [ ] Todos os campos têm rótulos visíveis e os erros são associados ao campo.
- [ ] É possível concluir o fluxo apenas com teclado, com foco visível.
- [ ] A informação não depende somente de cor, inclusive receitas, despesas e alertas.
- [ ] Controles têm nome acessível, estado desativado claro e área de toque adequada.

## Manutenção

- [ ] O template não contém regra de negócio, acesso HTTP ou cálculo financeiro.
- [ ] As classes Tailwind descrevem a necessidade visual sem valores arbitrários desnecessários.
- [ ] Um novo componente compartilhado só foi criado por reutilização real ou comportamento próprio.
