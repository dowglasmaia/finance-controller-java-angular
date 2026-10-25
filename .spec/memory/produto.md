# Finance Controller — Memória do produto

## Nome do projeto

**Finance Controller** — aplicação de controle financeiro pessoal.

## Visão geral

O Finance Controller ajuda a organizar receitas, despesas e reservas financeiras. Permite acompanhar o saldo de cada mês, identificar onde o dinheiro é gasto, definir limites de despesas e acompanhar objetivos de poupança.

O novo produto será uma **aplicação exclusivamente web, com interface responsiva**, acessível pelo navegador em computadores, tablets e celulares. Não haverá aplicações nativas para Android ou iOS.

Esta memória resume o contexto funcional identificado no produto atual e o escopo definido para a nova aplicação. As decisões ainda abertas estão reunidas no final.

## Problema que resolve

- Dificuldade de acompanhar quanto entrou, quanto saiu e quanto restou em cada mês.
- Falta de visibilidade sobre as categorias que mais consomem dinheiro.
- Dificuldade de comparar despesas com limites de gastos.
- Confusão entre o saldo mensal e o dinheiro separado para reservas.
- Necessidade de guardar e recuperar os próprios registos financeiros.

## Público

Pessoas que desejam acompanhar e organizar as próprias finanças, com registos de receitas, despesas, metas de gastos e reservas.

## Funcionalidades principais

| Funcionalidade | Objetivo |
|---|---|
| Dashboard | Apresentar o resumo do mês, o acompanhamento das metas e o total em reservas. |
| Lançamentos | Registar, consultar, editar e excluir receitas e despesas. |
| Categorias e subcategorias | Organizar os lançamentos por finalidade. |
| Mês de referência | Consultar meses passados, o mês atual e meses futuros. |
| Moedas | Acompanhar valores em reais e euros de forma independente. |
| Meta de gastos | Definir limites recorrentes, ajustes mensais e limites por categoria. |
| Cofrinhos | Organizar reservas e acompanhar depósitos, saques e metas. |
| Relatórios | Analisar receitas, despesas e distribuição dos gastos por período. |
| Backup | Exportar e importar dados para recuperação ou transferência. |
| Preferências | Escolher moeda e tema claro ou escuro. |

## Regras de negócio

### Lançamentos

- Todo lançamento representa uma entrada ou uma saída.
- Todo lançamento deve ter descrição, valor, data, categoria e moeda.
- O valor deve ser maior que zero. O tipo do lançamento determina se ele aumenta ou diminui o saldo.
- A subcategoria é opcional e pertence à categoria escolhida.
- As observações são opcionais e complementam o lançamento sem alterar sua identidade.
- Lançamentos podem ser editados ou excluídos.
- Datas futuras são permitidas para registar receitas e despesas previstas.
- Um novo lançamento começa com uma data no mês de referência.

### Mês de referência e saldo

- A aplicação inicia com o mês atual selecionado.
- O utilizador pode consultar meses anteriores ou futuros.
- Dashboard, lançamentos e meta de gastos acompanham o mês selecionado.
- O saldo mensal é a soma das entradas menos a soma das saídas daquele mês e daquela moeda.
- O saldo é calculado a partir dos lançamentos, sem um valor independente gravado como saldo.
- Registos futuros participam dos cálculos do seu período; não há distinção de pagamento ou recebimento confirmado nos campos analisados.

### Moedas

- As moedas contempladas são real brasileiro (BRL) e euro (EUR).
- Cada lançamento e cada cofrinho possui uma moeda.
- Valores de moedas diferentes não são somados.
- Não há conversão cambial automática.
- A moeda selecionada orienta dashboard, lançamentos, metas, relatórios e reservas.
- A preferência de moeda é mantida entre utilizações.

### Categorias e subcategorias

- Toda categoria deve ter nome e um tipo: receita, despesa ou ambos.
- A categoria também possui ícone e cor para identificação visual.
- O nome de categoria deve ser único, ignorando maiúsculas, minúsculas e espaços nas extremidades.
- Uma categoria do tipo “ambos” pode ser usada em entradas e saídas.
- Uma categoria com lançamentos vinculados não pode ser excluída.
- Ao excluir uma categoria sem lançamentos vinculados, suas subcategorias também são removidas.
- “Outros” é uma subcategoria padrão protegida contra exclusão.
- A configuração inicial inclui 11 categorias padrão: sete de despesa e quatro de receita.

### Meta de gastos

- A meta de gastos representa um limite desejado para despesas.
- Existe um limite mensal recorrente e podem existir ajustes para meses específicos.
- Um ajuste mensal positivo substitui o limite recorrente naquele mês.
- No comportamento atual, um ajuste ausente, igual a zero ou negativo mantém o limite recorrente.
- Também podem ser definidos limites por categoria.
- As metas e os ajustes são separados por moeda.
- O progresso considera as despesas do mês de referência.
- Sem um limite positivo, não há meta ativa para aquele cálculo.
- Abaixo de 80% do limite, o gasto está dentro da faixa normal.
- A partir de 80% e abaixo de 100%, há situação de aviso.
- A partir de 100%, a meta foi atingida ou ultrapassada.
- Ao salvar uma despesa, o alerta ocorre quando há transição para uma situação pior, sem repetir apenas pela permanência no mesmo estado.
- Se a categoria cruzar um limiar, seu alerta tem prioridade. Caso contrário, a meta global também é verificada.

### Cofrinhos e reservas

