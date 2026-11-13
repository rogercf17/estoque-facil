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
| 11 | Validade do token | 8 horas, configurável por `JWT_EXPIRATION`; sem refresh token | Cobre um turno de trabalho sem complicar o MVP |
| 12 | Claims do JWT | `sub` = id (UUID) do usuário, `role`, `iat`, `exp`; sem nome nem e-mail | Identificador estável e token sem dados pessoais |
| 13 | Token no frontend | `localStorage`; risco de XSS aceito no MVP | Simples e já implementado; revisar antes de produção real |
| 14 | Primeiro administrador | Criado na subida por `ADMIN_NAME`, `ADMIN_EMAIL` e `ADMIN_PASSWORD`, só se não houver nenhum usuário | Sem credencial fixa no repositório |
| 15 | Formato de erro | `ProblemDetail` (RFC 9457); login falho sempre responde 401 com a mesma mensagem | Um único formato para o frontend e sem revelar se o e-mail existe |

## Fluxo vertical do MVP

Login → cadastrar produto → entrada → saída → consultar saldo e histórico → alerta de estoque mínimo.

Tudo fora desse caminho (dashboard, CSV, fornecedores completos) só entra depois que ele estiver completo.
