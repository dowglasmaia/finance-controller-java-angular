---
name: flutter-analyzer
description: Analisa aplicações Flutter existentes para descobrir funcionalidades, navegação, validações, estado e regras de negócio, com evidências rastreáveis para especificação SDD e migração. Use para levantamento funcional e análise de paridade; não para implementar ou corrigir código Flutter, Java ou Angular.
---

# Flutter Analyzer

Transforme o comportamento encontrado no Flutter em descobertas verificáveis para uma migração incremental. Separe o sistema observado do sistema desejado: código existente, documentação e testes não aprovam automaticamente requisitos do destino.

## Escopo e origem

- Para o Finance Controller, consulte [o mapa do projeto](references/finance-controller.md) antes de investigar uma capacidade. O mapa é um ponto de partida datado, não uma especificação vigente.
- Use o caminho de origem fornecido pelo utilizador. Na ausência de outro, procure o projeto irmão `finance_controller` a partir da raiz do monorepo; o caminho original está no mapa. Se não existir, peça a localização sem criar um projeto substituto.
- Leia as instruções aplicáveis nos repositórios de origem e destino. Inspecione `.spec/` e convenções existentes antes de propor onde guardar resultados.
- Registe revisão Git, estado das alterações locais e escopo. Uma revisão Git não identifica mudanças não commitadas: se houver, indique os ficheiros relevantes e use hash de conteúdo quando necessário para reproduzir a evidência.
- Trabalhe por capacidade ou jornada. Para um pedido global, comece por um inventário e indique claramente o que foi aprofundado e o que ficou por analisar.

## Limites da análise

Preserve o Flutter de origem. Esta skill não autoriza correções, regeneração de adapters, instalação de dependências, migração de dados, scaffold, contratos ou implementação Java/Angular.

Por padrão, faça análise estática e apresente o resultado na conversa. Se o pedido autorizar um relatório documental, escreva apenas no destino acordado, respeitando os templates existentes. Não crie `.spec/`, templates ou uma nova organização como efeito colateral. Não marque specs como aprovadas.

Em pedidos estritamente de leitura, não execute testes ou builds que gerem caches ou artefactos. Quando a execução estiver autorizada, use dados sintéticos e ambiente isolado; registe comando, ambiente, resultado e limitações. Não abra nem altere os dados reais da PWA para investigar uma regra.

## Percurso de descoberta

1. **Entrada e navegação:** leia `pubspec.yaml`, inicialização, router e layouts adaptativos. Liste rotas, parâmetros, diálogos, ações de entrada e diferenças entre plataformas.
2. **Jornada do utilizador:** siga cada ação da tela ou diálogo até provider/notifier, serviço, repositório e modelo. Procure também callbacks, validadores e mensagens que expressem restrições fora dos serviços.
3. **Regras e estado:** identifique pré-condições, filtros, defaults, cálculos, transições, efeitos colaterais e tratamento de erros. Distinga estado global, estado local de formulário e valores derivados.
4. **Persistência:** examine campos, adapters Hive, preferências, IDs, relações, defaults legados, importação/exportação e migrações relevantes. Um campo persistido legado não implica uso funcional atual.
5. **Comparação:** confronte código com documentação e testes; compare formulário mobile com diálogo web quando ambos existirem. Não generalize a partir de apenas uma implementação.
6. **Cobertura:** associe os testes encontrados aos comportamentos analisados. A presença de um teste não significa que passou, e um teste de serviço não comprova a jornada completa.

Use `rg` para localizar símbolos e leia o contexto e os chamadores antes de concluir. Não use apenas nomes de ficheiros, comentários ou resultados de pesquisa como prova de um fluxo completo.

## Classificação da evidência

| Estado | Critério |
|---|---|
| Verificado no código | Implementação e caminho relevante inspecionados; delimite a camada e o alcance da conclusão. |
| Teste existente | Teste localizado e lido; execução não presumida. |
| Observado em execução | Cenário executado com entrada, ambiente e resultado registados. |
| Declarado | Informação de documentação ou do utilizador ainda não confirmada no código. |
| Inferido | Interpretação plausível, acompanhada da evidência parcial e da forma de confirmação. |
| Desconhecido | Evidência ausente, contraditória ou insuficiente; explicite a pergunta pendente. |

Uma descoberta pode ter mais de uma evidência classificada. Não transforme ausência numa pesquisa em prova de inexistência. Registe conflitos separadamente e solicite decisão antes de os converter em requisito do destino.

Para cada descoberta relevante, registe:

- ID estável dentro da análise, capacidade e comportamento descrito de forma observável.
- Classificação, repositório, caminho relativo, símbolo e linhas consultadas.
- Revisão Git e, se aplicável, referência às alterações locais.
- Condições de entrada, resultado/erro e efeitos persistidos ou derivados.
- Testes relacionados, estado de execução e lacunas de cobertura.
- Divergências e decisão necessária para a migração.

## Cuidados específicos de interpretação

- Diferencie validação de formulário, invariantes de domínio, checks de repositório e `assert`. Não assuma que um `assert` garante validação em produção.
- Verifique mês de referência, limites de períodos, datas futuras e filtros de moeda nos chamadores, além do serviço de cálculo.
- Distinga totais mensais de reservas acumulativas e metas individuais de metas gerais.
- Confirme alcance e falhas de importações antes de declarar atomicidade, compatibilidade ou ausência de perda de dados.
- Separe comportamento observado, possível defeito e proposta de melhoria. Nenhum deles substitui a decisão explícita sobre o sistema futuro.

## Entrega para SDD

Adapte-se ao template existente; na ausência dele, entregue uma análise concisa com:

1. Escopo, revisão de origem, método e ficheiros efetivamente analisados.
2. Funcionalidades e jornadas, incluindo rotas, entradas, resultados e erros.
3. Regras e validações com os IDs e referências das evidências.
4. Dados, persistência, efeitos colaterais e diferenças mobile/web relevantes.
5. Testes encontrados/executados e comportamentos ainda não confirmados.
6. Divergências, perguntas e candidatos a critérios de aceitação, explicitamente não aprovados.

Use a cadeia `evidência → requisito candidato → critério candidato → teste` quando útil. Referencie documentos existentes em vez de copiar regras para múltiplos lugares. Conclua com o próximo passo de especificação; não inicie implementação como continuação automática da análise.
