# Engenharia reversa do Finance Controller — índice geral

Documentação produzida por análise estática do projeto Flutter `projetos/finance_controller` (revisão `03f107936fd2ad06a1d83ac3ba179851716c9722`, árvore limpa) para reconstruir a aplicação como backend Java/Spring Boot + frontend Angular.

**Nada aqui foi executado ou implementado.** Todas as specs estão em estado de proposta para revisão. A aprovação documental não autoriza código nem migração ([Como executar](../../shared/como-executar.md)). A spec [01](../01-scaffold-backend-spring-modulith/spec.md) (scaffold do backend) é anterior e já foi executada.

**Segunda atualização (2026-10-24):** recuperação de senha por e-mail especificada (specs 48 e 49); login sem passo extra de confirmação; backend `api-finance-controller` e frontend `app-finance-controller` na raiz do projeto (spec 01 reaberta; a renomeação do backend foi executada e validada); sem backup do banco. Todas as pendências P-01…P-28 têm resposta.

**Atualização (2026-10-24):** o responsável respondeu às pendências. Decisões e interpretações em [pendencias.md](pendencias.md); resumo do impacto: login por e-mail e senha com dados isolados por utilizador, imagens Docker, **sem backup, importação nem migração** (specs 21 e 38 canceladas), saldo de cofrinho nunca negativo, meta de gastos flexível por mês, progresso da meta no dashboard, relatório corrigido, PostgreSQL + Flyway + JPA otimizado, ApexCharts e Lucide, CI mínimo em duas esteiras. Os cabeçalhos das specs continuam "não aprovada, não executada": as decisões foram aplicadas ao texto, mas a aprovação formal de cada spec ainda não foi dada.

## Documentos de apoio

| Documento | Conteúdo |
|---|---|
| [descobertas.md](descobertas.md) | Catálogo de evidências (`DESC-*`) com classificação, caminho de origem e testes |
| [inventario.md](inventario.md) | Inventário funcional e técnico da origem |
| [matriz-rastreabilidade.md](matriz-rastreabilidade.md) | Funcionalidade → evidência → regra → specs → critério → estado |
| [decisoes-adr.md](decisoes-adr.md) | Registo de decisões arquiteturais propostas (`ADR-*`) |
| [pendencias.md](pendencias.md) | Ambiguidades e decisões humanas (`P-*`) |
| [cobertura-e-limitacoes.md](cobertura-e-limitacoes.md) | O que foi e o que não foi analisado; limitações das skills |

## Specs por categoria

O template do destino ([template-mudanca.md](../../templates/template-mudanca.md)) usa pastas numeradas `NN-descricao/spec.md`. Os IDs estáveis do pedido (`GLOBAL-SPEC-00` etc.) constam no cabeçalho de cada spec. Quando um ID não se aplica ou foi consolidado, a tabela abaixo diz onde.

### Global (4)

| ID | Spec |
|---|---|
| GLOBAL-SPEC-00 | [02 visão geral e inventário](../02-global-visao-geral-e-inventario/spec.md) |
| GLOBAL-SPEC-01 | [03 arquitetura e requisitos transversais](../03-global-arquitetura-e-requisitos-transversais/spec.md) |
| GLOBAL-SPEC-02 | [04 domínio e glossário](../04-global-dominio-e-glossario/spec.md) |
| GLOBAL-SPEC-03 | [05 contratos e integração](../05-global-contratos-e-integracao/spec.md) |

### Backend (19 IDs → 18 specs)

