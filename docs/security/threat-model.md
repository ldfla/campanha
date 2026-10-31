# Threat model (STRIDE) — Ação entre Amigos

> Revisão de segurança exigida pela P16. Escopo: backend (API REST, MySQL, outbox, sessões).
> Ambiente: `DEMO`, sem cobrança real, `LIVE_OPERATIONS_ENABLED=false`.

## Ativos
- Contas e PII (nome, telefone E.164, e-mail), credenciais de sessão.
- Números, participações, pagamentos simulados e ledger.
- Segredos de apuração (compromisso surpresa), chaves de cifra.
- Trilha de auditoria append-only.

## Fronteiras de confiança
1. Navegador ↔ API (sessão por cookie + CSRF; CORS estrito).
2. API ↔ MySQL (rede interna; credenciais por ambiente).
3. API ↔ fonte oficial da Caixa (somente leitura; desabilitada por padrão).
4. Operadores (papéis) ↔ domínio (RBAC + vínculo por organização).
5. Pipeline ↔ repositório (secrets, SBOM).

## STRIDE por fluxo

| Fluxo | Spoofing | Tampering | Repudiation | Info. disclosure | DoS | Elev. privilege |
|---|---|---|---|---|---|---|
| Login/cadastro | Rate limit + lockout por conta; verificação de contato | Sessão HttpOnly/Secure/SameSite | `audit_events` | Erros genéricos; sem enumerar contas | `RateLimitFilter` (429) | MFA admin; papel por recurso |
| Participação/compra | Sessão + CSRF | Idempotência + `FOR UPDATE`; `entityId` obrigatório | Auditoria + correlação | Pseudônimos; PII isolada | Rate limit | Autorização por organização |
| Indicação | Código `SecureRandom` base62 | Atribuição única por entrada; antifraude | `referral_attributions` | Painel sem PII de terceiros | — | Divulgador exige ≥1 cota |
| Pagamento | Webhook assinado/verificado | Inbox idempotente (`event_id`) + reconciliação | `payment_events`/ledger | Sem dados de cartão | Timeouts | `COMMERCIAL_PROMOTION` → 403 |
| Apuração | Fonte oficial + dupla revisão | Congelamento com hash; regra versionada | Pacote de transparência | Só dados públicos | — | Papéis `DRAW_AUDITOR`/`SYSTEM_ADMIN` |
| Ranking | — | Snapshot com hash | `audit_events` | Pseudônimo (iniciais + 4 dígitos) | — | — |

## Riscos residuais conhecidos
- Rate limit e lockout em memória (instância única) — substituir por armazenamento compartilhado.
- Sem CPF no domínio atual (identidade por telefone), então não há cifragem de CPF a implementar;
  caso o CPF seja adicionado, exigirá cifra de campo + índice HMAC (ver ASVS/I.2).
- Revisão de segurança independente externa ainda não realizada (`G-SECURITY`).
