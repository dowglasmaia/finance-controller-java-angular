# 21-backend-modulo-backup-e-importacao

> **ID:** BACKEND-SPEC-16 · **Categoria:** Backend (módulo `backup`) · **Estado:** **cancelada por decisão do produto** (P-03, P-16, 2026-10-24) — mantida para rastreabilidade; nada a executar.

## Objetivo

Registar que a funcionalidade de exportação e importação de backup, e a importação de dados da origem, **foram removidas do escopo**.

## Escopo

- Inclui: o motivo do cancelamento e o destino de cada regra relacionada.
- Não inclui: qualquer implementação. Nenhuma tarefa desta spec deve ser executada.

## Contexto técnico local

### Decisão

- P-03: "Não fazer backup — os dados serão salvos em base de dados SQL."
- P-16: "Não migra nada: API nova, novos dados, nova arquitetura, novo banco de dados."
- Confirmado com o responsável: a exportação/importação manual de JSON também sai.

### Destino do que existia na origem

| Origem | Destino |
|---|---|
| Exportar e importar JSON v2 (DESC-BKP-01…05) | removido |
| Backup automático em pasta (DESC-BKP-06/07) | removido |
| Migração de saldo legado de cofrinhos (DESC-BOX-13) | removida (não há dados legados) |
| Semente de categorias (DESC-SEE-01) | passa para o cadastro de cada utilizador (BACKEND-SPEC-10) |
| Risco de perda parcial na importação (DESC-BKP-03) | deixa de existir |
| Regras `RN-BKP-01…05` | canceladas em [BACKEND-SPEC-01](../07-backend-dominio-e-regras-de-negocio/spec.md) |
| ADR-006, ADR-009 | canceladas |
| Cópia de segurança do banco | tema operacional, adiado com a hospedagem (P-27); não é funcionalidade do produto |

## Referências

- [Pendências](../00-engenharia-reversa/pendencias.md)
- [Decisões](../00-engenharia-reversa/decisoes-adr.md)
- [Como executar](../../shared/como-executar.md)

## Regras e critérios de aceitação

- [ ] Nenhum endpoint, módulo, tabela ou página de backup/importação existe no destino.
- [ ] Os contratos e o índice de operações não contêm `exportBackup` nem `importBackup`.

### Skills de implementação

- **Skills obrigatórias:** nenhuma (spec cancelada).
- **Caminhos:** —
- **Responsabilidade:** —
- **Momento de utilização:** —
- **Dependências:** —
- **Limitações:** —

## Tasks

### Descoberta e decisão

- [ ] **Cancelada** — decisão P-03/P-16, sem substituição. Reabrir apenas por nova decisão do produto.

## Resultado esperado

- Escopo sem backup nem migração, com a rastreabilidade do que foi retirado.

## Encerramento

Esta spec está cancelada e não conta como pendência, conforme [Como executar](../../shared/como-executar.md).