| ID | Spec |
|---|---|
| BACKEND-SPEC-00 | [06 arquitetura Java/Spring](../06-backend-arquitetura-java-spring/spec.md) |
| BACKEND-SPEC-01 | [07 domínio e regras](../07-backend-dominio-e-regras-de-negocio/spec.md) |
| BACKEND-SPEC-02 | [08 persistência e migrações](../08-backend-persistencia-e-migracoes/spec.md) |
| BACKEND-SPEC-03 | [09 API REST e OpenAPI](../09-backend-api-rest-e-openapi/spec.md) |
| BACKEND-SPEC-04 | [10 validações, erros e exceções](../10-backend-validacoes-erros-e-excecoes/spec.md) |
| BACKEND-SPEC-05 | [11 autenticação e segurança](../11-backend-autenticacao-e-seguranca/spec.md) (e-mail e senha, isolamento por utilizador) |
| BACKEND-SPEC-06 | [12 casos de uso e serviços](../12-backend-casos-de-uso-e-servicos/spec.md) |
| BACKEND-SPEC-07 | Sem requisito da origem; coberto pela integração de e-mail em [48](../48-backend-recuperacao-de-senha/spec.md) — ver nota na [spec 13](../13-backend-configuracao-logs-e-observabilidade/spec.md) |
| BACKEND-SPEC-08 | [13 configuração, logs e observabilidade](../13-backend-configuracao-logs-e-observabilidade/spec.md) |
| BACKEND-SPEC-09 | [14 testes do backend](../14-backend-testes/spec.md) |
| BACKEND-SPEC-10 | [15 módulo categorias](../15-backend-modulo-categorias/spec.md) |
| BACKEND-SPEC-11 | [16 módulo lançamentos](../16-backend-modulo-lancamentos/spec.md) |
| BACKEND-SPEC-12 | [17 resumo e dashboard](../17-backend-modulo-resumo-e-dashboard/spec.md) |
| BACKEND-SPEC-13 | [18 metas de gastos](../18-backend-modulo-metas-de-gastos/spec.md) |
| BACKEND-SPEC-14 | [19 cofrinhos](../19-backend-modulo-cofrinhos/spec.md) |
| BACKEND-SPEC-15 | [20 relatórios](../20-backend-modulo-relatorios/spec.md) |
| BACKEND-SPEC-16 | [21 backup e importação](../21-backend-modulo-backup-e-importacao/spec.md) — **cancelada** (P-03, P-16) |
| BACKEND-SPEC-17 | [22 preferências](../22-backend-modulo-preferencias/spec.md) (servidor, por utilizador) |
| BACKEND-SPEC-18 | [48 recuperação de senha por e-mail](../48-backend-recuperacao-de-senha/spec.md) (novo) |

### Frontend (17)

| ID | Spec |
|---|---|
| FRONTEND-SPEC-00 | [23 arquitetura Angular](../23-frontend-arquitetura-angular/spec.md) |
| FRONTEND-SPEC-01 | [24 rotas e navegação](../24-frontend-rotas-e-navegacao/spec.md) |
| FRONTEND-SPEC-02 | [25 design system](../25-frontend-design-system-e-componentes/spec.md) |
| FRONTEND-SPEC-03 | [26 integração HTTP](../26-frontend-integracao-http/spec.md) |
| FRONTEND-SPEC-04 | [27 estado](../27-frontend-gerenciamento-de-estado/spec.md) |
| FRONTEND-SPEC-05 | [28 formulários](../28-frontend-formularios-e-validacoes/spec.md) |
| FRONTEND-SPEC-06 | [29 autenticação e proteção de rotas](../29-frontend-autenticacao-e-protecao-de-rotas/spec.md) |
| FRONTEND-SPEC-07 | [30 responsividade e acessibilidade](../30-frontend-responsividade-e-acessibilidade/spec.md) |
| FRONTEND-SPEC-08 | [31 testes](../31-frontend-testes/spec.md) |
| FRONTEND-SPEC-09 | [32 dashboard](../32-frontend-pagina-dashboard/spec.md) |
| FRONTEND-SPEC-10 | [33 lançamentos](../33-frontend-pagina-lancamentos/spec.md) |
| FRONTEND-SPEC-11 | [34 categorias](../34-frontend-pagina-categorias/spec.md) |
| FRONTEND-SPEC-12 | [35 cofrinhos](../35-frontend-pagina-cofrinhos/spec.md) |
| FRONTEND-SPEC-13 | [36 metas](../36-frontend-pagina-metas/spec.md) |
| FRONTEND-SPEC-14 | [37 relatórios](../37-frontend-pagina-relatorios/spec.md) |
| FRONTEND-SPEC-15 | [38 backup](../38-frontend-pagina-backup/spec.md) — **cancelada** (P-03, P-16) |
| FRONTEND-SPEC-16 | [49 recuperação de senha](../49-frontend-recuperacao-de-senha/spec.md) (novo) |

### Infraestrutura (13 IDs → 5 specs consolidadas)

| IDs | Spec | Observação |
|---|---|---|
| INFRA-SPEC-00, 01, 03 | [39 ambientes e execução local](../39-infra-ambientes-e-execucao-local/spec.md) | consolidadas: o conteúdo é curto e evidenciado por uma única análise de ambiente |
| INFRA-SPEC-02, 11 | [40 banco de dados](../40-infra-banco-de-dados/spec.md) | cópia de segurança do banco (11) adiada com a hospedagem |
| INFRA-SPEC-04, 05, 06 | [41 build e execução](../41-infra-build-e-execucao/spec.md) | |
| INFRA-SPEC-07, 08 | [42 CI/CD e deploy](../42-infra-ci-cd-e-deploy/spec.md) | CI mínimo em duas esteiras agora; deploy adiado (P-18) |
| INFRA-SPEC-09, 10, 12 | [43 segurança, logs e operação](../43-infra-seguranca-logs-e-operacao/spec.md) | |

