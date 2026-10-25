# 38-frontend-pagina-backup

> **ID:** FRONTEND-SPEC-15 · **Categoria:** Frontend (`features/backup`) · **Estado:** **cancelada por decisão do produto** (P-03, P-16, 2026-10-24) — mantida para rastreabilidade; nada a executar.

## Objetivo

Registar que a página de backup (exportar, importar e backup automático em pasta) **foi removida do escopo**.

## Escopo

- Inclui: o motivo e o efeito na navegação.
- Não inclui: qualquer implementação.

## Contexto técnico local

- Decisão: sem backup e sem migração (P-03, P-16); a exportação/importação manual também sai (confirmado).
- Efeito: o menu lateral passa a ter **seis** entradas (Dashboard, Lançamentos, Relatórios, Categorias, Cofrinhos, Meta); a rota `/backup` não existe; os textos da origem sobre "dados salvos apenas neste dispositivo" não são reaproveitados.
- Ver [BACKEND-SPEC-16](../21-backend-modulo-backup-e-importacao/spec.md) para o destino das regras.

## Referências

- [Pendências](../00-engenharia-reversa/pendencias.md)
- [FRONTEND-SPEC-01](../24-frontend-rotas-e-navegacao/spec.md)
- [Como executar](../../shared/como-executar.md)

## Regras e critérios de aceitação

- [ ] Não existe rota, item de menu nem módulo `features/backup`.

### Skills de implementação

- **Skills obrigatórias:** nenhuma (spec cancelada).
- **Caminhos:** —
- **Responsabilidade:** —
- **Momento de utilização:** —
- **Dependências:** —
- **Limitações:** —

## Tasks

### Descoberta e decisão

- [ ] **Cancelada** — decisão P-03/P-16, sem substituição.

## Resultado esperado

- Navegação sem backup.

## Encerramento

Esta spec está cancelada e não conta como pendência, conforme [Como executar](../../shared/como-executar.md).
