# Blue Team — defesas implementadas (P16)

> Relatório defensivo (blue team) do backend. Cada controle aponta onde está implementado e como é
> verificado. Complementa o `red-team-report.md` (ataques) e o `threat-model.md` (STRIDE).

## Controles por camada

### Perímetro / HTTP
- **CSP** restritiva (`default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'`),
  **HSTS** com subdomínios, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`,
  `Referrer-Policy: no-referrer`. → `SecurityConfig`; teste `SecurityHeadersTest`.
- **CORS estrito**: nenhuma origem por padrão (`app.cors.allowed-origins`); credenciais permitidas só
  para origens explícitas. → `SecurityConfig.corsConfigurationSource`.
- **Rate limit** em login/cadastro (10 req/60 s) e **lockout** por conta (5 falhas/15 min).
  → `RateLimitFilter`, `LoginAttemptTracker`; testes `RateLimitFilterTest`, `LoginAttemptTrackerTest`.

### Autenticação e sessão
- Sessão por cookie `HttpOnly`/`Secure`/`SameSite` (Spring Session JDBC), **CSRF** por cookie em todos
  os comandos, rotação no login, limite de sessões. MFA para administradores.

### Autorização
- RBAC por papel (`@PreAuthorize`) **e** vínculo por organização (checagem de membership ativo).
- Recursos escopados por campanha/organização (IDOR/BOLA cobertos por testes).

### Dados e privacidade
- PII do comprador isolada (`campaign_number_buyers`), pseudônimos em rankings, e-mail mascarado e
  **CPF cifrado (AES-GCM) + índice HMAC** (ver `cpf-prize-recipient.md`).
- Exportação e anonimização LGPD auditadas.

### Dinheiro e integridade
- `long` em centavos; idempotência; inbox de webhook; reconciliação com a fonte; ledger append-only;
  hash da carga de prêmios e do congelamento da apuração.

### Auditoria e detecção
- `audit_events` append-only para ações sensíveis; hashes encadeáveis onde aplicável.

### Supply chain
- CI: SCA/secrets/misconfig (Trivy, falha em HIGH/CRITICAL) + SBOM CycloneDX.

## Métricas que o blue team deve monitorar
- Taxa de 401/403/429; tentativas de login por conta; falhas de assinatura de webhook; divergências de
  ledger; `DEAD_LETTER` na outbox; `verify` de hash de prêmios/apuração retornando `match=false`.

## Lacunas conhecidas (a fechar)
- Rate limit/lockout em memória (não distribuído) — bypass por múltiplas instâncias.
- Sem anti-bot dedicado em rotas de leitura; sem WAF documentado.
- Rotação de chaves de cifra pendente; retenção/expurgo pendente (`D-OPEN-012`).
