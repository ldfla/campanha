# ADR-0013 — Minimização de conteúdo dos chamados

## Contexto e decisão

O pedido autenticado de anonimização já desativa o usuário, mas deixava assunto, mensagem e impressão de idempotência dos seus chamados intactos. Na mesma transação, o serviço de suporte substitui assunto e mensagem por um marcador e remove a chave e a impressão de idempotência dos chamados do titular. Estado, vínculo pseudônimo e eventos de auditoria são preservados. A abertura e a anonimização bloqueiam primeiro o usuário para serializar operações concorrentes, inclusive aberturas sem chave.

A decisão depende das colunas introduzidas pela migração V030 do PR de idempotência de suporte. Não cria migração, não apaga backups nem outras cópias e não demonstra anonimização integral. Não há execução automática por prazo; a alteração ocorre somente no fluxo existente solicitado pelo titular.

## Limites e validação

A política de retenção e eventuais impedimentos de exclusão exigem decisão humana antes de operação real. A ADR-0008 registra proposta de retenção de CPF por cinco anos, enquanto o fluxo existente limpa o CPF imediatamente; a divergência permanece aberta em D-OPEN-012. Não se conclui conformidade jurídica. Operação real continua desativada.

Os testes locais usam dados sintéticos e MySQL de teste para conferir isolamento por titular, preservação de auditoria, rollback e concorrência em ambas as ordens, com e sem chave.
