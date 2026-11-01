# CPF do contemplado — recebimento de prêmio (P13/P16)

> O CPF é coletado **apenas** para o recebimento do prêmio. Não é exigido no cadastro.

## Tratamento de dados
- **Cifrado em repouso**: `users.cpf_encrypted` (envelope AES-GCM, nonce aleatório) com chave
  `app.cpf.encryption-key` (Base64, 32 bytes).
- **Índice de busca**: `users.cpf_hmac` = HMAC-SHA256 (chave `app.cpf.hmac-key`) — permite detectar
  duplicidade/buscar por igualdade **sem** armazenar o CPF em claro e sem índice reversível.
- **Validação**: dígitos verificadores (`Cpf`); rejeita sequências repetidas.
- **Exposição**: a API devolve somente a máscara (`***.***.777-35`). Logs e auditoria registram apenas
  o HMAC (`CPF_REGISTERED`).
- **LGPD**: entra na exportação do titular (mascarado) e é **removido** na anonimização.

## Endpoints
- `PUT /privacy/me/cpf` `{ "cpf": "111.444.777-35" }` → `204` (CSRF obrigatório).
- `GET /privacy/me/cpf` → `{ "maskedCpf": "***.***.777-35" }` (ou `404` se não houver).

## Operação e riscos
- Sem as chaves configuradas, o armazenamento fica **indisponível** (`503 CPF_NOT_CONFIGURED`) — não há
  fallback que grave em claro.
- **Retenção (D-OPEN-012 / ADR-0008):** CPF retido por **5 anos após o encerramento da campanha**
  (obrigação legal/regulatória de premiação e prestação de contas); depois, **anonimização**.
- **Rotação de chaves:** **anual ou por campanha**, com **KMS gerenciado**; requer recifrar os CPFs
  existentes — procedimento e ensaio pendentes (ADR-0008).
- A chave **não** vai no dump do banco; guardar em KMS/cofre e testar a recuperação.
- CPF é dado sensível: acesso restrito; princípio de minimização e finalidade específica
  (recebimento de prêmio).
