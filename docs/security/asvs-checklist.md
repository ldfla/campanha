# Checklist de segurança (OWASP ASVS 2 / API Top 10)

> Mapeamento pragmático do backend atual. "Aplicado" = existe código/teste; "Pendente" = requer ação
> humana (gate) ou trabalho futuro. Não substitui auditoria independente.

## OWASP API Security Top 10

| Item | Situação |
|---|---|
| API1 BOLA (autorização por objeto) | Aplicado — autorização por organização/papel; testes de IDOR (snapshot de outra campanha → 404). |
| API2 Autenticação quebrada | Aplicado — lockout por conta (5/15min), rate limit (10/60s), MFA admin. |
| API3 Exposição de dados excessiva | Aplicado — DTOs explícitos; pseudônimos; PII do comprador isolada; e-mail mascarado na exportação LGPD. |
| API4 Consumo irrestrito | Parcial — rate limit em login/cadastro; falta limite global/por usuário por rota. |
| API5 BFLA (funções) | Aplicado — `@PreAuthorize` por papel + vínculo de organização. |
| API6 Acesso irrestrito a fluxos sensíveis | Parcial — gates de campanha; sem anti-bot dedicado. |
| API7 SSRF | Parcial — cliente da Caixa com base-url fixa e configurável; sem URLs do usuário. |
| API8 Misconfiguração | Aplicado — CSP, HSTS, `X-Content-Type-Options`, `X-Frame-Options`, Referrer-Policy; CORS estrito (vazio por padrão). |
| API9 Inventário | Parcial — endpoints versionados; sem catálogo OpenAPI publicado. |
| API10 Consumo inseguro de APIs | Parcial — verificação/reconciliação; timeouts no cliente HTTP pendentes de tuning. |

## ASVS 2 (pontos-chave)

- V2 Autenticação: sessão HttpOnly/Secure/SameSite, CSRF por cookie, lockout, MFA admin. OK.
- V3 Sessão: expiração e limite de sessões; rotação no login. OK.
- V4 Controle de acesso: RBAC + vínculo por organização. OK.
- V5 Validação: Jakarta Validation + validações de domínio; `entityId`/E.164. OK.
- V6 Criptografia: AES-GCM para segredo surpresa; senha de sessão gerenciada pelo Spring Session. OK.
- V7 Erros e logs: `application/problem+json` com códigos estáveis; sem PII/segredos em log. OK.
- V8 Proteção de dados: PII isolada, pseudônimos, exportação/anonimização LGPD. OK (retenção pendente).
- V9 Comunicação: TLS no proxy (produção). Pendente de `G-OPERATIONS`.
- V10 Malicioso: sem upload público; evidência restrita. Parcial.
- V11 Lógica de negócio: idempotência, limites, antifraude, gates. OK.
- V13 API: contratos, códigos de erro, idempotência. OK.
- V14 Configuração: headers e CORS endurecidos. OK.

## Pendências que exigem gate humano
- Revisão independente (Red Team externo) — `G-SECURITY`.
- SAST/DAST/SCA executados em pipeline real com relatório arquivado.
- Política de retenção e expurgo (`D-OPEN-012`).
