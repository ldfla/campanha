# ADR-0008 — Retenção do CPF e rotação de chaves

**Status:** Aceito  
**Data:** 2026-11-01  
**Decisores:** DPO / Segurança (G-SECURITY)

## Decisão

- **CPF** (coletado apenas para recebimento de prêmio): retido por **5 anos após o encerramento da
  campanha**, com base em obrigação legal/regulatória (premiação e prestação de contas); após o prazo,
  **anonimização** (remoção do CPF cifrado e do HMAC).
- **Rotação de chaves** de cifra (CPF e números-surpresa): **anual ou por campanha**, com **KMS
  gerenciado** (custo baixo, limita o dano de um vazamento).

## Consequências

- Implementação atual: CPF cifrado (AES-GCM) + índice HMAC; a **anonimização** já remove o CPF
  (`PrivacyService.anonymize`). O **expurgo automático por prazo** (job/cron + trilha) fica **pendente**.
- A **rotação de chaves** exige procedimento: gerar nova chave, recifrar os registros existentes
  (CPF) e reprocessar/rotacionar o segredo da surpresa; documentar e testar a recuperação.
- A chave **não** entra no dump do banco; guardar em cofre/KMS e testar a restauração.
- Retenção/expurgo deixam de ser OPEN (D-OPEN-012 resolvido); resta implementar o job de expurgo.

## Pendências
- Job de expurgo por prazo (5 anos) com auditoria.
- Procedimento e ensaio de rotação de chaves.
