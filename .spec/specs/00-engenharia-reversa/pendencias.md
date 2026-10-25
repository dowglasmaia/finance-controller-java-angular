# Ambiguidades e decisões

Decisões do responsável pelo produto, registadas em 2026-10-24 a partir das respostas no chat e de três esclarecimentos. Onde a resposta foi "escolha a melhor opção" ou "mantenha o existente", a escolha feita pela análise está indicada e pode ser revista. Estado: **Decidida**, **Delegada** (escolha da análise, aguarda objeção), **Adiada**, **Manual** (feita pelo responsável).

Gravidade original: **A** bloqueia esquema ou contrato; **M** altera comportamento; **B** detalhe.

| ID | Tema | Decisão | Estado | Specs afetadas |
|---|---|---|---|---|
| P-01 | Acesso | Autenticação por **e-mail e senha**; cada utilizador acessa **somente os seus dados** (multiutilizador com isolamento; sem partilha). Todas as tabelas de dados têm `owner_id`. | Decidida | 05, 08, 11, 12, 29 e todos os módulos |
| P-02 | Offline / entrega | API web entregue como **imagem Docker**. Sem requisito offline nem PWA. | Decidida | 23, 41 |
| P-03 | Backup | **Sem funcionalidade de backup**; dados guardados no banco SQL. Backup automático em pasta, exportação e importação saem do escopo (confirmado). | Decidida | 21 e 38 canceladas; 40 |
| P-04 | Saque de cofrinho | Mantém histórico; **saldo nunca fica negativo**; saque recusado se o saldo for ≤ 0 **ou** se o valor exceder o saldo; qualquer edição ou exclusão de movimentação que deixasse o saldo negativo também é recusada (interpretação necessária para cumprir "não fica negativo"). | Decidida | 07, 10, 19, 35 |
| P-05 | Âncora semanal | **Manter a regra atual**: semana dom–sáb que contém o dia 15 do mês selecionado. ("30 dias" interpretado como manter o existente — confirmado.) | Decidida | 20, 37 |
| P-06 | Meta num mês | **Flexível**: o ajuste de um mês pode ser um valor, ou "sem meta neste mês" explícito, ou ausente (volta ao padrão). | Decidida | 07, 09, 18, 36 |
| P-07 | Precisão | Padrão Java para dinheiro: `BigDecimal` escala 2 (`NUMERIC(19,2)`). JSON como string decimal (detalhe técnico da análise). | Decidida | 03, 05, 07, 08 |
| P-08 | Contratos | **Layout por módulo** (um OpenAPI por módulo, erros comuns em `common`). | Decidida | 05, 06, 09 |
| P-09 | Correção do relatório | Os módulos novos nascem com a lógica correta: barras e totais do mesmo intervalo. | Decidida | 20, 37 |
| P-10 | Ranking | Adiado o tema 5×7; **mantém-se o comportamento atual (7)** (confirmado). | Decidida | 20, 37 |
| P-11 | Subcategorias | Melhor estratégia, mantendo o que já funciona: excluir subcategoria com lançamentos **desassocia** (o lançamento fica sem subcategoria, como a origem mostrava `—`); nomes duplicados e segundo "Outros" na mesma categoria são recusados. | Delegada | 07, 08, 15, 34 |
| P-12 | Limite de categorias | **Não aplicar** limite. | Decidida | 07, 15 |
| P-13 | Tipo de categoria | O utilizador não definiu. Como saldos e totais são sempre derivados, editar a categoria de um lançamento recalcula tudo automaticamente. Mudar o **tipo** de uma categoria é recusado quando há lançamentos incompatíveis (`CATEGORY_TYPE_CONFLICT`). | Delegada | 07, 10, 15, 34 |
| P-14 | Meta no dashboard | **Mostrar** o progresso da meta global no dashboard web. | Decidida | 17, 32 |
| P-15 | Moeda | Euro e Real; cada moeda tem os seus lançamentos, despesas e dashboard, **conforme a lógica existente** — incluída a troca da moeda selecionada ao salvar um lançamento noutra moeda (DESC-CUR-04). | Decidida | 16, 27, 33 |
| P-16 | Migração | **Nenhuma**. API nova, banco novo, dados novos. | Decidida | 08, 21, 46 |
| P-17 | Ferramentas | PostgreSQL na **versão estável corrente** (verificar no momento); **Flyway**; **JPA com consultas otimizadas**; gráficos: **ApexCharts** (`ng-apexcharts`), escolha do responsável pelo visual moderno (a análise sugeria Chart.js). Detalhes na seção abaixo. | Decidida/Delegada | 06, 08, 23, 25, 26, 31, 45 |
| P-18 | Hospedagem e CI | Hospedagem **flexível, decidida depois** (tudo em Docker). CI mínimo no GitHub Actions **agora**: esteiras de build separadas para backend e frontend; deploy preparado mas sem destino. Provedor de e-mail de produção e domínio remetente entram com a hospedagem. | Decidida | 41, 42, 43, 48 |
| P-19 | Preferências | **No servidor, por utilizador** (há login, então seguem o utilizador entre dispositivos; valor de reserva em `localStorage` apenas antes do login). | Delegada | 22, 27 |
| P-20 | Alerta fora do mês | **Manter o existente**: o progresso e o alerta consideram o mês de referência do cliente; despesa datada fora dele não altera o progresso e não alerta. A API recebe o mês de referência. | Decidida | 16, 18 |
| P-21 | Limites de campo | Padrão de mercado: descrição 120, observações 500, nomes 60, notas de movimentação 200, `goal ≥ 0`, valor máximo `9 999 999 999,99`. | Delegada | 07, 08, 19 |
| P-22 | Ícones e cores | Biblioteca de ícones compatível com Angular: **Lucide** (`lucide-angular`), tree-shakable, com tabela de correspondência dos nomes Material usados na origem; cores guardadas como `#RRGGBB`. Verificar compatibilidade com Angular 21 na implementação. | Delegada | 04, 25 |
| P-23 | Repositório Git | O responsável cria o repositório **manualmente**. Os fluxos de CI vão na raiz dele. | Manual | 42 |

