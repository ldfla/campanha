# Custos declarados da campanha

`POST /financial/campaigns/{campaignId}/expenses` registra uma despesa declarada por operador,
sem executar transferência ou comprovar pagamento. O corpo contém `idempotencyKey` UUID, `category`
(`PRIZE`, `COMMISSION`, `PLATFORM`, `COMMUNICATION` ou `OTHER`), `amountCents` inteiro positivo,
`description` de até 200 caracteres e `occurredAt` ISO 8601 em UTC. A resposta informa BRL,
`status=DECLARADA` e alerta de que o registro não comprova pagamento nem conciliação.

A mesma chave e os mesmos dados retornam o registro anterior; alteração de dados ou do operador
retorna conflito. `GET` no mesmo caminho lista os registros da campanha, paginados por `page` e
`size` (1 a 100). As duas operações exigem papel `FINANCE` ou `SYSTEM_ADMIN`, conta e vínculo
ativos na organização da campanha; o POST exige sessão e CSRF. Cada inclusão grava evento de
auditoria na mesma transação. A API não altera o ledger de pagamentos e não oferece edição ou
exclusão silenciosa; procedimento de correção compensatória permanece pendente.