- Um cofrinho representa dinheiro separado para uma reserva ou objetivo.
- Todo cofrinho deve ter nome e moeda; também possui ícone e cor.
- A descrição é opcional e informa livremente onde o dinheiro está guardado.
- A meta individual de um cofrinho é opcional.
- Cada movimentação pertence a um cofrinho e representa um depósito ou um saque.
- A movimentação deve ter valor positivo e data; a observação é opcional.
- Depósitos aumentam o saldo do cofrinho e saques o diminuem.
- A moeda da movimentação deve ser igual à moeda do cofrinho.
- Um cofrinho com movimentações não permite alteração de moeda.
- O saldo é calculado pela soma dos depósitos menos a soma dos saques.
- Reservas são acumulativas e independem do mês selecionado.
- Movimentações de reservas não criam automaticamente receitas ou despesas e não alteram o saldo mensal.
- A meta individual acompanha apenas o progresso daquele cofrinho.
- A meta geral de reservas é definida separadamente para cada moeda; não corresponde à soma das metas individuais.
- O total em reservas permanece disponível no dashboard.

### Relatórios

- Os relatórios apresentam entradas, saídas e saldo por semana, mês ou ano.
- Os cálculos consideram a moeda selecionada.
- Os períodos são ancorados no mês de referência.
- A análise inclui evolução de entradas e saídas, distribuição das despesas e categorias com maior gasto.
- No comportamento atual, o relatório semanal considera de domingo a sábado da semana que contém o dia 15 do mês selecionado.

### Backup

- A exportação reúne categorias, subcategorias, lançamentos, metas, cofrinhos e movimentações em JSON.
- A importação substitui os dados existentes; não faz mesclagem automática.
- Registos antigos sem moeda são interpretados como BRL.
- A importação rejeita moedas não suportadas, movimentações sem cofrinho correspondente e moedas incompatíveis entre cofrinho e movimentação.
- O produto atual também oferece backup automático em local escolhido pelo utilizador, condicionado ao suporte e à permissão do navegador.
- A forma de disponibilizar o backup automático na nova aplicação depende da definição de armazenamento e execução.

## Experiência do produto

- Interface web responsiva, adaptada a diferentes tamanhos de tela.
- Informações e navegação consistentes entre computador, tablet e celular.
- Apresentação em português do Brasil, com datas no formato dia/mês/ano.
- Valores formatados conforme a moeda selecionada.
- Tema claro e escuro, com preferência mantida entre utilizações.
- Identidade visual Neon Graphite: grafite e ciano, com distinção visual entre receitas, despesas e avisos.
- Formulários com indicação de campos obrigatórios e mensagens claras de validação.
- Estados de carregamento, ausência de dados e erro compreensíveis.

## Linguagem comum do time

| Termo | Significado |
|---|---|
| **Lançamento / transação** | Registo de uma receita ou despesa. |
| **Entrada / receita** | Valor que contribui positivamente para o saldo do período. |
| **Saída / despesa** | Valor que contribui negativamente para o saldo do período. |
| **Categoria** | Classificação principal de um lançamento. |
| **Subcategoria** | Classificação complementar dentro de uma categoria. |
| **Mês de referência** | Mês escolhido para acompanhar as finanças. |
| **Moeda selecionada** | Real ou euro usado como contexto para consulta dos valores. |
| **Saldo mensal** | Diferença entre receitas e despesas de um mês e de uma moeda. |
| **Meta de gastos / orçamento** | Limite desejado para despesas. |
| **Limite recorrente** | Meta mensal padrão. |
| **Ajuste mensal** | Limite específico que substitui o padrão em determinado mês. |
| **Cofrinho / reserva** | Agrupamento do dinheiro separado para um objetivo. |
| **Movimentação de reserva** | Depósito ou saque vinculado a um cofrinho. |
| **Aporte / depósito** | Valor acrescentado a um cofrinho. |
| **Saque** | Valor retirado de um cofrinho. |
| **Saldo da reserva** | Depósitos menos saques acumulados no cofrinho. |
| **Meta individual** | Objetivo de valor de um cofrinho. |
| **Meta geral de reservas** | Objetivo de valor para o conjunto de reservas de uma moeda. |
| **Descrição do cofrinho** | Informação livre sobre onde o dinheiro está guardado. |
| **Observação** | Informação complementar de um lançamento ou movimentação. |
| **Backup** | Cópia dos dados para recuperação ou transferência. |
| **Importação** | Restauração dos dados a partir de um backup. |

Para evitar ambiguidade, usar “movimentação de reserva” ao falar conjuntamente de depósitos e saques, e “aporte” ao falar especificamente de depósito.

## Decisões pendentes

- **Execução e armazenamento:** definir onde os dados ficam e se a nova aplicação deve funcionar sem internet. O produto atual utiliza armazenamento local e funcionamento offline.
- **Acesso:** definir autenticação, múltiplos utilizadores e eventual partilha de dados.
- **Saques:** decidir se será mantida a possibilidade atual de um cofrinho ficar com saldo negativo.
- **Relatório semanal:** confirmar a referência ao dia 15 ou definir outra forma de escolher a semana.
- **Ajuste mensal:** decidir se será possível desativar a meta apenas num mês, sem alterar o padrão recorrente.
- **Proteção dos dados:** definir precisão monetária, tratamento de datas, compatibilidade de backups e recuperação em caso de falha na importação.

A plataforma já está definida: somente web responsivo. As decisões pendentes complementam as especificações das funcionalidades antes da implementação.
