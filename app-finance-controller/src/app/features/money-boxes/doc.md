# features/money-boxes

Página `/cofrinhos` (FRONTEND-SPEC-12, fatia F8): resumo das reservas, meta geral, cartões por cofrinho, detalhe com histórico e os diálogos de cofrinho, movimentação e meta geral. Não depende do mês: só da moeda selecionada.

## Jornadas e rotas

| Jornada | Rota | Resultado esperado |
|---|---|---|
| Ver as reservas | `/cofrinhos` (`money-boxes.routes.ts`, sob demanda, `showMonth: false`) | KPIs `Saldo Atual`, `Meta Total` (`—` sem meta), `Progresso` (`N%` ou `—`) e `Reservas`; com meta geral, barra e `N% concluído — {total} de {meta}`; cartões na ordem de criação |
| Novo / editar cofrinho | `Novo Cofrinho`, lápis do cartão | Nome\*, `Onde está guardado (opcional)`, objetivo (opcional, ≥ 0), moeda\* (desabilitada se há aportes), 10 ícones, 12 cores; toast `Cofrinho salvo.` |
| Aporte / saque | `Aporte` e `Saque` do cartão ou do detalhe | `Movimentar {nome}`; alternador Depósito/Saque, valor\*, data, observação; botão `Depositar` (verde) ou `Sacar` (coral); toasts `Aporte registrado.` / `Saque registrado.` |
| Detalhe | nome do cartão | Saldo, barra de meta e lista `Aportes` (mais recentes primeiro, `+`/`−` com cor, nota padrão `Depósito`/`Saque`), editar e excluir por movimentação com confirmação |
| Meta geral | `Definir meta geral` / `Editar meta geral` | `Meta geral de reservas`; vazio remove; toast `Meta geral salva.` |
| Excluir cofrinho | lixeira do cartão | Confirmação com o nome; remove também as movimentações; toast `Cofrinho excluído.` |
| Trocar a moeda | seletor do shell | Recarrega lista e resumo daquela moeda |

## Contrato consumido (`core/api/generated/money-boxes.ts` e `reserves.ts`)

`listMoneyBoxes`, `createMoneyBox`, `updateMoneyBox`, `deleteMoneyBox`, `listMoneyBoxEntries`, `createMoneyBoxEntry`, `updateMoneyBoxEntry`, `deleteMoneyBoxEntry`, `getReservesSummary`, `saveReserveGoal` (`getMoneyBox` não é usado: a lista já traz tudo). Os OpenAPI (`money-boxes.yaml`, `reserves.yaml`) são a fonte de verdade; regenerar com `npm run generate:api`.

## Estado e responsabilidades

- `MoneyBoxesStore` (`@Injectable()` sem `providedIn`, fornecido pela página): lista e resumo dependem da moeda (`CurrencyStore`); trocá-la cancela a consulta anterior (`switchMap`) e mostra o carregando. Depois de qualquer gravação a lista e o resumo recarregam **em silêncio** (a tela não some, os diálogos continuam abertos), e o histórico do cofrinho aberto também. `detail` é sempre a versão mais recente da lista.
- `MoneyBoxesApiService`: cliente sem estado dos dois contratos.
- `MoneyBoxesPage`: compõe KPIs, barra, cartões e os diálogos; guarda só estado de interface (qual diálogo está aberto).
- `ui/`: `MoneyBoxCard` (apresentação), `MoneyBoxDetail`, `MoneyBoxDialog`, `EntryDialog`, `GoalDialog`. `money-box-catalog.ts` define os 10 ícones (subconjunto do catálogo de categorias) e reaproveita as 12 cores.
- Saldo, progresso, total e contagem vêm prontos do servidor; o cliente só formata (`formatMoney`) e arredonda o percentual para exibir (`percentOf`).

## Regras e decisões

- Saldo nunca negativo (P-04): com saldo `≤ 0` o botão `Saque` fica desabilitado, com `title` e o texto `Saldo insuficiente para saque.` ligado por `aria-describedby`. Saque acima do saldo, edição ou exclusão que negativariam o histórico voltam do servidor como `INSUFFICIENT_BALANCE`: aparece no diálogo (saque/edição) ou em toast (exclusão) e nada muda.
- A movimentação herda a moeda do cofrinho; o formulário não pede moeda e o pedido não a envia.
- A moeda de um cofrinho com aportes fica desabilitada. Como o contrato não traz a contagem de movimentações, o diálogo de edição chama `listMoneyBoxEntries` ao abrir e mantém o campo travado até saber; `CURRENCY_LOCKED` do servidor também aparece no diálogo.
- Cofrinho criado noutra moeda: a interface passa a mostrar essa moeda (como nos lançamentos).
- Os botões de editar e excluir movimentações têm rótulo próprio (`Excluir depósito de R$ 1.500,00 em 01/10/2026`) para movimentos do mesmo dia não ficarem ambíguos para leitor de tela; o sinal `+`/`−` repete na forma o que a cor diz.
- Cores: saldos e KPIs de reservas em violeta (`text-reserve`, também a barra da meta geral); depósitos e `Aporte` em azul (`income`) e saques em vermelho (`expense`).
- Movimentações não criam lançamentos: o dashboard só mostra o total em `Reservas`.

## Validação

- `pages/money-boxes-page.spec.ts` (Vitest, 15 testes): KPIs e meta geral, sem meta (`—`), estado vazio, `Saque` desabilitado com saldo 0, criação com valores exatos e recarga, aporte (tipo, valor exato, data, sem moeda), saque recusado com a mensagem e saldo mantido, detalhe com sinais/nota padrão/data e exclusão recusada, detalhe vazio, exclusão do cofrinho com confirmação, meta geral (valor exato; vazio manda `0`), moeda desabilitada com e sem aportes, troca de moeda, erro de carregamento com nova tentativa.
- Verificado em 2026-10-29 no navegador contra a API real com PostgreSQL (tema escuro, desktop): cofrinho `Viagem Portugal` criado (meta 2.000), aporte de 1.500 (`75% de R$ 2.000,00`), saque de 1.600 recusado com a mensagem de saldo insuficiente, saque de 300 (saldo `R$ 1.200,00`, `60%`), detalhe com as duas movimentações, exclusão do depósito recusada com toast, meta geral 4.000 (`30% concluído — R$ 1.200,00 de R$ 4.000,00`) e dashboard com `Reservas R$ 1.200,00` e saldo mensal inalterado. Achado ao verificar e corrigido: rótulos idênticos nos botões de movimentos do mesmo dia.
- Pendente: teclado completo, leitor de tela, tema claro, 320/768 px e contraste; EUR, edição com moeda bloqueada e exclusão do cofrinho só em Vitest.
