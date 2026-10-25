# flutter-analyzer — como usar com uma IA

Skill de **engenharia reversa do app Flutter** (somente leitura): descobre funcionalidades, navegação, validações, estado, persistência e regras de negócio, com evidências rastreáveis para virar especificação (SDD) e orientar a migração para Java + Angular. As instruções que a IA segue estão em [SKILL.md](SKILL.md); o mapa do projeto original está em [references/finance-controller.md](references/finance-controller.md).

## 1. Para que serve

| Faz | Não faz |
|---|---|
| Levanta rotas, jornadas, entradas, resultados e erros por capacidade | Corrigir ou implementar código Flutter, Java ou Angular |
| Registra regras com evidência (arquivo, símbolo, linhas, revisão Git) | Aprovar requisitos: tudo sai como **candidato**, nunca aprovado |
| Classifica a evidência (verificado no código, teste existente, declarado, inferido, desconhecido) | Instalar dependências, regenerar adapters ou migrar dados |
| Compara código, documentação e testes; aponta divergências | Abrir ou alterar dados reais da PWA |
| Entrega análise concisa para a spec | Criar `.spec/` ou templates como efeito colateral |

## 2. Pré-requisitos

- Código-fonte do app Flutter acessível. No projeto original: `C:\Users\dowgl\OneDrive\Documents\projetos\finance_controller` (projeto irmão deste repositório).
- Leitura permitida de `AGENTS.md`/`CLAUDE.md` e de `.spec/` do destino.
- Por padrão a análise é **estática**: nada é executado e o resultado vem na conversa. Escrever relatório em arquivo só com autorização e destino indicado.

## 3. Dados a informar

| Informação | Obrigatória | Observação |
|---|---|---|
| Capacidade ou jornada | **Sim** | Ex.: lançamentos, metas, cofrinhos, relatórios, backup. Pedido global começa por inventário |
| Caminho do projeto Flutter | Se não for o irmão padrão | Sem ele, a IA procura `finance_controller` ao lado da raiz |
| Destino do relatório | Opcional | Padrão: conversa. Informe o arquivo para gravar |
| Profundidade | Opcional | Inventário ou detalhamento de regras e testes |

## 4. Como acionar

### Exemplos de pedido

Análise de uma capacidade:

```text
Use a skill flutter-analyzer para analisar a capacidade "metas de gastos" do app Flutter em
C:\Users\dowgl\OneDrive\Documents\projetos\finance_controller.
Quero rotas, validações, cálculos, persistência e testes, com evidência por arquivo e símbolo.
Responda na conversa, sem gravar arquivos.
```

Inventário:

```text
Use a skill flutter-analyzer para fazer o inventário de todas as funcionalidades do app Flutter,
indicando o que foi aprofundado e o que ficou por analisar.
```

Comparação:

```text
Use a skill flutter-analyzer para comparar o formulário de lançamento mobile com o diálogo web
e listar as divergências de validação.
```

### Claude Code / Codex / outras

Claude Code: `/flutter-analyzer` + pedido. Codex: `$flutter-analyzer`. Sem suporte a skills: "Leia `.agents/skills/flutter-analyzer/SKILL.md` e siga as instruções para: ...".

## 5. Como a IA trabalha

1. Registra revisão Git, alterações locais e escopo; consulta o mapa do projeto como ponto de partida (datado, não normativo).
2. **Entrada e navegação:** `pubspec.yaml`, inicialização, router, layouts adaptativos.
3. **Jornada:** da tela ao provider/serviço/repositório/modelo, incluindo validadores e mensagens.
4. **Regras e estado:** pré-condições, filtros, defaults, cálculos, transições, erros.
5. **Persistência:** campos, adapters Hive, preferências, IDs, migrações.
6. **Comparação e cobertura:** código vs documentação vs testes (teste existente não significa que passou).

## 6. O que vem no resultado

1. Escopo, revisão de origem, método e arquivos analisados.
2. Funcionalidades e jornadas (rotas, entradas, resultados, erros).
3. Regras e validações com IDs e evidências.
4. Dados, persistência, efeitos colaterais e diferenças mobile/web.
5. Testes encontrados/executados e o que não foi confirmado.
6. Divergências, perguntas e critérios candidatos (não aprovados).

Cadeia útil: `evidência → requisito candidato → critério candidato → teste`.

## 7. Erros comuns

| Sintoma | Causa e solução |
|---|---|
| "Projeto Flutter não encontrado" | Informe o caminho; a skill não cria projeto substituto |
| Pede para corrigir um bug do Flutter | Fora do escopo: apenas relata |
| Conclui "não existe" | Ausência em uma busca não prova inexistência; peça verificação por chamadores e testes |
| Resultado virou requisito | Não deve: requisitos só mudam com decisão e spec aprovada |
