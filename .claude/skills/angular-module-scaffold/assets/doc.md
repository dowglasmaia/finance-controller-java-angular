# <Nome do módulo>

## Propósito

Uma frase sobre a necessidade do utilizador atendida por este módulo.

## Jornadas e rotas

| Jornada | Rota | Resultado esperado |
|---|---|---|
| Nome da jornada | `/caminho` | Resultado observável para o utilizador. |

## Contrato consumido

| Operação | Finalidade | Estados tratados |
|---|---|---|
| `operationId` ou `MÉTODO /caminho` | Ação realizada pelo módulo. | Carregamento, sucesso, vazio e erros relevantes. |

O OpenAPI é a fonte de verdade para schemas, parâmetros, respostas e erros. Não copiar DTOs completos neste documento.

## Estado e responsabilidades

- Estado local ou compartilhado e seu proprietário.
- Componentes ou páginas que compõem a jornada.
- Adaptações entre contrato e modelo de apresentação, quando existirem.

## Regras e dependências

- Regra de interface ou dependência relevante para este módulo.
- Referência a uma spec aprovada quando houver comportamento de negócio específico.

## Validação

- Testes e verificações que cobrem o módulo.
- Limitações conhecidas ou decisão pendente, quando existirem.