A origem não tinha infraestrutura de servidor (DESC-INF-03); estas specs são PROPOSTO, sem evidência de origem.

### Qualidade (6 IDs → 3 specs)

| IDs | Spec |
|---|---|
| QUALITY-SPEC-00 | [44 estratégia](../44-quality-estrategia/spec.md) |
| QUALITY-SPEC-01, 02, 03 | [45 unitários, integração e contrato](../45-quality-testes-unitarios-integracao-contrato/spec.md) |
| QUALITY-SPEC-04, 05 | [46 E2E, segurança, desempenho e regressão](../46-quality-e2e-seguranca-desempenho-regressao/spec.md) |

### Planejamento (1)

| ID | Spec |
|---|---|
| IMPLEMENTATION-SPEC-00 | [47 plano e sequência](../47-implementation-plano-e-sequencia/spec.md) |

**Total:** 48 specs novas (02–49) cobrindo 60 IDs-base, mais a spec 01 já existente (reaberta para a renomeação do backend — P-28). Duas delas (21 e 38) estão **canceladas**: restam 46 specs ativas.

| Categoria | Specs (ficheiros) |
|---|---|
| Global | 4 |
| Backend | 18 |
| Frontend | 17 |
| Infraestrutura | 5 |
| Qualidade | 3 |
| Planejamento | 1 |
| Apoio (`00-engenharia-reversa`) | 6 documentos |

## <a id="skills"></a>Skills confirmadas

Catálogo pesquisado em `.claude/skills` do destino e `C:\Users\dowgl\.codex\skills`. Cada skill foi lida antes da associação.

| Skill | Caminho | Papel nas specs |
|---|---|---|
| `java-modern-engineering` | `C:\Users\dowgl\.codex\skills\java-modern-engineering\SKILL.md` | modelagem Java 25, invariantes, testes de domínio |
| `java-spring-create-module` | `C:\Users\dowgl\.codex\skills\java-spring-create-module\SKILL.md` | implementar operações OpenAPI selecionadas num módulo; **exige a seleção explícita** de módulo/operação |
| `openapi-rest-contract` | `C:\Users\dowgl\.codex\skills\openapi-rest-contract\SKILL.md` | autoria dos contratos por módulo; **exige prévia e aprovação explícita** antes de criar ficheiros |
| `java-spring-scaffold` | `C:\Users\dowgl\.codex\skills\java-spring-scaffold\SKILL.md` | já aplicada na spec 01; não reutilizada |
| `angular-frontend-scaffold` | `.claude/skills/angular-frontend-scaffold/SKILL.md` | criar o projeto Angular 21; **exige o nome do projeto** |
| `angular-architect` | `.claude/skills/angular-architect/SKILL.md` | arquitetura, estado, formulários, testes |
| `angular-module-scaffold` | `.claude/skills/angular-module-scaffold/SKILL.md` | módulos de funcionalidade com `doc.md`; exige contrato aprovado e seleção |
| `angular-tailwind-ui-specialist` | `.claude/skills/angular-tailwind-ui-specialist/SKILL.md` | UI Tailwind, tokens Neon Graphite, acessibilidade |
| `flutter-analyzer` | `.claude/skills/flutter-analyzer/SKILL.md` | usada nesta análise; sem papel de implementação |

**Indisponíveis nos diretórios exigidos:** skills de banco de dados/migrações, segurança e autenticação, CI/CD e deploy, observabilidade, E2E/qualidade geral e infraestrutura (Docker/orquestração). Cada spec afetada declara essa limitação; nenhuma foi simulada. A skill `analise-juridico-imobiliaria` existe no catálogo Codex mas é irrelevante ao projeto.

## Ordem de leitura recomendada

1. Este índice → [pendencias.md](pendencias.md) → [decisoes-adr.md](decisoes-adr.md).
2. Globais 02–05.
3. Backend 06–14, depois módulos 15–22.
4. Frontend 23–31, depois páginas 32–38.
5. Infra 39–43, qualidade 44–46, plano 47.

A ordem de **implementação** está em [IMPLEMENTATION-SPEC-00](../47-implementation-plano-e-sequencia/spec.md).
