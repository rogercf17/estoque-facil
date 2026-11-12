# Decisões do projeto (ADR resumido)

Registro das decisões que a especificação deixou em aberto. Alterar aqui antes de mudar no código.

| # | Tema | Decisão | Motivo |
|---|------|---------|--------|
| 1 | Saldo | Ledger (`stock_movement`) + tabela `stock_balance` atualizada na mesma transação, com `version` (lock otimista) | Auditável e rápido para consulta; recomendação da especificação |
| 2 | Unidades | `UN`, `KG`, `L`, sem conversão | Evita saldos sem significado |
| 3 | Ajuste de inventário | Somente `MANAGER` | Reduz risco de divergência no MVP |
| 4 | Localização | Uma por instalação | Multi-depósito fica fora do MVP |
| 5 | Autenticação | JWT stateless, senha com BCrypt | Simples e adequado ao MVP |
| 6 | Identificadores | UUID | Padrão da especificação |
| 7 | Datas | `TIMESTAMPTZ` em UTC; conversão para fuso local só na interface | Evita ambiguidades |
| 8 | Quantidades | `NUMERIC(14,3)`, nunca ponto flutuante | Precisão |
| 9 | Imutabilidade | Trigger no banco bloqueia UPDATE/DELETE de movimentação confirmada; correção por movimentação compensatória | Garantia independente da aplicação |
| 10 | Schema | Somente via Flyway; `ddl-auto: validate` | Migrations versionadas |

## Fluxo vertical do MVP

Login → cadastrar produto → entrada → saída → consultar saldo e histórico → alerta de estoque mínimo.

Tudo fora desse caminho (dashboard, CSV, fornecedores completos) só entra depois que ele estiver completo.
