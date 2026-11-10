# Custos declarados da campanha

`POST /financial/campaigns/{campaignId}/expenses` registra uma despesa declarada por operador,
sem executar transferência ou comprovar pagamento. O corpo contém `idempotencyKey` UUID, `category`
(`PRIZE`, `COMMISSION`, `PLATFORM`, `COMMUNICATION` ou `OTHER`), `amountCents` inteiro positivo,
`description` de até 200 caracteres e `occurredAt` ISO 8601 em UTC, entre 1970-01-01T00:00:01Z e o instante atual. O valor máximo é 9.007.199.254.740.991 centavos, limite exato de inteiros em clientes JavaScript. A resposta informa BRL,
`status=DECLARADA` na criação e alerta de que o registro não comprova pagamento nem conciliação. Um replay após anulação informa `status=ANULADA`.

A mesma chave e os mesmos dados retornam o registro anterior; alteração de dados ou do operador
retorna conflito. `GET` no mesmo caminho lista os registros da campanha, paginados por `page` e
`size` (1 a 100). As duas operações exigem papel `FINANCE` ou `SYSTEM_ADMIN`, conta e vínculo
ativos na organização da campanha; o POST exige sessão e CSRF. Cada inclusão grava evento de
auditoria na mesma transação. A API não altera o ledger de pagamentos e não oferece edição ou
exclusão silenciosa.

Uma declaração incorreta pode ser anulada por `POST /financial/campaigns/{campaignId}/expenses/{expenseId}/reversal`, com `idempotencyKey` UUID e `reason` não vazia de até 200 caracteres. A resposta usa `status=ANULADA`: a anulação é declaratória e não representa devolução de dinheiro. A mesma chave, operador e motivo repetem o resultado; outra solicitação para a mesma despesa retorna conflito. O registro original permanece intacto e a anulação recebe evento próprio de auditoria. `GET` no mesmo caminho consulta a anulação; ambos exigem vínculo financeiro ativo, e o POST exige CSRF. A listagem informa o estado atual sem alterar o registro original e não calcula saldo líquido.
