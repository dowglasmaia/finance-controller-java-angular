# Cobertura e limitações

## Método

Inspeção estática da origem `projetos/finance_controller` (Flutter), revisão `03f107936fd2ad06a1d83ac3ba179851716c9722`, `git status` sem alterações locais. Foram lidos modelos, repositórios, serviços, `providers.dart`, roteador, ecrã web e diálogos das sete áreas funcionais, `app_constants`, utilitários, tema, testes e documentação. Nenhum teste, build, servidor ou comando de escrita foi executado na origem, que permanece inalterada. No destino foram criados apenas ficheiros em `.spec/specs/`.

## Cobertura

| Área | Estado |
|---|---|
| Domínio e regras (moeda, lançamentos, categorias, metas, cofrinhos, relatórios, backup) | analisada em profundidade no código; evidências em `descobertas.md` |
| Estado e persistência (Riverpod, Hive, preferências) | analisada |
| Telas web de lançamentos, categorias, cofrinhos, relatórios, dashboard | analisadas |
| Metas e backup (telas compartilhadas com o mobile) | analisadas |
| Testes (7 ficheiros) | lidos, não executados |
| Documentação da origem | lida; três divergências registadas |
| Infraestrutura da origem | listada (scripts, PWA, ausência de CI/logs/rede) |
| Backend e frontend de destino | especificados; nada implementado |

## Não analisado ou analisado parcialmente

| Item | Motivo / efeito |
|---|---|
| Telas móveis e navegação inferior | fora do escopo (só web); usadas apenas para confirmar lógica compartilhada (`home_screen` para o `BudgetCard`) |
| `web/sw.js`, `flutter_bootstrap.js`, `manifest.json` | listados, conteúdo não analisado em detalhe; afeta só a decisão offline (P-02) |
| `auto_backup_io_web.dart` | lidas as primeiras ~120 linhas e o resumo do cabeçalho; o comportamento do backup automático vem de `providers.dart` e `docs/funcionalidades.md` |
| `*.g.dart` (adaptadores Hive gerados) | não lidos; regras de leitura tolerante vêm de `app_currency.dart` e `docs/moedas-e-pwa.md` |
| `app_theme.dart`, `app_dimens.dart`, widgets de cartões e `icon_catalog` além dos nomes | tokens principais lidos em `app_colors.dart` |
| `docs/guia-de-uso.md`, `PROMPT.md`, `README.md`, `CLAUDE.md` da origem | não lidos; `AGENTS.md`, `funcionalidades`, `regras`, `arquitetura`, `moedas-e-pwa`, `relatorios`, `metas-orcamento` foram lidos |
| `app_desktop/web/` (build compilado) | artefacto gerado, ignorado |
| Execução da aplicação | não realizada; comportamentos de interface são "verificado no código" |
| Responsividade real da origem web | não analisada em execução |

Portanto **não se declara cobertura total**: a cobertura funcional das regras é alta e rastreável, mas a paridade de interface só será provada pelos testes E2E propostos.

## Limitações de skills

Confirmadas e lidas: `java-modern-engineering`, `java-spring-create-module`, `openapi-rest-contract`, `java-spring-scaffold`, `angular-architect`, `angular-module-scaffold`, `angular-frontend-scaffold`, `angular-tailwind-ui-specialist` e `flutter-analyzer`. As referências detalhadas dentro das skills (por exemplo `references/*.md`) foram consultadas apenas quando necessário a uma afirmação (`java-spring-scaffold`: `conventions.md`, `contract-first.md`, `defaults.json`, `dependencies.json`, `input-validator.mjs`; os restantes SKILL.md).

Não existem, nos diretórios `.claude/skills` do destino e `C:\Users\dowgl\.codex\skills`, skills para: banco de dados e migrações, segurança e autenticação, CI/CD e deploy, observabilidade e operação, testes E2E/estratégia de qualidade, Docker/orquestração. As specs afetadas (08, 11, 13, 14 em parte, 29, 31 em parte, 39–46) declaram a limitação. Existem skills com temas parecidos noutros catálogos da ferramenta (fora dos caminhos exigidos); não foram avaliadas nem associadas.

Duas skills têm restrições que condicionam a execução futura e estão refletidas nas specs: `openapi-rest-contract` só cria ficheiros depois de uma prévia aprovada pelo utilizador, e `angular-frontend-scaffold` exige o nome do projeto antes de qualquer ação.

## Fatos sobre o ambiente do destino

- O repositório Git que contém o destino tem raiz em `C:/Users/dowgl` (não na pasta do projeto) — afeta CI (P-23).
- O scaffold do backend (spec 01) gerou um único contrato `openapi/openapi.yaml` e um `.github/workflows/build.yml` aninhado (ver ADR-004 e spec 42).
- A verificação da imagem Docker do scaffold ficou **não executada** por decisão do utilizador (spec 01).

## Template SDD

O template `template-mudanca.md` foi respeitado: cada spec mantém as seções Objetivo, Escopo, Contexto técnico local, Referências, Regras e critérios de aceitação, Tasks, Resultado esperado e Encerramento. Acréscimos sem criar seções de nível 2 novas: linha de identificação (ID/categoria/estado) abaixo do título e subseção "Skills de implementação" dentro de "Regras e critérios de aceitação" (o template não tem campo equivalente). As tasks estão todas desmarcadas porque nada foi executado.

## Confirmação

Nenhuma spec foi executada. Nenhum código de produção Java ou Angular foi escrito. O Flutter de origem não foi alterado.
