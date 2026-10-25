# Implementação orientada a contrato

O contrato OpenAPI aprovado descreve o limite entre frontend e backend. Antes de implementar, confira a operação selecionada, seus parâmetros, corpo, schemas, códigos de resposta, erros e requisitos de segurança.

## Responsabilidades

- O contrato define o formato da requisição e da resposta; a UI não inventa campos ou caminhos.
- Tipos gerados pelo contrato devem ser usados quando já existirem no projeto.
- Um modelo de apresentação pode diferir do DTO quando protege uma necessidade real da interface: agrupamento visual, estado local, rótulo ou conversão de uma representação aprovada.
- A conversão entre DTO e modelo de apresentação deve ficar próxima da fronteira de dados, com nome que explique a diferença.
- Validações de experiência ocorrem no frontend; validações de integridade e regras oficiais continuam no backend.

## Erros e estados

Para cada operação utilizada, confirme como a interface apresenta carregamento, sucesso, resultado vazio válido, erro de validação com campos afetados, conflito de negócio e falha inesperada.

Não converta qualquer erro em uma coleção vazia nem esconda uma resposta inesperada com um cast. Quando a resposta não puder ser confiada ao tipo estático, receba `unknown` e valide a parte necessária antes de a usar.

## Geração e mudanças

Se o projeto gera cliente ou tipos a partir de OpenAPI, use o comando configurado e nunca edite a saída gerada. Alterações no contrato precisam seguir o fluxo de aprovação de contratos, com impacto avaliado nos dois lados.