**Estado geral:** todas as pendências P-01…P-28 têm resposta. Restam apenas confirmações técnicas na implementação (compatibilidade das bibliotecas com Angular 21, versão do PostgreSQL, tempo de validade do token de recuperação) e a aprovação formal das specs.

## Decisões geradas pelas decisões anteriores (todas respondidas em 2026-10-24)

| ID | Tema | Pergunta | Recomendação | Grav. |
|---|---|---|---|---|
| P-24 | Contas | **Decidida (2026-10-24):** recuperação de senha por e-mail — o utilizador informa o e-mail da conta, recebe um link e cria nova senha, no padrão de mercado. Especificada nas specs [48](../48-backend-recuperacao-de-senha/spec.md) e [49](../49-frontend-recuperacao-de-senha/spec.md). Verificação de e-mail e exclusão de conta **não foram pedidas** e continuam fora do escopo. | Decidida (recuperação); verificação/exclusão não solicitadas | M |
| P-25 | Autenticação | **Decidida:** não há passo extra de confirmação — e-mail e senha corretos autenticam. O desenho técnico (token de acesso curto + renovação rotativa em cookie `HttpOnly`) permanece a recomendação da análise (ADR-012), sem objeção. | Decidida/Delegada | M |
| P-26 | Dados do desenvolvimento | Como popular dados sintéticos para testes sem backup/importação? | Fixtures e scripts de teste; semeadura de categorias no cadastro | B |
| P-27 | Backup do banco | **Decidida:** fora do escopo — o responsável não tem isso em mente. Sem política de cópia de segurança do PostgreSQL por ora. | Fora do escopo | B |
| P-28 | Nomes e local dos projetos | **Decidida:** frontend `app-finance-controller` e backend `api-finance-controller`, ambos na raiz do projeto (ao lado de `.claude`). Spec 01 reaberta para a renomeação do backend já gerado; spec 23 usa o novo nome no scaffold Angular. | Decidida | B |

## Detalhes de P-17

| Item | Decisão |
|---|---|
| PostgreSQL | versão estável corrente na data da implementação, suportada pelas imagens oficiais; fixar a versão no `compose` e no CI |
| Migrações | Flyway, scripts imutáveis |
| Acesso a dados | JPA/Hibernate com consultas dedicadas por caso de uso (projeções e agregações em JPQL/SQL nativo quando mais eficiente), sem N+1, paginação no banco, índices alinhados às consultas (BACKEND-SPEC-02) |
| Gráficos | ApexCharts (barras duplas e pizza); validar `ng-apexcharts` com Angular 21 antes de adotar |
| Ícones | Lucide |
| Executor de testes Angular | o padrão do Angular 21 no momento (confirmar) |
| E2E | Playwright (recomendado; decidir na spec 46) |
| E-mail em desenvolvimento | servidor SMTP de captura (Mailpit) no `compose`; produção com a hospedagem (P-18) |
| Contrato/tipos TS | tipos gerados do mesmo OpenAPI; ferramenta a escolher na spec 26 |

## Ambiguidades de leitura (a confirmar em execução)

- DESC-REP-03: incoerência das barras — confirmada no código; corrigida por decisão P-09 nos módulos novos.
- DESC-BKP-07 e demais itens de backup: deixaram de ser relevantes (P-03).
- Responsividade da origem web em larguras estreitas — não analisada em execução.
- Conteúdo de `web/sw.js` e `flutter_bootstrap.js` — irrelevante após P-02.
