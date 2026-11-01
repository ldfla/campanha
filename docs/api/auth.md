# Autenticação — contrato (ADR-0010)

Base: `/api`. Erros em `application/problem+json;charset=UTF-8` com `code` estável.

| Método e caminho | Corpo | Sucesso | Erros |
|---|---|---|---|
| `POST /auth/register` | `{firstName, surname, phoneNumber}` | **202** `{"status":"CODE_SENT","detail":"…"}` — sem sessão | 400 `VALIDATION_ERROR`; 429 `AUTH_ACCOUNT_LOCKED` (envios em excesso) |
| `POST /auth/login` | `{phoneNumber}` | **202** idêntico ao cadastro — sem sessão, não revela existência | 400; 429 |
| `POST /auth/login/verify` | `{phoneNumber, code}` (6 dígitos) | **200** `UserResponse` + cookie `SESSION` rotacionado | 401 `AUTH_INVALID_CREDENTIALS` (mesmo erro para telefone inexistente); 403 `AUTH_MFA_REQUIRED` (papel administrativo: sessão pendente criada); 429 `AUTH_ACCOUNT_LOCKED` após 5 falhas |
| `POST /auth/mfa/verify` | `{phoneNumber, code}` | **200** `UserResponse` | 403 `AUTH_MFA_REQUIRED` sem primeiro fator na mesma sessão ou código errado; 429 após 5 falhas |
| `POST /auth/verify-contact` | `{phoneNumber, code}` | 204 | 401; 429 |
| `GET /auth/me` | — | 200 `UserResponse` (telefone mascarado) | 401 |
| `POST /auth/logout` | — | 204 | — |

- Telefone: E.164 (`+5511…`); `55DDDNÚMERO` com 12–13 dígitos é aceito como já contendo o DDI.
- DEMO: nenhuma mensagem é enviada; o código de teste `000000` existe apenas nos perfis `default/local/test`.
- Mutações autenticadas exigem `X-XSRF-TOKEN` (cookie `XSRF-TOKEN`); as rotas acima são isentas por não
  dependerem de sessão prévia (exceto `mfa/verify`, que exige a sessão pendente).

## Meus números
`GET /participations/campaigns/{campaignPublicId}/numbers/mine` (sessão obrigatória; sem sessão → 401 sem dados):

```json
{"campaignPublicId":"…","numbers":[{"formattedNumber":"00000","status":"CONFIRMED"}],
 "confirmedCount":1,"pendingCount":0,"confirmedAmountCents":0,"currency":"BRL","simulation":true}
```

## Mudanças relacionadas (P20)
- `POST /payments/orders` (valor no corpo) passou a ser exclusivo de `SYSTEM_ADMIN` (sandbox).
- `GET …/numbers/prize-load/export` responde 409 `PRIZE_LOAD_SEALED` até a campanha chegar a `LOCKED`.
- `POST /participations/campaigns/{id}` (sem cobrança) responde 403 `CAMPAIGN_LIVE_OPERATIONS_DISABLED` fora de `DEMO`.
- `/actuator/**`, exceto `health`, exige `SYSTEM_ADMIN` ou `SECURITY_ADMIN`.
